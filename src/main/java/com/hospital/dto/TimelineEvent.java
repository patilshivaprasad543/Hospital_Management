package com.hospital.dto;

import java.time.LocalDateTime;

public class TimelineEvent {

    private final LocalDateTime occurredAt;
    private final String type;
    private final String title;
    private final String description;
    private final String badgeClass;

    public TimelineEvent(LocalDateTime occurredAt, String type, String title, String description, String badgeClass) {
        this.occurredAt = occurredAt;
        this.type = type;
        this.title = title;
        this.description = description;
        this.badgeClass = badgeClass;
    }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getBadgeClass() { return badgeClass; }
}
