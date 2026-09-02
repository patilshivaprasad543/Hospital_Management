package com.hospital.model;

public enum AmbulanceType {
    BASIC("Basic Life Support"),
    ADVANCED_LIFE_SUPPORT("Advanced Life Support (ALS)"),
    PATIENT_TRANSPORT("Patient Transport"),
    ICU_AMBULANCE("ICU Ambulance"),
    NEONATAL("Neonatal Care Ambulance"),
    CARDIAC("Cardiac Care Ambulance"),
    OTHER("Specialized Transport");

    private final String label;

    AmbulanceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
