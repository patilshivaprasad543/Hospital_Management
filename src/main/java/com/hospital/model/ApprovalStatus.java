package com.hospital.model;

public enum ApprovalStatus {
    PENDING_OTP("Awaiting email verification"),
    PENDING_DOCUMENTS("Documents required"),
    PENDING_ADMIN("Awaiting approval"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ApprovalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
