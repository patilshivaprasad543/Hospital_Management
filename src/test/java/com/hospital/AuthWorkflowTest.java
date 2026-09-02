package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.UserRepository;
import com.hospital.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthWorkflowTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testEndToEndUserRegistrationAndLogin() {
        // Register Patient
        User patientUser = new User("Workflow Patient", "wfpatient@hospital.com", "9112233445", "Pass@1234", Role.PATIENT);
        User patient = userService.registerUser(patientUser);
        assertNotNull(patient);
        assertEquals(Role.PATIENT, patient.getRole());

        // Login check
        User loggedInPatient = userService.loginUser("wfpatient@hospital.com", "Pass@1234").orElse(null);
        assertNotNull(loggedInPatient);
        assertEquals("Workflow Patient", loggedInPatient.getFullName());

        // Register Doctor (requires admin approval)
        User docUser = new User("Dr. Workflow Doc", "wfdoc@hospital.com", "9112233446", "Pass@1234", Role.DOCTOR);
        User doctor = userService.registerUser(docUser);
        assertNotNull(doctor);

        // Approve doctor
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor.setAdminApproved(true);
        userRepository.save(doctor);

        User approvedDoc = userRepository.findById(doctor.getId()).orElseThrow();
        assertEquals(ApprovalStatus.APPROVED, approvedDoc.getApprovalStatus());
        assertTrue(approvedDoc.isAdminApproved());
    }
}
