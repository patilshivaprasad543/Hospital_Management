package com.hospital.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private static final Map<String, Object> HEALTH_RESPONSE = Map.of(
            "status", "UP",
            "application", "hospital-management",
            "timestamp", System.currentTimeMillis()
    );

    @GetMapping(value = {"/health", "/healthz", "/ping", "/keepalive"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(HEALTH_RESPONSE);
    }
}
