package com.hospital.service;

import com.hospital.model.Notification;
import com.hospital.model.NotificationCategory;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private NotificationChannelService notificationChannelService;

    @InjectMocks
    private NotificationService notificationService;

    private User patient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "emailEnabled", true);
        patient = new User("Jane Patient", "patient@example.com", "9999999999", "secret", Role.PATIENT);
        patient.setId(1L);
        when(notificationChannelService.buildPortalLink("/patient/prescriptions"))
                .thenReturn("http://localhost:8080/patient/prescriptions");
    }

    @Test
    void sendPortalNotification_savesAndEmailsRecipient() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification saved = notificationService.sendPortalNotification(
                patient,
                "Digital Prescription Issued",
                "Your doctor issued a prescription.",
                NotificationCategory.PRESCRIPTION,
                "/patient/prescriptions"
        );

        assertNotNull(saved);
        assertEquals("Digital Prescription Issued", saved.getTitle());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendNotificationEmail(
                eq("patient@example.com"),
                eq("Jane Patient"),
                eq("Digital Prescription Issued"),
                bodyCaptor.capture()
        );
        String body = bodyCaptor.getValue();
        assertTrue(body.contains("Your doctor issued a prescription."));
        assertTrue(body.contains("http://localhost:8080/patient/prescriptions"));
    }
}
