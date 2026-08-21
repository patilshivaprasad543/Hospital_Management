package com.hospital.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs which database-related env keys exist (names only) so Render logs
 * show whether Postgres was linked to the web service.
 */
@Component
public class DatabaseStartupLog {

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseKeys() {
        System.out.println(">>> DB env DATABASE_URL=" + present("DATABASE_URL")
                + " POSTGRES_URL=" + present("POSTGRES_URL")
                + " PGHOST=" + present("PGHOST")
                + " POSTGRES_HOST=" + present("POSTGRES_HOST"));
        System.getenv().keySet().stream()
                .filter(k -> {
                    String u = k.toUpperCase();
                    return u.contains("DATABASE") || u.contains("POSTGRES") || u.startsWith("PG");
                })
                .sorted()
                .forEach(k -> System.out.println(">>> DB env key: " + k));
    }

    private static String present(String key) {
        String value = System.getenv(key);
        return value != null && !value.isBlank() ? "yes" : "no";
    }
}
