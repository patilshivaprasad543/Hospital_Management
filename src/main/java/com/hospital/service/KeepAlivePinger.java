package com.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class KeepAlivePinger {

    private static final Logger log = LoggerFactory.getLogger(KeepAlivePinger.class);

    private final RestTemplate restTemplate;
    private final boolean enabled;
    private final String baseUrl;

    public KeepAlivePinger(
            @Value("${smartcare.keepalive.enabled:false}") boolean enabled,
            @Value("${smartcare.app.base-url:}") String baseUrl) {
        this.enabled = enabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedDelayString = "${smartcare.keepalive.interval-ms:180000}", initialDelay = 45_000)
    public void pingSelf() {
        if (!enabled || baseUrl.isBlank()) {
            return;
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String lower = normalized.toLowerCase();
        if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
            return;
        }
        try {
            restTemplate.getForEntity(normalized + "/health", String.class);
            log.debug("Keep-alive ping ok for {}", normalized);
        } catch (Exception ex) {
            log.debug("Keep-alive ping missed {}: {}", normalized, ex.getMessage());
        }
    }
}
