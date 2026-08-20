package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    public Prescription createPrescription(Appointment appointment, User doctor, User patient, String diagnosis, String instructions, LocalDate followUpDate, List<PrescriptionItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Add at least one medicine to the prescription.");
        }
        Prescription prescription = new Prescription(appointment, doctor, patient, diagnosis, instructions, followUpDate);
        if (items != null) {
            for (PrescriptionItem item : items) {
                prescription.addItem(item);
            }
        }
        Prescription saved = prescriptionRepository.save(prescription);

        notificationService.sendPortalNotification(
            patient,
            "💊 Digital Prescription Issued",
            "Dr. " + doctor.getFullName() + " has generated a digital prescription for your consultation. View diagnosis and prescribed medicines.",
            NotificationCategory.PRESCRIPTION,
            "/patient/prescriptions"
        );

        userService.findVendors().stream()
                .filter(v -> v.getVendorType() == VendorType.PHARMACY)
                .forEach(vendor -> notificationService.sendPortalNotification(
                        vendor,
                        "💊 New Prescription Received",
                        "A new prescription for patient " + patient.getFullName() + " is available. Prepare medicines.",
                        NotificationCategory.PHARMACY,
                        "/vendor/dashboard"
                ));

        return saved;
    }

    public List<Prescription> getPatientPrescriptions(User patient) {
        return prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Prescription> getDoctorPrescriptions(User doctor) {
        return prescriptionRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }

    public Optional<Prescription> findByIdForPatient(Long prescriptionId, User patient) {
        return prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient).stream()
                .filter(p -> p.getId().equals(prescriptionId))
                .findFirst();
    }
}
