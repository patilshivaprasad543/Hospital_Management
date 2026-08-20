package com.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private static final String WEAK_DEFAULT_PASSWORD = "Admin@360";

    @Value("${smartcare.admin.password:}")
    private String adminPassword;

    @Value("${smartcare.admin.email:}")
    private String adminEmail;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionSecrets() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "SMARTCARE_ADMIN_PASSWORD must be set in production. Admin credentials are never published publicly.");
        }
        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "SMARTCARE_ADMIN_EMAIL must be set in production.");
        }
        if (WEAK_DEFAULT_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "Default admin password detected in production. Set a strong SMARTCARE_ADMIN_PASSWORD.");
        }
    }
}
