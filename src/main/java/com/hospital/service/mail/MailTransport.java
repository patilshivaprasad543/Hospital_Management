package com.hospital.service.mail;

public interface MailTransport {

    String providerName();

    boolean isConfigured();

    boolean send(String to, String subject, String body, String type);
}
