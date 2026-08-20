package com.hospital.dto;

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
