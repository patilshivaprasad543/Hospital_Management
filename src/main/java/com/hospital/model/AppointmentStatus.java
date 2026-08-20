package com.hospital.model;

public enum AppointmentStatus {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    REJECTED("Declined"),
    COMPLETED("Completed");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
