package com.hospital.config;

import com.hospital.service.mail.BrevoMailTransport;
import com.hospital.service.mail.MailTransport;
import com.hospital.service.mail.SmtpMailTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MailConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MailConfiguration.class);

    @Bean
    public MailTransport mailTransport(
            @Value("${smartcare.mail.provider:smtp}") String provider,
            SmtpMailTransport smtpMailTransport,
            BrevoMailTransport brevoMailTransport) {
        String selected = provider == null ? "smtp" : provider.trim().toLowerCase();
        MailTransport transport = switch (selected) {
            case "brevo", "sendinblue" -> brevoMailTransport;
            default -> smtpMailTransport;
        };

        if (transport.isConfigured()) {
            logger.info("Email provider: {} (configured)", transport.providerName());
        } else {
            logger.warn("Email provider: {} is NOT configured — OTP and password-reset emails will fail. "
                            + "For Render free hosting, use SMARTCARE_MAIL_PROVIDER=brevo with a Brevo API key "
                            + "(Gmail SMTP is blocked on free Render).",
                    transport.providerName());
        }
        return transport;
    }
}
