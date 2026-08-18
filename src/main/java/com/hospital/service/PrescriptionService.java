package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private NotificationService notificationService;

    public Prescription createPrescription(Appointment appointment, User doctor, User patient, String diagnosis, String instructions, LocalDate followUpDate, List<PrescriptionItem> items) {
        Prescription prescription = new Prescription(appointment, doctor, patient, diagnosis, instructions, followUpDate);
        if (items != null) {
            for (PrescriptionItem item : items) {
                prescription.addItem(item);
            }
        }
        Prescription saved = prescriptionRepository.save(prescription);

        notificationService.sendNotification(
            patient,
            "💊 Digital Prescription Issued",
            "Dr. " + doctor.getFullName() + " has generated a digital prescription for your consultation. View diagnosis and prescribed medicines.",
            NotificationCategory.PRESCRIPTION,
            "/patient/prescriptions"
        );

        return saved;
    }

    public List<Prescription> getPatientPrescriptions(User patient) {
        return prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Prescription> getDoctorPrescriptions(User doctor) {
        return prescriptionRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }
}
