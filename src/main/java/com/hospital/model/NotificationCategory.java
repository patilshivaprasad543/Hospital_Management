package com.hospital.model;

public enum NotificationCategory {
    APPOINTMENT("Appointment"),
    PRESCRIPTION("Prescription"),
    LABORATORY("Laboratory"),
    PHARMACY("Pharmacy"),
    BILLING("Billing"),
    REMINDER("Reminder"),
    SYSTEM("General Update");

    private final String label;

    NotificationCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
