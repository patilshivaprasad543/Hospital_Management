package com.hospital.dto;

import java.time.LocalDate;

public record PatientIntake(
        LocalDate dateOfBirth,
        String gender,
        String address,
        String bloodGroup,
        String emergencyContactName,
        String emergencyContactPhone,
        String allergies,
        String medicalHistory
) {
    public static PatientIntake basic(LocalDate dateOfBirth, String gender, String address) {
        return new PatientIntake(dateOfBirth, gender, address, null, null, null, null, null);
    }
}
