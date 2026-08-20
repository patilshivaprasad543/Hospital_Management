package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_dispatch_logs", indexes = {
        @Index(name = "idx_dispatch_log_created", columnList = "created_at"),
        @Index(name = "idx_dispatch_log_recipient", columnList = "recipient")
})
public class NotificationDispatchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false)
    private String recipient;

    @Column(length = 500)
    private String subject;

    @Column(length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean delivered;

    @Column(length = 500)
    private String note;

    public NotificationDispatchLog() {
    }

    public NotificationDispatchLog(String channel, String recipient, String subject, String body,
                                   boolean delivered, String note) {
        this.createdAt = LocalDateTime.now();
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.delivered = delivered;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
