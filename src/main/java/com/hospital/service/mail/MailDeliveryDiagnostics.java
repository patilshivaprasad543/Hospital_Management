package com.hospital.service.mail;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class MailDeliveryDiagnostics {

    private final AtomicReference<String> lastFailure = new AtomicReference<>();

    public void recordSuccess() {
        lastFailure.set(null);
    }

    public void recordFailure(String provider, String detail) {
        if (detail == null || detail.isBlank()) {
            lastFailure.set(provider + " delivery failed.");
            return;
        }
        lastFailure.set(provider + ": " + sanitize(detail));
    }

    public String getLastFailure() {
        return lastFailure.get();
    }

    private static String sanitize(String detail) {
        String trimmed = detail.trim();
        if (trimmed.length() > 220) {
            return trimmed.substring(0, 217) + "...";
        }
        return trimmed;
    }
}
