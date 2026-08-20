package com.hospital.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.service.NotificationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BrevoMailTransport implements MailTransport {

    private static final Logger logger = LoggerFactory.getLogger(BrevoMailTransport.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final NotificationLogService notificationLogService;
    private final MailDeliveryDiagnostics diagnostics;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final boolean emailEnabled;

    @Autowired
    public BrevoMailTransport(
            NotificationLogService notificationLogService,
            MailDeliveryDiagnostics diagnostics,
            @Value("${smartcare.mail.brevo.api-key:}") String apiKey,
            @Value("${smartcare.mail.brevo.sender-email:}") String senderEmail,
            @Value("${smartcare.mail.brevo.sender-name:SmartCare 360}") String senderName,
            @Value("${smartcare.notifications.email-enabled:true}") boolean emailEnabled) {
        this.notificationLogService = notificationLogService;
        this.diagnostics = diagnostics;
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
        return isValidApiKey(apiKey) && !senderEmail.isBlank();
    }

    public String configurationHint() {
        if (!isValidApiKey(apiKey)) {
            return "Set SMARTCARE_BREVO_API_KEY (must start with xkeysib-).";
        }
        if (senderEmail.isBlank()) {
            return "Set SMARTCARE_BREVO_SENDER_EMAIL to a verified sender in Brevo.";
        }
        return "Brevo is configured with sender " + senderEmail + ".";
    }

    private static boolean isValidApiKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String trimmed = key.trim();
        String lower = trimmed.toLowerCase();
        if (lower.contains("your-brevo")
                || lower.contains("replace")
                || lower.contains("changeme")
                || lower.equals("your-brevo-api-key")) {
            return false;
        }
        return trimmed.startsWith("xkeysib-");
    }

    @Override
    public boolean send(String to, String subject, String body, String type) {
        if (!emailEnabled) {
            return fail(to, subject, body, "Email channel disabled in settings");
        }
        if (to == null || to.isBlank()) {
            return fail(to, subject, body, "Recipient email is blank");
        }
        if (!isConfigured()) {
            return fail(to, subject, body, configurationHint());
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
                diagnostics.recordSuccess();
                return true;
            }

            return fail(to, subject, body, "Brevo API returned HTTP " + response.getStatusCode().value());
        } catch (HttpStatusCodeException e) {
            String note = parseBrevoError(e);
            logger.warn("Could not send Brevo email to {}: {}", to, note);
            return fail(to, subject, body, note);
        } catch (Exception e) {
            logger.warn("Could not send Brevo email to {}: {}", to, e.getMessage());
            return fail(to, subject, body, e.getMessage());
        }
    }

    private boolean fail(String to, String subject, String body, String note) {
        notificationLogService.log("EMAIL", to, subject, body, false, note);
        diagnostics.recordFailure("Brevo", note);
        return false;
    }

    private String parseBrevoError(HttpStatusCodeException e) {
        String responseBody = e.getResponseBodyAsString();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            if (root.hasNonNull("message")) {
                String message = root.get("message").asText();
                if (e.getStatusCode().value() == 401) {
                    return "Invalid Brevo API key (401). Create a new v3 API key in Brevo → Settings → SMTP & API.";
                }
                if (message.toLowerCase().contains("sender")) {
                    return message + " Verify " + senderEmail + " under Brevo → Settings → Senders.";
                }
                return message;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "Brevo HTTP " + e.getStatusCode().value() + ": " + responseBody;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
