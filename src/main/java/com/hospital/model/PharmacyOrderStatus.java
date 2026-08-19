package com.hospital.model;

public enum PharmacyOrderStatus {
    PLACED("Order Placed"),
    ACCEPTED("Accepted by Pharmacy"),
    PROCESSING("Processing"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    PharmacyOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }
}
