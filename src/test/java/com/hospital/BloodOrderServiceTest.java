package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.BloodOrderRepository;
import com.hospital.repository.BloodUnitRepository;
import com.hospital.repository.PrescriptionRepository;
import com.hospital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BloodOrderServiceTest {

    @Autowired
    private BloodOrderService bloodOrderService;

    @Autowired
    private BloodBankService bloodBankService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BloodUnitRepository bloodUnitRepository;

    @Autowired
    private BloodOrderRepository bloodOrderRepository;

    private User patient;
    private User doctor;
    private User otherPatient;
    private Prescription prescription;
    private BloodUnit bloodUnit;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();

        patient = new User("Blood Test Patient " + ts, "bpatient_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.PATIENT);
        patient = userRepository.save(patient);

        otherPatient = new User("Other Patient " + ts, "other_" + ts + "@hospital.com", "97" + (ts % 100000000), "pass123", Role.PATIENT);
        otherPatient = userRepository.save(otherPatient);

        doctor = new User("Dr. Hematology Expert " + ts, "hdoc_" + ts + "@hospital.com", "96" + (ts % 100000000), "pass123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor = userRepository.save(doctor);

        List<PrescriptionItem> items = new ArrayList<>();
        items.add(new PrescriptionItem("Folic Acid", "5mg", "1-0-0", "15 Days", "Take morning"));

        prescription = prescriptionService.createPrescription(
                null, doctor, patient,
                "Acute Blood Loss & Severe Anemia",
                "Immediate transfusion of 2 units PRBC required.",
                LocalDate.now().plusDays(7),
                items,
                BloodGroup.O_POSITIVE,
                BloodComponentType.PACKED_RED_CELLS,
                2,
                "Pre-surgical Hb correction"
        );

        bloodUnit = bloodBankService.registerBloodUnit(
                "TEST-BLD-O-" + ts,
                BloodGroup.O_POSITIVE,
                BloodComponentType.PACKED_RED_CELLS,
                "Donor Joe",
                "9870001122",
                450,
                LocalDate.now().plusDays(30),
                null
        );
    }

    @Test
    void testCreateBloodOrderWithValidPrescription() {
        BloodOrder order = bloodOrderService.createBloodOrder(
                patient,
                prescription.getId(),
                BloodGroup.O_POSITIVE,
                BloodComponentType.PACKED_RED_CELLS,
                2,
                "HOSPITAL_WARD",
                "Ward 4, Bed 12",
                "9800000000",
                "Severe Anemia",
                "URGENT",
                "HOSPITAL_BILL"
        );

        assertNotNull(order);
        assertNotNull(order.getId());
        assertTrue(order.getOrderNumber().startsWith("BLD-ORD-"));
        assertEquals(2, order.getUnits());
        assertEquals(3000.0, order.getTotalPrice());
        assertEquals(BloodOrderStatus.REQUESTED, order.getStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(prescription.getId(), order.getPrescription().getId());
        assertEquals(patient.getId(), order.getPatient().getId());
        assertEquals(doctor.getId(), order.getDoctor().getId());
    }

    @Test
    void testCreateBloodOrderFailsWithoutPrescription() {
        assertThrows(IllegalArgumentException.class, () -> {
            bloodOrderService.createBloodOrder(
                    patient,
                    null,
                    BloodGroup.O_POSITIVE,
                    BloodComponentType.PACKED_RED_CELLS,
                    1,
                    "HOSPITAL_WARD",
                    "Ward 4",
                    "9800000000",
                    "Routine",
                    "ROUTINE",
                    "ONLINE"
            );
        });
    }

    @Test
    void testCreateBloodOrderFailsWhenPrescriptionBelongsToAnotherPatient() {
        assertThrows(SecurityException.class, () -> {
            bloodOrderService.createBloodOrder(
                    otherPatient, // other patient trying to use patient's prescription
                    prescription.getId(),
                    BloodGroup.O_POSITIVE,
                    BloodComponentType.PACKED_RED_CELLS,
                    1,
                    "HOSPITAL_WARD",
                    "Ward 4",
                    "9800000000",
                    "Emergency",
                    "URGENT",
                    "HOSPITAL_BILL"
            );
        });
    }

    @Test
    void testVerifyAndAllocateAndCompleteOrder() {
        BloodOrder order = bloodOrderService.createBloodOrder(
                patient,
                prescription.getId(),
                BloodGroup.O_POSITIVE,
                BloodComponentType.PACKED_RED_CELLS,
                1,
                "HOSPITAL_WARD",
                "ICU Room 3",
                "9800000000",
                "Emergency surgery",
                "STAT_EMERGENCY",
                "ONLINE"
        );

        // Staff verifies prescription
        BloodOrder verified = bloodOrderService.verifyPrescription(order.getId(), doctor);
        assertEquals(BloodOrderStatus.VERIFIED, verified.getStatus());
        assertTrue(verified.isPrescriptionVerified());

        // Staff allocates unit
        List<Long> unitIds = List.of(bloodUnit.getId());
        BloodOrder allocated = bloodOrderService.allocateAndCrossMatch(order.getId(), unitIds, "Cross-match confirmed compatible", doctor);
        assertEquals(BloodOrderStatus.READY_FOR_COLLECTION, allocated.getStatus());
        assertNotNull(allocated.getAllocatedUnitCodes());
        assertTrue(allocated.getAllocatedUnitCodes().contains(bloodUnit.getUnitCode()));

        // Check unit inventory status updated to ISSUED
        BloodUnit updatedUnit = bloodUnitRepository.findById(bloodUnit.getId()).orElseThrow();
        assertEquals(BloodUnitStatus.ISSUED, updatedUnit.getStatus());

        // Dispatch to ward
        BloodOrder dispatched = bloodOrderService.dispatchOrder(order.getId(), "Cold chain box #4", doctor);
        assertEquals(BloodOrderStatus.DISPATCHED, dispatched.getStatus());

        // Pay order
        BloodOrder paid = bloodOrderService.payOrder(order.getId(), "ONLINE");
        assertEquals(PaymentStatus.PAID, paid.getPaymentStatus());

        // Complete order
        BloodOrder completed = bloodOrderService.completeOrder(order.getId(), doctor);
        assertEquals(BloodOrderStatus.COMPLETED, completed.getStatus());
    }

    @Test
    void testRejectOrder() {
        BloodOrder order = bloodOrderService.createBloodOrder(
                patient,
                prescription.getId(),
                BloodGroup.O_POSITIVE,
                BloodComponentType.PACKED_RED_CELLS,
                1,
                "COLLECT_AT_BLOOD_BANK",
                "Counter",
                "9800000000",
                "Elective",
                "ROUTINE",
                "HOSPITAL_BILL"
        );

        BloodOrder rejected = bloodOrderService.rejectOrder(order.getId(), "Clinical mismatch", doctor);
        assertEquals(BloodOrderStatus.REJECTED, rejected.getStatus());
        assertEquals("Clinical mismatch", rejected.getRejectionReason());
    }
}
