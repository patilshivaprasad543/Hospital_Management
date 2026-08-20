package com.hospital.model;

public enum AppointmentStatus {
    PENDING("Pending"),
    CONFIRMED("Accepted"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled"),
    COMPLETED("Completed");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
