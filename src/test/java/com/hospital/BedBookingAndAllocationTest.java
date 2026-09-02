package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.AdmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BedBookingAndAllocationTest {

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private AdmissionRepository admissionRepository;

    private User patient;
    private User doctor;
    private User admin;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Patient BedTest " + ts, "patient_bed_" + ts + "@hospital.com", "99" + (ts % 100000000), "password123", Role.PATIENT));
        doctor = userRepository.save(new User("Dr. BedTest " + ts, "doc_bed_" + ts + "@hospital.com", "98" + (ts % 100000000), "password123", Role.DOCTOR));
        admin = userRepository.save(new User("Admin BedTest " + ts, "admin_bed_" + ts + "@hospital.com", "97" + (ts % 100000000), "password123", Role.ADMIN));

        admissionService.seedWardsAndBedsIfEmpty();
    }

    @Test
    void testWardsAndBedsSeeding() {
        List<Ward> wards = admissionService.getAllWards();
        assertFalse(wards.isEmpty(), "Wards should be seeded");

        List<Bed> allBeds = admissionService.getAllBeds();
        assertFalse(allBeds.isEmpty(), "Beds should be seeded");

        List<Bed> availableBeds = admissionService.getAvailableBeds();
        assertFalse(availableBeds.isEmpty(), "Available beds should exist");
        assertTrue(admissionService.getAvailableBedCount() > 0);
    }

    @Test
    void testPatientRequestBedBooking() {
        Admission admission = admissionService.requestBedBooking(
                patient, doctor.getId(), "Post-operative recovery & monitoring", "Requires quiet room"
        );

        assertNotNull(admission);
        assertNotNull(admission.getId());
        assertEquals(patient.getId(), admission.getPatient().getId());
        assertEquals(AdmissionStatus.REQUESTED, admission.getStatus());
        assertNull(admission.getBed());
        assertEquals("Post-operative recovery & monitoring", admission.getReason());

        List<Admission> pending = admissionService.getPendingAdmissions();
        assertTrue(pending.stream().anyMatch(a -> a.getId().equals(admission.getId())));
    }

    @Test
    void testAdminBedAllocationAndDischarge() {
        // Step 1: Patient requests bed
        Admission request = admissionService.requestBedBooking(
                patient, doctor.getId(), "General observation", "No special notes"
        );
        assertEquals(AdmissionStatus.REQUESTED, request.getStatus());

        // Step 2: Get an available bed
        List<Bed> availableBeds = admissionService.getAvailableBeds();
        assertFalse(availableBeds.isEmpty());
        Bed selectedBed = availableBeds.get(0);
        assertEquals(BedStatus.AVAILABLE, selectedBed.getStatus());

        // Step 3: Admin allocates bed to request
        Admission admitted = admissionService.allocateBedAndAdmit(request.getId(), selectedBed.getId());
        assertEquals(AdmissionStatus.ADMITTED, admitted.getStatus());
        assertNotNull(admitted.getBed());
        assertEquals(selectedBed.getId(), admitted.getBed().getId());

        // Refresh bed from database
        Bed updatedBed = bedRepository.findById(selectedBed.getId()).orElseThrow();
        assertEquals(BedStatus.OCCUPIED, updatedBed.getStatus());

        // Step 4: Admin discharges patient
        admissionService.dischargePatientSimple(admitted.getId());

        Admission discharged = admissionRepository.findById(admitted.getId()).orElseThrow();
        assertEquals(AdmissionStatus.DISCHARGED, discharged.getStatus());

        Bed freedBed = bedRepository.findById(selectedBed.getId()).orElseThrow();
        assertEquals(BedStatus.AVAILABLE, freedBed.getStatus());
    }

    @Test
    void testPatientCancelBedBooking() {
        Admission request = admissionService.requestBedBooking(
                patient, doctor.getId(), "Observation for fever", "Self request"
        );
        assertEquals(AdmissionStatus.REQUESTED, request.getStatus());

        admissionService.cancelBedBooking(request.getId(), patient);

        Admission cancelled = admissionRepository.findById(request.getId()).orElseThrow();
        assertEquals(AdmissionStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testAdminRejectBedBooking() {
        Admission request = admissionService.requestBedBooking(
                patient, doctor.getId(), "Full ICU care request", "Urgent"
        );

        admissionService.rejectBedBooking(request.getId(), "No ICU bed currently available");

        Admission rejected = admissionRepository.findById(request.getId()).orElseThrow();
        assertEquals(AdmissionStatus.CANCELLED, rejected.getStatus());
    }

    @Test
    void testAdminUpdateBedStatusAndAddInventory() {
        List<Bed> beds = admissionService.getAllBeds();
        assertFalse(beds.isEmpty());

        Bed b = beds.get(0);
        BedStatus originalStatus = b.getStatus();

        // Update bed status to MAINTENANCE
        Bed updated = admissionService.updateBedStatus(b.getId(), BedStatus.MAINTENANCE);
        assertEquals(BedStatus.MAINTENANCE, updated.getStatus());

        // Revert back
        admissionService.updateBedStatus(b.getId(), originalStatus);

        // Add custom Ward, Room, Bed
        Ward newWard = admissionService.createWard("Surgical Care Ward", "Surgical", "Post-op surgical units");
        assertNotNull(newWard.getId());

        Room newRoom = admissionService.createRoom(newWard.getId(), "S-401", "Private Suite", 1800.0);
        assertNotNull(newRoom.getId());

        Bed newBed = admissionService.createBed(newRoom.getId(), "S-401-A");
        assertNotNull(newBed.getId());
        assertEquals("S-401-A", newBed.getBedNumber());
        assertEquals(BedStatus.AVAILABLE, newBed.getStatus());
    }

    @Test
    void testPatientSelectedBedAllocationAndEnforcement() {
        List<Bed> availableBeds = admissionService.getAvailableBeds();
        assertFalse(availableBeds.isEmpty());
        Bed patientSelectedBed = availableBeds.get(0);

        // 1. Patient requests a specific bed
        Admission request = admissionService.requestBedBooking(
                patient, doctor.getId(), patientSelectedBed.getId(), "Specific bed requirement", "Prefer room near nursing station"
        );

        assertNotNull(request.getBed());
        assertEquals(patientSelectedBed.getId(), request.getBed().getId());

        // 2. Admin allocates the patient-selected bed
        Admission admitted = admissionService.allocateBedAndAdmit(request.getId(), patientSelectedBed.getId());
        assertEquals(AdmissionStatus.ADMITTED, admitted.getStatus());
        assertEquals(patientSelectedBed.getId(), admitted.getBed().getId());
    }
}
