package com.hospital.model;

public enum BloodComponentType {
    WHOLE_BLOOD("Whole Blood"),
    PACKED_RED_CELLS("Packed Red Blood Cells (PRBC)"),
    FRESH_FROZEN_PLASMA("Fresh Frozen Plasma (FFP)"),
    PLATELETS("Platelet Concentrates"),
    CRYOPRECIPITATE("Cryoprecipitate");

    private final String label;

    BloodComponentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
