package com.hospital.model;

import java.util.List;

public enum PharmacyOrderStatus {
    PLACED("Pending"),
    ACCEPTED("Accepted"),
    PROCESSING("Preparing"),
    READY_FOR_PICKUP("Ready for pickup"),
    DISPATCHED("Dispatched"),
    DELIVERED("Delivered"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    PharmacyOrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    public List<PharmacyOrderStatus> nextStatuses() {
        return switch (this) {
            case PLACED -> List.of(ACCEPTED, CANCELLED);
            case ACCEPTED -> List.of(PROCESSING, CANCELLED);
            case PROCESSING -> List.of(READY_FOR_PICKUP, DISPATCHED, CANCELLED);
            case READY_FOR_PICKUP -> List.of(DISPATCHED, DELIVERED, CANCELLED);
            case DISPATCHED -> List.of(DELIVERED, CANCELLED);
            case DELIVERED -> List.of(COMPLETED);
            default -> List.of();
        };
    }
}
