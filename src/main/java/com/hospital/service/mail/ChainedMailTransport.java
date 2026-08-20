package com.hospital.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ChainedMailTransport implements MailTransport {

    private static final Logger logger = LoggerFactory.getLogger(ChainedMailTransport.class);

    private final List<MailTransport> transports;

    public ChainedMailTransport(MailTransport... transports) {
        this.transports = List.of(transports);
    }

    @Override
    public String providerName() {
        return transports.stream()
                .filter(MailTransport::isConfigured)
                .map(MailTransport::providerName)
                .findFirst()
                .orElse("none");
    }

    @Override
    public boolean isConfigured() {
        return transports.stream().anyMatch(MailTransport::isConfigured);
    }

    @Override
    public boolean send(String to, String subject, String body, String type) {
        boolean attempted = false;
        for (MailTransport transport : transports) {
            if (!transport.isConfigured()) {
                continue;
            }
            attempted = true;
            if (transport.send(to, subject, body, type)) {
                return true;
            }
            logger.warn("Email delivery via {} failed for {}, trying next provider if available", transport.providerName(), to);
        }
        return false;
    }
}
