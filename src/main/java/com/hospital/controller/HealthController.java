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
        body.put("storage", storageName());
        String databaseUrl = System.getenv("DATABASE_URL");
        body.put("databaseUrlSet", databaseUrl != null && !databaseUrl.isBlank() ? "yes" : "no");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
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
