package com.hospital.model;

public enum Role {
    PATIENT("Patient"),
    DOCTOR("Doctor"),
    ADMIN("Administrator"),
    VENDOR("Vendor");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
