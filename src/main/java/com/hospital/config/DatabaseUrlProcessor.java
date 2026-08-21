package com.hospital.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uses Render/Heroku {@code DATABASE_URL} (Postgres/MySQL) when set so live
 * data survives web-service sleep. Otherwise production H2 is stored under
 * {@code /app/data} (Render disk) or {@code ./data}.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("SMARTCARE_DB_URL"));
        Map<String, Object> props = configure(raw, environment.acceptsProfiles(Profiles.of("prod")));
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("smartcareDatabaseUrl", props));
            Object url = props.get("spring.datasource.url");
            System.out.println(">>> Database: " + redact(String.valueOf(url)));
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
            props.put("SMARTCARE_DB_URL", h2);
            props.put("spring.datasource.driver-class-name", "org.h2.Driver");
            props.put("spring.datasource.username", "sa");
            props.put("spring.datasource.password", "");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
            props.put("SMARTCARE_DATA_DIR", dataDir.toAbsolutePath().toString());
        }
        return props;
    }

    static void applyExternalUrl(String rawUrl, Map<String, Object> props) {
        String url = rawUrl;
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url.replaceFirst("^postgres://", "postgresql://"));
            String userInfo = uri.getUserInfo();
            String user = "";
            String password = "";
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    user = userInfo.substring(0, colon);
                    password = userInfo.substring(colon + 1);
                } else {
                    user = userInfo;
                }
            }
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/smartcare360" : uri.getPath();
            String query = uri.getQuery();
            String jdbc = "jdbc:postgresql://" + host + ":" + port + path;
            if (query == null || query.isBlank()) {
                jdbc += "?sslmode=require";
            } else {
                jdbc += "?" + query;
                if (!query.contains("sslmode")) {
                    jdbc += "&sslmode=require";
                }
            }
            props.put("spring.datasource.url", jdbc);
            props.put("SMARTCARE_DB_URL", jdbc);
            props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
            props.put("spring.datasource.username", user);
            props.put("spring.datasource.password", password);
            props.put("SMARTCARE_DB_USERNAME", user);
            props.put("SMARTCARE_DB_PASSWORD", password);
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("spring.datasource.hikari.maximum-pool-size", "5");
            return;
        }
        if (url.startsWith("mysql://")) {
            URI uri = URI.create(url);
            String userInfo = uri.getUserInfo();
            String user = "root";
            String password = "";
            if (userInfo != null) {
                int colon = userInfo.indexOf(':');
                if (colon >= 0) {
                    user = userInfo.substring(0, colon);
                    password = userInfo.substring(colon + 1);
                } else {
                    user = userInfo;
                }
            }
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 3306;
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/smartcare360" : uri.getPath();
            String jdbc = "jdbc:mysql://" + host + ":" + port + path
                    + "?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            props.put("spring.datasource.url", jdbc);
            props.put("SMARTCARE_DB_URL", jdbc);
            props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
            props.put("spring.datasource.username", user);
            props.put("spring.datasource.password", password);
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
            return;
        }
        if (url.startsWith("jdbc:")) {
            props.put("spring.datasource.url", url);
            props.put("SMARTCARE_DB_URL", url);
            if (url.startsWith("jdbc:postgresql:")) {
                props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
                props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
            } else if (url.startsWith("jdbc:mysql:")) {
                props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
                props.put("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
            }
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String redact(String jdbcUrl) {
        return jdbcUrl.replaceAll("://[^@/]+@", "://****@");
    }
}
