package com.hospital.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uses Render {@code DATABASE_URL} (Postgres) when set so live data survives
 * web-service sleep. File H2 is only used when no Postgres URL is present.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                System.getenv("DATABASE_URL"),
                getenv(environment, "DATABASE_URL"),
                System.getenv("POSTGRES_URL"),
                getenv(environment, "SMARTCARE_DB_URL"));
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        try {
            Map<String, Object> props = configure(raw, prod);
            if (!props.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource("smartcareDatabaseUrl", props));
                System.out.println(">>> Database: " + redact(String.valueOf(props.get("spring.datasource.url"))));
            }
        } catch (Exception ex) {
            System.err.println(">>> Failed to apply DATABASE_URL: " + ex.getMessage());
            if (raw != null && isPostgres(raw)) {
                throw new IllegalStateException("DATABASE_URL is set but could not be parsed: " + ex.getMessage(), ex);
            }
            if (prod) {
                Map<String, Object> props = configure(null, true);
                environment.getPropertySources().addFirst(new MapPropertySource("smartcareDatabaseUrl", props));
            }
        }
    }

    static Map<String, Object> configure(String rawUrl, boolean prod) {
        Map<String, Object> props = new LinkedHashMap<>();
        if (rawUrl != null && !rawUrl.isBlank() && !rawUrl.startsWith("jdbc:h2:")) {
            applyExternalUrl(rawUrl.trim(), props);
            return props;
        }
        if (prod) {
            Path dataDir = Files.isDirectory(Path.of("/app/data")) ? Path.of("/app/data") : Path.of("data");
            try {
                Files.createDirectories(dataDir);
            } catch (Exception ignored) {
                dataDir = Path.of("data");
            }
            String h2 = "jdbc:h2:file:" + dataDir.toAbsolutePath() + "/smartcare360;DB_CLOSE_DELAY=-1";
            props.put("spring.datasource.url", h2);
            props.put("spring.datasource.driver-class-name", "org.h2.Driver");
            props.put("spring.datasource.username", "sa");
            props.put("spring.datasource.password", "");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
            props.put("SMARTCARE_DATA_DIR", dataDir.toAbsolutePath().toString());
        }
        return props;
    }

    static void applyExternalUrl(String rawUrl, Map<String, Object> props) {
        String url = rawUrl.trim();
        if (isPostgres(url)) {
            ParsedPostgres parsed = parsePostgres(url);
            boolean externalHost = parsed.host.contains("render.com");
            String ssl = externalHost ? "sslmode=require" : "sslmode=disable";
            String jdbc = "jdbc:postgresql://" + parsed.host + ":" + parsed.port + parsed.path;
            if (parsed.query == null || parsed.query.isBlank()) {
                jdbc += "?" + ssl;
            } else {
                jdbc += "?" + parsed.query;
                if (!parsed.query.contains("sslmode")) {
                    jdbc += "&" + ssl;
                }
            }
            props.put("spring.datasource.url", jdbc);
            props.put("SMARTCARE_DB_URL", jdbc);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.datasource.username", parsed.user);
            props.put("spring.datasource.password", parsed.password);
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("spring.datasource.hikari.maximum-pool-size", "5");
            return;
        }
        if (url.startsWith("mysql://") || url.startsWith("jdbc:mysql:")) {
            if (url.startsWith("jdbc:")) {
                props.put("spring.datasource.url", url);
            } else {
                ParsedPostgres parsed = parsePostgres("postgresql://" + url.substring("mysql://".length()));
                String jdbc = "jdbc:mysql://" + parsed.host + ":" + (parsed.port.equals("5432") ? "3306" : parsed.port)
                        + parsed.path + "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                props.put("spring.datasource.url", jdbc);
                props.put("spring.datasource.username", parsed.user);
                props.put("spring.datasource.password", parsed.password);
            }
            props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
            return;
        }
        if (url.startsWith("jdbc:postgresql:")) {
            props.put("spring.datasource.url", url);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        } else if (url.startsWith("jdbc:")) {
            props.put("spring.datasource.url", url);
        }
    }

    static boolean isPostgres(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("postgres://") || lower.startsWith("postgresql://")
                || lower.startsWith("jdbc:postgresql:");
    }

    static ParsedPostgres parsePostgres(String rawUrl) {
        String url = rawUrl.replaceFirst("(?i)^jdbc:", "");
        url = url.replaceFirst("(?i)^postgres(ql)?://", "");
        int at = url.lastIndexOf('@');
        if (at < 0) {
            throw new IllegalArgumentException("Postgres URL missing @host");
        }
        String userPass = url.substring(0, at);
        String hostPart = url.substring(at + 1);
        String user;
        String password = "";
        int colon = userPass.indexOf(':');
        if (colon >= 0) {
            user = decode(userPass.substring(0, colon));
            password = decode(userPass.substring(colon + 1));
        } else {
            user = decode(userPass);
        }
        String query = null;
        int q = hostPart.indexOf('?');
        if (q >= 0) {
            query = hostPart.substring(q + 1);
            hostPart = hostPart.substring(0, q);
        }
        String path = "/smartcare360";
        int slash = hostPart.indexOf('/');
        String hostPort = hostPart;
        if (slash >= 0) {
            path = hostPart.substring(slash);
            hostPort = hostPart.substring(0, slash);
        }
        String host = hostPort;
        String port = "5432";
        int lastColon = hostPort.lastIndexOf(':');
        if (lastColon >= 0) {
            host = hostPort.substring(0, lastColon);
            port = hostPort.substring(lastColon + 1);
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        return new ParsedPostgres(user, password, host, port, path.startsWith("/") ? path : "/" + path, query);
    }

    private static String getenv(ConfigurableEnvironment environment, String key) {
        String value = environment.getProperty(key);
        if (value != null) {
            return value;
        }
        Object env = environment.getSystemEnvironment().get(key);
        return env == null ? null : String.valueOf(env);
    }

    private static String decode(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return null;
    }

    private static String redact(String jdbcUrl) {
        return jdbcUrl.replaceAll("://[^@/]+@", "://****@");
    }

    static final class ParsedPostgres {
        final String user;
        final String password;
        final String host;
        final String port;
        final String path;
        final String query;

        ParsedPostgres(String user, String password, String host, String port, String path, String query) {
            this.user = user;
            this.password = password;
            this.host = host;
            this.port = port;
            this.path = path;
            this.query = query;
        }
    }
}
