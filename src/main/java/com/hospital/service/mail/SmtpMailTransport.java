package com.hospital.service.mail;

import com.hospital.service.NotificationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailTransport implements MailTransport {

    private static final Logger logger = LoggerFactory.getLogger(SmtpMailTransport.class);

    private final JavaMailSender mailSender;
    private final NotificationLogService notificationLogService;
    private final MailDeliveryDiagnostics diagnostics;
    private final String fromEmail;
    private final String mailPassword;
    private final boolean emailEnabled;

    public SmtpMailTransport(
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender,
            NotificationLogService notificationLogService,
            MailDeliveryDiagnostics diagnostics,
            @Value("${spring.mail.username:}") String fromEmail,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${smartcare.notifications.email-enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.notificationLogService = notificationLogService;
        this.diagnostics = diagnostics;
        this.fromEmail = trim(fromEmail);
        this.mailPassword = trim(mailPassword);
        this.emailEnabled = emailEnabled;
    }

    @Override
    public String providerName() {
        return "smtp";
    }

    @Override
    public boolean isConfigured() {
        return mailSender != null && !fromEmail.isBlank() && !mailPassword.isBlank();
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
                    "SMTP not configured — set SMARTCARE_MAIL_USERNAME and SMARTCARE_MAIL_PASSWORD");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            logger.info("SMTP email dispatched to {}", to);
            notificationLogService.log("EMAIL", to, subject, body, true, "Sent via SMTP");
            diagnostics.recordSuccess();
            return true;
        } catch (Exception e) {
            String note = e.getMessage();
            if (isLikelySmtpPortBlock(note)) {
                note = "SMTP blocked on this host (Render free). Configure Brevo: SMARTCARE_BREVO_API_KEY + SMARTCARE_BREVO_SENDER_EMAIL.";
            }
            logger.warn("Could not send SMTP email to {}: {}", to, e.getMessage());
            notificationLogService.log("EMAIL", to, subject, body, false, note);
            diagnostics.recordFailure("Gmail SMTP", note);
            return false;
        }
    }

    private static boolean isLikelySmtpPortBlock(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase();
        return lower.contains("network is unreachable")
                || lower.contains("connection timed out")
                || lower.contains("connect timed out")
                || lower.contains("couldn't connect to host")
                || lower.contains("connection refused");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
