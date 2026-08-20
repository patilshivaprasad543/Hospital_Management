package com.hospital.config;

import com.hospital.service.mail.BrevoMailTransport;
import com.hospital.service.mail.ChainedMailTransport;
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
            @Value("${smartcare.mail.provider:auto}") String provider,
            SmtpMailTransport smtpMailTransport,
            BrevoMailTransport brevoMailTransport) {
        String selected = provider == null ? "auto" : provider.trim().toLowerCase();
        MailTransport transport = switch (selected) {
            case "brevo", "sendinblue" -> brevoMailTransport;
            case "smtp", "gmail" -> smtpMailTransport;
            case "auto" -> new ChainedMailTransport(brevoMailTransport, smtpMailTransport);
            default -> new ChainedMailTransport(brevoMailTransport, smtpMailTransport);
        };

        if (transport.isConfigured()) {
            logger.info("Email provider ready: {}", transport.providerName());
        } else {
            logger.warn("Email is NOT configured — registration OTP will fail.");
            logger.warn("Render: set SMARTCARE_BREVO_API_KEY (xkeysib-...), SMARTCARE_BREVO_SENDER_EMAIL (verified in Brevo), SMARTCARE_MAIL_PROVIDER=brevo");
            logger.warn("Local: set SMARTCARE_MAIL_USERNAME + SMARTCARE_MAIL_PASSWORD in .env, run ./scripts/start.sh");
        }
        if (!brevoMailTransport.isConfigured()) {
            logger.warn("Brevo status: {}", brevoMailTransport.configurationHint());
        }
        return transport;
    }
}
