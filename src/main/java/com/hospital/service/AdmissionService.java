package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AdmissionService {

    @Autowired
    private AdmissionRepository admissionRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private DischargeSummaryRepository dischargeSummaryRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Admission requestAdmission(User doctor, User patient, String reason, String notes) {
        Admission admission = new Admission(patient, doctor, reason, notes);
        Admission saved = admissionRepository.save(admission);

        notificationService.sendPortalNotification(
                patient,
                "🏥 Admission Recommended",
                "Dr. " + (doctor != null ? doctor.getFullName() : "Medical Staff") + " has recommended hospital admission for: " + reason,
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );

        return saved;
    }

    @Transactional
    public Admission requestBedBooking(User patient, Long doctorId, Long bedId, String reason, String notes) {
        User doctor = null;
        if (doctorId != null) {
            doctor = userRepository.findById(doctorId).orElse(null);
        }
        if (doctor == null) {
            List<User> doctors = userRepository.findByRole(Role.DOCTOR);
            if (!doctors.isEmpty()) {
                doctor = doctors.get(0);
            }
        }

        Bed bed = null;
        if (bedId != null) {
            bed = bedRepository.findById(bedId).orElse(null);
        }

        Admission admission = new Admission();
        admission.setPatient(patient);
        admission.setDoctor(doctor);
        admission.setBed(bed);
        admission.setReason(reason);
        admission.setNotes(notes);
        admission.setStatus(AdmissionStatus.REQUESTED);

        Admission saved = admissionRepository.save(admission);

        String bedMsg = (bed != null) ? (" for Bed " + bed.getBedNumber() + " (" + bed.getRoom().getWard().getName() + ")") : "";
        notificationService.sendPortalNotification(
                patient,
                "🛏 Bed Booking Request Submitted",
                "Your bed booking request" + bedMsg + " for '" + reason + "' has been submitted. Admin will allocate your selected bed shortly.",
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );

        return saved;
    }

    @Transactional
    public Admission requestBedBooking(User patient, Long doctorId, String reason, String notes) {
        return requestBedBooking(patient, doctorId, null, reason, notes);
    }

    @Transactional
    public Admission directAllocateAndAdmit(Long patientId, Long doctorId, Long bedId, String reason, String notes) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient record not found"));

        Admission admission = requestBedBooking(patient, doctorId, bedId, (reason != null && !reason.isBlank() ? reason : "Inpatient Admission"), notes);
        return allocateBedAndAdmit(admission.getId(), bedId);
    }

    @Transactional
    public Admission allocateBedAndAdmit(Long admissionId, Long bedId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission record not found"));

        Bed bed = null;
        if (bedId != null) {
            bed = bedRepository.findById(bedId).orElse(null);
        }
        if (bed == null && admission.getBed() != null) {
            bed = admission.getBed();
        }
        if (bed == null) {
            throw new RuntimeException("No bed specified for allocation.");
        }

        // Enforce that if patient selected a specific bed, only that selected bed can be allocated!
        if (admission.getBed() != null && !admission.getBed().getId().equals(bed.getId())) {
            throw new RuntimeException("Admin can only allocate the patient-selected bed (Bed " + admission.getBed().getBedNumber() + ").");
        }

        if (bed.getStatus() != BedStatus.AVAILABLE && bed.getStatus() != BedStatus.RESERVED) {
            throw new RuntimeException("Selected bed (" + bed.getBedNumber() + ") is not available.");
        }

        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        admission.setBed(bed);
        admission.setStatus(AdmissionStatus.ADMITTED);
        if (admission.getAdmissionDate() == null) {
            admission.setAdmissionDate(LocalDateTime.now());
        }

        Admission saved = admissionRepository.save(admission);

        notificationService.sendPortalNotification(
                admission.getPatient(),
                "🛏 Bed Allocated & Admitted",
                "You have been allocated Bed " + bed.getBedNumber() + " in Room " + bed.getRoom().getRoomNumber() + " (" + bed.getRoom().getWard().getName() + ").",
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );

        return saved;
    }

    @Transactional
    public void cancelBedBooking(Long admissionId, User patient) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Bed booking request not found"));

        if (!admission.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized to cancel this bed booking");
        }

        if (admission.getStatus() == AdmissionStatus.DISCHARGED || admission.getStatus() == AdmissionStatus.CANCELLED) {
            throw new RuntimeException("Booking request is already closed.");
        }

        if (admission.getBed() != null) {
            Bed bed = admission.getBed();
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
            admission.setBed(null);
        }

        admission.setStatus(AdmissionStatus.CANCELLED);
        admissionRepository.save(admission);
    }

    @Transactional
    public void rejectBedBooking(Long admissionId, String reason) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Bed booking request not found"));

        if (admission.getBed() != null) {
            Bed bed = admission.getBed();
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
            admission.setBed(null);
        }

        admission.setStatus(AdmissionStatus.CANCELLED);
        admissionRepository.save(admission);

        notificationService.sendPortalNotification(
                admission.getPatient(),
                "❌ Bed Request Update",
                "Your bed allocation request was cancelled/rejected: " + (reason != null ? reason : "Unavailable capacity."),
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );
    }

    @Transactional
    public DischargeSummary dischargePatient(Long admissionId, String finalDiagnosis, String treatmentSummary,
                                             String dischargeMedications, String followUpInstructions) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission record not found"));

        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new RuntimeException("Patient is not currently admitted.");
        }

        if (admission.getBed() != null) {
            Bed bed = admission.getBed();
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setActualDischargeDate(LocalDateTime.now());
        admissionRepository.save(admission);

        DischargeSummary summary = new DischargeSummary(admission, finalDiagnosis, treatmentSummary, dischargeMedications, followUpInstructions);
        DischargeSummary savedSummary = dischargeSummaryRepository.save(summary);

        notificationService.sendPortalNotification(
                admission.getPatient(),
                "📜 Patient Discharged",
                "Your discharge summary has been generated. Please review your follow-up instructions.",
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );

        return savedSummary;
    }

    @Transactional
    public void dischargePatientSimple(Long admissionId) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new RuntimeException("Admission record not found"));

        if (admission.getBed() != null) {
            Bed bed = admission.getBed();
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setActualDischargeDate(LocalDateTime.now());
        admissionRepository.save(admission);

        notificationService.sendPortalNotification(
                admission.getPatient(),
                "📜 Bed Released & Discharged",
                "You have been discharged and your bed has been released.",
                NotificationCategory.SYSTEM,
                "/patient/beds"
        );
    }

    @Transactional
    public Bed updateBedStatus(Long bedId, BedStatus status) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new RuntimeException("Bed not found"));
        bed.setStatus(status);
        return bedRepository.save(bed);
    }

    @Transactional
    public Ward createWard(String name, String category, String description) {
        Ward ward = new Ward(name, category, description);
        return wardRepository.save(ward);
    }

    @Transactional
    public Room createRoom(Long wardId, String roomNumber, String roomType, Double dailyRate) {
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new RuntimeException("Ward not found"));
        Room room = new Room(roomNumber, ward, roomType, dailyRate);
        return roomRepository.save(room);
    }

    @Transactional
    public Bed createBed(Long roomId, String bedNumber) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        Bed bed = new Bed(bedNumber, room, BedStatus.AVAILABLE);
        return bedRepository.save(bed);
    }

    public List<Admission> getAdmissionsByPatient(User patient) {
        return admissionRepository.findByPatientOrderByIdDesc(patient);
    }

    public List<Admission> getPendingAdmissions() {
        return admissionRepository.findByStatusOrderByIdDesc(AdmissionStatus.REQUESTED);
    }

    public List<Admission> getActiveAdmissions() {
        return admissionRepository.findByStatusOrderByIdDesc(AdmissionStatus.ADMITTED);
    }

    public List<Admission> getAllAdmissions() {
        return admissionRepository.findAllByOrderByIdDesc();
    }

    public long getActiveAdmissionCount() {
        return admissionRepository.countByStatus(AdmissionStatus.ADMITTED);
    }

    public long getPendingAdmissionCount() {
        return admissionRepository.countByStatus(AdmissionStatus.REQUESTED);
    }

    public List<Bed> getAllBeds() {
        return bedRepository.findAllByOrderByRoomWardNameAscRoomRoomNumberAscBedNumberAsc();
    }

    public List<Bed> getAvailableBeds() {
        return bedRepository.findByStatusOrderByBedNumberAsc(BedStatus.AVAILABLE);
    }

    public long getAvailableBedCount() {
        return bedRepository.countByStatus(BedStatus.AVAILABLE);
    }

    public long getOccupiedBedCount() {
        return bedRepository.countByStatus(BedStatus.OCCUPIED);
    }

    public long getMaintenanceBedCount() {
        return bedRepository.countByStatus(BedStatus.MAINTENANCE);
    }

    public long getTotalBedCount() {
        return bedRepository.count();
    }

    public List<Ward> getAllWards() {
        return wardRepository.findAll();
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Transactional
    public void seedWardsAndBedsIfEmpty() {
        if (wardRepository.count() > 0) {
            return;
        }

        Ward generalWard = wardRepository.save(new Ward("General Ward", "General", "Multi-bed standard patient care units."));
        Ward icuWard = wardRepository.save(new Ward("Intensive Care Unit (ICU)", "ICU", "Critical care unit equipped with advanced monitoring."));
        Ward deluxeWard = wardRepository.save(new Ward("Deluxe Private Ward", "Deluxe", "Single occupancy private rooms with amenities."));
        Ward pedsWard = wardRepository.save(new Ward("Pediatric Ward", "Pediatric", "Child and adolescent specialized care unit."));
        Ward emergencyWard = wardRepository.save(new Ward("Emergency Triage Ward", "Emergency", "Short-stay immediate emergency observation beds."));

        Room r101 = roomRepository.save(new Room("101", generalWard, "Shared (3-Bed)", 500.0));
        Room r102 = roomRepository.save(new Room("102", generalWard, "Shared (2-Bed)", 600.0));
        Room rIcu1 = roomRepository.save(new Room("ICU-01", icuWard, "Single Private ICU", 2500.0));
        Room rIcu2 = roomRepository.save(new Room("ICU-02", icuWard, "Single Private ICU", 2500.0));
        Room rDeluxe1 = roomRepository.save(new Room("D-201", deluxeWard, "Deluxe Suite", 1500.0));
        Room rDeluxe2 = roomRepository.save(new Room("D-202", deluxeWard, "Deluxe Suite", 1500.0));
        Room rPeds1 = roomRepository.save(new Room("P-301", pedsWard, "Shared (2-Bed)", 700.0));
        Room rEr1 = roomRepository.save(new Room("ER-01", emergencyWard, "Emergency Bay", 900.0));

        bedRepository.save(new Bed("G-101-A", r101, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("G-101-B", r101, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("G-101-C", r101, BedStatus.AVAILABLE));

        bedRepository.save(new Bed("G-102-A", r102, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("G-102-B", r102, BedStatus.AVAILABLE));

        bedRepository.save(new Bed("ICU-01-A", rIcu1, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("ICU-02-A", rIcu2, BedStatus.AVAILABLE));

        bedRepository.save(new Bed("D-201-A", rDeluxe1, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("D-202-A", rDeluxe2, BedStatus.AVAILABLE));

        bedRepository.save(new Bed("P-301-A", rPeds1, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("P-301-B", rPeds1, BedStatus.AVAILABLE));

        bedRepository.save(new Bed("ER-01-A", rEr1, BedStatus.AVAILABLE));
        bedRepository.save(new Bed("ER-01-B", rEr1, BedStatus.AVAILABLE));

        System.out.println(">>> Seeded 5 Wards, 8 Rooms, and 13 Beds <<<");
    }
}
