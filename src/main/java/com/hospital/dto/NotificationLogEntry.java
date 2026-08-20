package com.hospital.dto;

import com.hospital.model.NotificationDispatchLog;

import java.time.LocalDateTime;

public class NotificationLogEntry {

    private final LocalDateTime timestamp;
    private final String channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final boolean delivered;
    private final String note;

    public NotificationLogEntry(String channel, String recipient, String subject, String body,
                                boolean delivered, String note) {
        this.timestamp = LocalDateTime.now();
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.delivered = delivered;
        this.note = note;
    }

    private NotificationLogEntry(LocalDateTime timestamp, String channel, String recipient, String subject,
                                 String body, boolean delivered, String note) {
        this.timestamp = timestamp;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.delivered = delivered;
        this.note = note;
    }

    public static NotificationLogEntry fromEntity(NotificationDispatchLog entity) {
        return new NotificationLogEntry(
                entity.getCreatedAt(),
                entity.getChannel(),
                entity.getRecipient(),
                entity.getSubject(),
                maskSensitiveContent(entity.getSubject(), entity.getBody()),
                entity.isDelivered(),
                entity.getNote()
        );
    }

    /** Redact OTP codes from message bodies shown in the admin portal. */
    private static String maskSensitiveContent(String subject, String body) {
        if (body == null) {
            return null;
        }
        if (subject != null && subject.toLowerCase().contains("otp")) {
            return body.replaceAll("\\b\\d{6}\\b", "******");
        }
        return body;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public String getNote() {
        return note;
    }
}
