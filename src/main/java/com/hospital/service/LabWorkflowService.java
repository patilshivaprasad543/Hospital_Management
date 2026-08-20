package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.LabRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LabWorkflowService {

    @Autowired
    private LabRequestRepository labRequestRepository;

    @Autowired
    private NotificationService notificationService;

    public LabRequest requestLabTest(User doctor, User patient, String testName, String notes) {
        LabRequest labRequest = new LabRequest(doctor, patient, testName, notes);
        LabRequest saved = labRequestRepository.save(labRequest);

        notificationService.sendPortalNotification(
            patient,
            "🔬 Diagnostic Test Requested",
            "Dr. " + doctor.getFullName() + " recommended a diagnostic lab test (" + testName + "). Select a laboratory vendor to proceed.",
            NotificationCategory.LABORATORY,
            "/patient/lab-reports"
        );

        return saved;
    }

    public LabRequest assignVendorAndBook(Long labRequestId, User labVendor) {
        LabRequest req = labRequestRepository.findById(labRequestId)
                .orElseThrow(() -> new RuntimeException("Lab request not found"));
        req.setLabVendor(labVendor);
        req.setStatus("PROCESSING");
        LabRequest saved = labRequestRepository.save(req);

        if (labVendor != null) {
            notificationService.sendPortalNotification(
                labVendor,
                "🧪 New Lab Sample Request",
                "New test booking (" + req.getTestName() + ") assigned for Patient " + req.getPatient().getFullName(),
                NotificationCategory.LABORATORY,
                "/vendor/dashboard"
            );
        }

        return saved;
    }

    public LabRequest uploadReport(Long labRequestId, String reportResult) {
        LabRequest req = labRequestRepository.findById(labRequestId)
                .orElseThrow(() -> new RuntimeException("Lab request not found"));
        req.setReportResult(reportResult);
        req.setStatus("REPORT_READY");
        LabRequest saved = labRequestRepository.save(req);

        notificationService.sendPortalNotification(
            req.getPatient(),
            "📋 Diagnostic Report Ready",
            "Your lab test report for (" + req.getTestName() + ") is now ready. View and download online.",
            NotificationCategory.LABORATORY,
            "/patient/lab-reports"
        );

        notificationService.sendPortalNotification(
            req.getDoctor(),
            "📋 Patient Lab Report Ready",
            "Lab report for patient " + req.getPatient().getFullName() + " (" + req.getTestName() + ") is now available for review.",
            NotificationCategory.LABORATORY,
            "/doctor/dashboard"
        );

        return saved;
    }

    public List<LabRequest> getDoctorLabRequests(User doctor) {
        return labRequestRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }

    public List<LabRequest> getPatientLabRequests(User patient) {
        return labRequestRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<LabRequest> getVendorLabRequests(User vendor) {
        return labRequestRepository.findByLabVendorOrderByCreatedAtDesc(vendor);
    }

    public List<LabRequest> getAllLabRequests() {
        return labRequestRepository.findAllDetailed();
    }

    public Optional<LabRequest> findById(Long id) {
        return labRequestRepository.findDetailedById(id)
                .or(() -> labRequestRepository.findById(id));
    }

    public Optional<LabRequest> findByIdForDoctor(Long id, User doctor) {
        return getDoctorLabRequests(doctor).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }

    public Optional<LabRequest> findByIdForVendor(Long id, User vendor) {
        return getVendorLabRequests(vendor).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst();
    }
}
