package com.hospital.model;

public enum AppointmentState {
    REQUESTED,
    PENDING_DOCTOR_APPROVAL,
    ACCEPTED,
    CONFIRMED,
    CHECKED_IN,
    IN_CONSULTATION,
    COMPLETED,
    REJECTED,
    CANCELLED,
    EXPIRED
}
