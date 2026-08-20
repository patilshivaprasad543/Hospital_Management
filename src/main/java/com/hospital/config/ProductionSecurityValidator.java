package com.hospital.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    @Value("${smartcare.admin.email:}")
    private String adminEmail;

    @EventListener(ApplicationReadyEvent.class)
    public void validateProductionSecrets() {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.warn("Admin email is blank after bootstrap. Check SMARTCARE_ADMIN_EMAIL.");
            return;
        }
        log.info("Production admin account is ready for {}", adminEmail);
    }
}
