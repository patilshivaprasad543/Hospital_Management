package com.hospital.model;

public enum VendorType {
    LABORATORY("Laboratory"),
    PHARMACY("Pharmacy"),
    NONE("—");

    private final String label;

    VendorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
