package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.UserRepository;
import com.hospital.service.BillingService;
import com.hospital.service.InsuranceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InsuranceServiceTest {

    @Autowired
    private InsuranceService insuranceService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private UserRepository userRepository;

    private User patient;
    private User admin;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Test Insurance Patient " + ts, "inspat_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT));
        admin = userRepository.save(new User("Admin User " + ts, "admin_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.ADMIN));
    }

    @Test
    void testRegisterInsurancePolicy() {
        Insurance insurance = insuranceService.registerPolicy(patient, "Star Health", "POL-TEST-999", "Comprehensive",
                LocalDate.now(), LocalDate.now().plusYears(1), 500000.0);

        assertNotNull(insurance);
        assertEquals("POL-TEST-999", insurance.getPolicyNumber());

        Optional<Insurance> retrieved = insuranceService.getPatientInsurance(patient);
        assertTrue(retrieved.isPresent());
        assertEquals("Star Health", retrieved.get().getProvider());
    }

    @Test
    void testSubmitClaimAndMetrics() {
        Insurance insurance = insuranceService.registerPolicy(patient, "HDFC ERGO", "HDFC-TEST-123", "Gold Floater",
                LocalDate.now(), LocalDate.now().plusYears(1), 300000.0);

        Invoice invoice = billingService.createInvoice(patient, "LABORATORY", "Pathology diagnostic workup", 7500.0, null);

        InsuranceClaim claim = insuranceService.submitClaim(insurance, invoice, 7500.0, "OPD checkup claim", patient);
        assertNotNull(claim.getId());
        assertEquals("SUBMITTED", claim.getStatus());
        assertEquals(7500.0, claim.getClaimAmount());

        List<InsuranceClaim> allClaims = insuranceService.getAllClaims();
        assertTrue(allClaims.stream().anyMatch(c -> c.getId().equals(claim.getId())));

        assertTrue(insuranceService.countPendingClaims() >= 1);

        // Update status to APPROVED
        InsuranceClaim approved = insuranceService.updateClaimStatus(claim.getId(), "APPROVED", "All documents verified", admin);
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("All documents verified", approved.getRemarks());

        // Test filtering
        List<InsuranceClaim> approvedClaims = insuranceService.getClaimsFiltered("APPROVED");
        assertTrue(approvedClaims.stream().anyMatch(c -> c.getId().equals(claim.getId())));
    }
}
