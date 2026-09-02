package com.hospital.model;

public enum ConsultationType {
    IN_PERSON("In-Person Consultation"),
    VIDEO("Video Consultation"),
    AUDIO("Audio Consultation");

    private final String label;

    ConsultationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
