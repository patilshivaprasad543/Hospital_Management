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
}
