package com.hospital.service;

import com.hospital.model.ApprovalStatus;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.*;
import com.hospital.service.mail.MailDeliveryDiagnostics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceLoginTest {

    @Mock private UserRepository userRepository;
    @Mock private PatientProfileRepository patientProfileRepository;
    @Mock private DoctorProfileRepository doctorProfileRepository;
    @Mock private VendorProfileRepository vendorProfileRepository;
    @Mock private NotificationChannelService notificationChannelService;
    @Mock private NotificationService notificationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;
    @Mock private OtpService otpService;
    @Mock private MailDeliveryDiagnostics mailDeliveryDiagnostics;

    @InjectMocks
    private UserService userService;

    private User patient;

    @BeforeEach
    void setUp() {
        patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "encoded", Role.PATIENT);
        patient.setId(1L);
        patient.setVerified(true);
        patient.setAdminApproved(true);
        patient.setApprovalStatus(ApprovalStatus.APPROVED);
    }

    @Test
    void loginUser_normalizesEmailBeforeLookup() {
        when(userRepository.findByEmail("patient@smartcare360.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("patient123", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(patient);

        assertTrue(userService.loginUser("Patient@SmartCare360.com", "patient123").isPresent());
        verify(userRepository).findByEmail(eq("patient@smartcare360.com"));
    }

    @Test
    void registerUser_storesHashedPasswordAndActivatesPatient() {
        User incoming = new User("New Patient", "new.user@example.com", "9000000000", "MySecret1", Role.PATIENT);
        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("MySecret1")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });
        when(patientProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User stored = userService.registerUser(incoming);

        assertEquals(42L, stored.getId());
        assertEquals("hashed-secret", stored.getPassword());
        assertTrue(stored.isVerified());
        assertEquals(ApprovalStatus.APPROVED, stored.getApprovalStatus());
        verify(patientProfileRepository).save(any());
        verify(notificationChannelService).sendWelcomeNotice(
                eq("new.user@example.com"), eq("9000000000"), eq("New Patient"), eq("PATIENT"));
    }

    @Test
    void loginUser_activatesUnverifiedPatientWithCorrectPassword() {
        User unverified = new User("Stuck User", "stuck@example.com", "9111111111", "encoded", Role.PATIENT);
        unverified.setVerified(false);
        unverified.setApprovalStatus(ApprovalStatus.PENDING_OTP);
        when(userRepository.findByEmail("stuck@example.com")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("mypassword", "encoded")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User loggedIn = userService.loginUser("stuck@example.com", "mypassword").orElseThrow();
        assertTrue(loggedIn.isVerified());
        assertEquals(ApprovalStatus.APPROVED, loggedIn.getApprovalStatus());
    }
}
