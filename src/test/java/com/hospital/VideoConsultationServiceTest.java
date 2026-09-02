package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.VideoConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class VideoConsultationServiceTest {

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private User doctor;
    private User patient;
    private User intruder;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        doctor = new User("Dr. Video Doc", "videodoc@hospital.com", "8887776660", "password123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor = userRepository.save(doctor);

        patient = new User("Video Patient", "videopatient@hospital.com", "8887776661", "password123", Role.PATIENT);
        patient = userRepository.save(patient);

        intruder = new User("Intruder User", "intruder@hospital.com", "8887776662", "password123", Role.PATIENT);
        intruder = userRepository.save(intruder);

        appointment = new Appointment(patient, doctor, LocalDate.now(), LocalTime.now().plusMinutes(5), "Telemedicine Checkup");
        appointment.setConsultationType(ConsultationType.VIDEO);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setVideoJoinAvailableFrom(LocalDateTime.now().minusMinutes(5));
        appointment.setVideoJoinExpiresAt(LocalDateTime.now().plusMinutes(45));
        appointment = appointmentRepository.save(appointment);
    }

    @Test
    void testCreateAndAccessVideoRoom() {
        VideoConsultation room = videoConsultationService.createVideoRoom(appointment);

        assertNotNull(room);
        assertNotNull(room.getRoomId());
        assertTrue(room.getRoomId().startsWith("VR-"));

        // Authorized patient & doctor can access
        assertDoesNotThrow(() -> videoConsultationService.validateAccess(patient, appointment));
        assertDoesNotThrow(() -> videoConsultationService.validateAccess(doctor, appointment));

        // Unauthorized intruder is blocked with exception
        Exception ex = assertThrows(RuntimeException.class, () -> videoConsultationService.validateAccess(intruder, appointment));
        assertTrue(ex.getMessage().contains("Access denied"));
    }

    @Test
    void testStartAndEndConsultation() {
        videoConsultationService.createVideoRoom(appointment);

        VideoConsultation started = videoConsultationService.startConsultation(appointment.getId(), doctor);
        assertEquals(VideoRoomStatus.ACTIVE, started.getStatus());
        assertEquals(AppointmentStatus.IN_PROGRESS, started.getAppointment().getStatus());

        VideoConsultation ended = videoConsultationService.endConsultation(appointment.getId(), doctor);
        assertEquals(VideoRoomStatus.COMPLETED, ended.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, ended.getAppointment().getStatus());
    }
}
