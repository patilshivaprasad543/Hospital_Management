package com.hospital.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Render/Docker prod often starts with empty admin env vars. Unresolved
 * placeholders used to crash the JVM, which shows up as Render 502/505.
 * Generate and persist admin credentials on first boot so the site stays up.
 */
public class AdminCredentialBootstrap implements EnvironmentPostProcessor, Ordered {

    static final String WEAK_DEFAULT_PASSWORD = "Admin@360";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        String email = firstNonBlank(
                environment.getProperty("SMARTCARE_ADMIN_EMAIL"),
                environment.getProperty("smartcare.admin.email"));
        String password = firstNonBlank(
                environment.getProperty("SMARTCARE_ADMIN_PASSWORD"),
                environment.getProperty("smartcare.admin.password"));
        String name = firstNonBlank(
                environment.getProperty("SMARTCARE_ADMIN_NAME"),
                environment.getProperty("smartcare.admin.name"),
                "System Administrator");
        String mobile = firstNonBlank(
                environment.getProperty("SMARTCARE_ADMIN_MOBILE"),
                environment.getProperty("smartcare.admin.mobile"),
                "9999999999");
        String appUrl = firstNonBlank(
                environment.getProperty("SMARTCARE_APP_URL"),
                environment.getProperty("smartcare.app.base-url"),
                "");

        Path dataDir = resolveDataDir(environment);
        Path store = dataDir.resolve("admin-bootstrap.properties");

        boolean needEmail = email == null;
        boolean needPassword = password == null || WEAK_DEFAULT_PASSWORD.equals(password);

        if ((needEmail || needPassword) && Files.isRegularFile(store)) {
            Properties saved = load(store);
            if (needEmail) {
                email = firstNonBlank(saved.getProperty("smartcare.admin.email"));
            }
            if (needPassword) {
                password = firstNonBlank(saved.getProperty("smartcare.admin.password"));
            }
            if (mobile == null || "9999999999".equals(mobile)) {
                mobile = firstNonBlank(saved.getProperty("smartcare.admin.mobile"), mobile);
            }
        }

        needEmail = email == null;
        needPassword = password == null || WEAK_DEFAULT_PASSWORD.equals(password);
        boolean generated = false;
        if (needEmail) {
            email = "admin@smartcare360.local";
            generated = true;
        }
        if (needPassword) {
            password = randomPassword();
            generated = true;
        }
        if (mobile == null) {
            mobile = "9999999999";
        }

        if (generated) {
            try {
                Files.createDirectories(dataDir);
                Properties out = new Properties();
                out.setProperty("smartcare.admin.email", email);
                out.setProperty("smartcare.admin.password", password);
                out.setProperty("smartcare.admin.mobile", mobile);
                try (var writer = Files.newBufferedWriter(store, StandardCharsets.UTF_8)) {
                    out.store(writer, "Generated on first production boot. Not published.");
                }
                Path readable = dataDir.resolve("admin-credentials.txt");
                Files.writeString(readable,
                        "SmartCare 360 admin (first-boot credentials)\n"
                                + "Email: " + email + "\n"
                                + "Password: " + password + "\n"
                                + "Change this password after login.\n",
                        StandardCharsets.UTF_8);
                System.out.println(">>> Admin credentials bootstrapped (also in " + readable + ") <<<");
                System.out.println(">>> ADMIN_EMAIL=" + email + " <<<");
                System.out.println(">>> ADMIN_PASSWORD=" + password + " <<<");
            } catch (IOException ex) {
                System.err.println("Could not persist admin bootstrap file: " + ex.getMessage());
            }
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("SMARTCARE_ADMIN_EMAIL", email);
        props.put("SMARTCARE_ADMIN_PASSWORD", password);
        props.put("SMARTCARE_ADMIN_NAME", name);
        props.put("SMARTCARE_ADMIN_MOBILE", mobile);
        props.put("smartcare.admin.email", email);
        props.put("smartcare.admin.password", password);
        props.put("smartcare.admin.name", name);
        props.put("smartcare.admin.mobile", mobile);
        if (appUrl != null) {
            props.put("SMARTCARE_APP_URL", appUrl);
            props.put("smartcare.app.base-url", appUrl);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("adminCredentialBootstrap", props));
    }

    static Path resolveDataDir(ConfigurableEnvironment environment) {
        String override = firstNonBlank(environment.getProperty("SMARTCARE_DATA_DIR"));
        if (override != null) {
            return Path.of(override);
        }
        if (Files.isDirectory(Path.of("/app/data"))) {
            return Path.of("/app/data");
        }
        return Path.of("data");
    }

    private static Properties load(Path store) {
        Properties properties = new Properties();
        try (var in = Files.newInputStream(store)) {
            properties.load(in);
        } catch (IOException ignored) {
            // first boot or unreadable file — generate fresh values
        }
        return properties;
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

    private static String randomPassword() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        char[] alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
        char[] out = new char[20];
        for (int i = 0; i < out.length; i++) {
            out[i] = alphabet[Math.floorMod(bytes[i % bytes.length] + i, alphabet.length)];
        }
        return new String(out);
    }
}
