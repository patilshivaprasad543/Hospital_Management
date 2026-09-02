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
        return createPrescription(appointment, doctor, patient, diagnosis, instructions, followUpDate, items, null, null, null, null);
    }

    public Prescription createPrescription(Appointment appointment, User doctor, User patient, String diagnosis, String instructions,
                                          LocalDate followUpDate, List<PrescriptionItem> items,
                                          BloodGroup bloodGroup, BloodComponentType bloodComponentType, Integer bloodUnits, String bloodTransfusionReason) {
        boolean hasMeds = items != null && !items.isEmpty();
        boolean hasBlood = (bloodUnits != null && bloodUnits > 0) || bloodGroup != null;

        if (!hasMeds && !hasBlood) {
            throw new IllegalArgumentException("Add at least one medicine or blood transfusion order to the prescription.");
        }

        Prescription prescription = new Prescription(appointment, doctor, patient, diagnosis, instructions, followUpDate);
        if (hasMeds) {
            for (PrescriptionItem item : items) {
                prescription.addItem(item);
            }
        }

        if (hasBlood) {
            prescription.setBloodGroup(bloodGroup);
            prescription.setBloodComponentType(bloodComponentType);
            prescription.setBloodUnits(bloodUnits != null ? bloodUnits : 1);
            prescription.setBloodTransfusionReason(bloodTransfusionReason);
        }

        Prescription saved = prescriptionRepository.save(prescription);

        String notifMsg = hasBlood
                ? "Dr. " + doctor.getFullName() + " has issued a prescription including blood transfusion requirements (" + (bloodGroup != null ? bloodGroup.getLabel() : "Blood") + "). You can now order blood or medicines."
                : "Dr. " + doctor.getFullName() + " has generated a digital prescription for your consultation. View diagnosis and prescribed medicines.";

        notificationService.sendPortalNotification(
            patient,
            hasBlood ? "🩸 Prescription & Blood Order Issued" : "💊 Digital Prescription Issued",
            notifMsg,
            NotificationCategory.PRESCRIPTION,
            hasBlood ? "/patient/blood-bank/buy?prescriptionId=" + saved.getId() : "/patient/prescriptions"
        );

        if (hasMeds) {
            userService.findVendors().stream()
                    .filter(v -> v.getVendorType() == VendorType.PHARMACY)
                    .forEach(vendor -> notificationService.sendPortalNotification(
                            vendor,
                            "💊 New Prescription Received",
                            "A new prescription for patient " + patient.getFullName() + " is available. Prepare medicines.",
                            NotificationCategory.PHARMACY,
                            "/vendor/dashboard"
                    ));
        }

        return saved;
    }

    public List<Prescription> getPatientPrescriptions(User patient) {
        return prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Prescription> getDoctorPrescriptions(User doctor) {
        return prescriptionRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }

    public List<Prescription> getAllPrescriptions() {
        return prescriptionRepository.findAllDetailed();
    }

    public long countPrescriptions() {
        return prescriptionRepository.count();
    }

    public Optional<Prescription> findByIdForPatient(Long prescriptionId, User patient) {
        return prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient).stream()
                .filter(p -> p.getId().equals(prescriptionId))
                .findFirst();
    }
}
