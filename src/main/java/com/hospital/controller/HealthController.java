package com.hospital.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(ObjectProvider<DataSource> dataSource) {
        this.dataSource = dataSource.getIfAvailable();
    }

    @RequestMapping(value = {"/health", "/healthz", "/ping", "/keepalive"},
            method = {RequestMethod.GET, RequestMethod.HEAD},
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", "ok");
        String storage = storageName();
        body.put("storage", storage);
        body.put("persistent", persistentFlag(storage));
        body.put("databaseUrlSet", presentEnv(
                "DATABASE_URL", "POSTGRES_URL", "POSTGRES_CONNECTION_STRING", "JDBC_DATABASE_URL"));
        body.put("pgHostSet", presentEnv("PGHOST", "POSTGRES_HOST", "DB_HOST"));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    private static String presentEnv(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) {
                return "yes";
            }
        }
        return "no";
    }

    private static String persistentFlag(String storage) {
        if (storage == null || "unknown".equals(storage) || "error".equals(storage)) {
            return "unknown";
        }
        if (storage.toLowerCase().contains("postgres")) {
            return "yes";
        }
        if ("H2".equalsIgnoreCase(storage)) {
            return "no";
        }
        return "yes";
    }

    private String storageName() {
        if (dataSource == null) {
            return "unknown";
        }
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || product.isBlank()) {
                return "unknown";
            }
            return product;
        } catch (Exception ex) {
            return "error";
        }
    }
}
