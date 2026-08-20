package com.hospital.dto;

import com.hospital.model.NotificationDispatchLog;
import com.hospital.util.SensitiveContentMasker;

import java.time.LocalDateTime;

public class NotificationLogEntry {

    private final LocalDateTime timestamp;
    private final String channel;
    private final String recipient;
    private final String subject;
    private final String body;
    private final boolean delivered;
    private final String note;
    private final boolean otpMessage;

    public NotificationLogEntry(String channel, String recipient, String subject, String body,
                                boolean delivered, String note) {
        this.timestamp = LocalDateTime.now();
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.delivered = delivered;
        this.note = note;
        this.otpMessage = SensitiveContentMasker.isOtpRelated(subject, body);
    }

    private NotificationLogEntry(LocalDateTime timestamp, String channel, String recipient, String subject,
                                 String body, boolean delivered, String note, boolean otpMessage) {
        this.timestamp = timestamp;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.delivered = delivered;
        this.note = note;
        this.otpMessage = otpMessage;
    }

    public static NotificationLogEntry fromEntity(NotificationDispatchLog entity) {
        String subject = entity.getSubject();
        String body = entity.getBody();
        return new NotificationLogEntry(
                entity.getCreatedAt(),
                entity.getChannel(),
                entity.getRecipient(),
                SensitiveContentMasker.displaySubject(subject),
                SensitiveContentMasker.displayBody(subject, body),
                entity.isDelivered(),
                entity.getNote(),
                SensitiveContentMasker.isOtpRelated(subject, body)
        );
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

    public boolean isOtpMessage() {
        return otpMessage;
    }
}
