package com.hospital.service.mail;

import com.hospital.service.NotificationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BrevoMailTransport implements MailTransport {

    private static final Logger logger = LoggerFactory.getLogger(BrevoMailTransport.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final NotificationLogService notificationLogService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final boolean emailEnabled;

    public BrevoMailTransport(
            NotificationLogService notificationLogService,
            @Value("${smartcare.mail.brevo.api-key:}") String apiKey,
            @Value("${smartcare.mail.brevo.sender-email:}") String senderEmail,
            @Value("${smartcare.mail.brevo.sender-name:SmartCare 360}") String senderName,
            @Value("${smartcare.notifications.email-enabled:true}") boolean emailEnabled) {
        this.notificationLogService = notificationLogService;
        this.apiKey = trim(apiKey);
        this.senderEmail = trim(senderEmail);
        this.senderName = trim(senderName);
        this.emailEnabled = emailEnabled;
    }

    @Override
    public String providerName() {
        return "brevo";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank() && !senderEmail.isBlank();
    }

    @Override
    public boolean send(String to, String subject, String body, String type) {
        if (!emailEnabled) {
            notificationLogService.log("EMAIL", to, subject, body, false, "Email channel disabled in settings");
            return false;
        }
        if (to == null || to.isBlank()) {
            notificationLogService.log("EMAIL", to, subject, body, false, "Recipient email is blank");
            return false;
        }
        if (!isConfigured()) {
            notificationLogService.log("EMAIL", to, subject, body, false,
                    "Brevo not configured — set SMARTCARE_BREVO_API_KEY and SMARTCARE_BREVO_SENDER_EMAIL");
            return false;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sender", Map.of("name", senderName, "email", senderEmail));
            payload.put("to", List.of(Map.of("email", to)));
            payload.put("subject", subject);
            payload.put("textContent", body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    BREVO_API_URL, new HttpEntity<>(payload, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Brevo email dispatched to {}", to);
                notificationLogService.log("EMAIL", to, subject, body, true, "Sent via Brevo API (HTTPS)");
                return true;
            }

            String note = "Brevo API returned HTTP " + response.getStatusCode().value();
            notificationLogService.log("EMAIL", to, subject, body, false, note);
            return false;
        } catch (Exception e) {
            logger.warn("Could not send Brevo email to {}: {}", to, e.getMessage());
            notificationLogService.log("EMAIL", to, subject, body, false, e.getMessage());
            return false;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
