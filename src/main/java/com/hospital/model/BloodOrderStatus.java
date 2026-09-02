package com.hospital.model;

public enum BloodOrderStatus {
    REQUESTED("Requested", "Order submitted with doctor prescription, awaiting blood bank verification"),
    VERIFIED("Prescription Verified", "Doctor prescription confirmed, scheduled for cross-matching"),
    CROSSMATCH_TESTING("Cross-matching & Testing", "Serological testing & donor unit compatibility in progress"),
    READY_FOR_COLLECTION("Ready for Transfusion", "Compatible blood units ready for ward collection or delivery"),
    DISPATCHED("Dispatched to Ward", "Cold-chain dispatched to specified hospital ward or bed"),
    COMPLETED("Completed", "Transfusion units handed over and requisition fulfilled"),
    CANCELLED("Cancelled", "Order cancelled by patient or physician"),
    REJECTED("Rejected", "Order rejected due to stock unavailability or prescription mismatch");

    private final String label;
    private final String description;

    BloodOrderStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
