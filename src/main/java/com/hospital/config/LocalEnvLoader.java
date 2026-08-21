package com.hospital.config;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads a local {@code .env} file into the process environment when Eclipse
 * (or another IDE) starts the app without {@code scripts/start.sh}.
 * Existing OS / launch-config variables are not overwritten.
 */
public final class LocalEnvLoader {

    private LocalEnvLoader() {
    }

    public static void load() {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            envFile = Path.of("config/local.env");
        }
        load(envFile);
    }

    static void load(Path envFile) {
        if (envFile == null || !Files.isRegularFile(envFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(envFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
            System.out.println(">>> Loaded local environment from " + envFile.toAbsolutePath());
        } catch (Exception ex) {
            System.err.println("Could not read " + envFile + ": " + ex.getMessage());
        }
    }
}
