package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.QueueEntryRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QueueServiceTest {

    @Autowired
    private QueueService queueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private User doctor;
    private User patient;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        doctor = new User("Dr. Test Doctor", "testdoc@hospital.com", "9998887770", "password123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor = userRepository.save(doctor);

        patient = new User("Test Patient", "testpatient@hospital.com", "9998887771", "password123", Role.PATIENT);
        patient = userRepository.save(patient);

        appointment = new Appointment(patient, doctor, LocalDate.now(), LocalTime.now().plusMinutes(5), "Regular Checkup");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepository.save(appointment);
    }

    @Test
    void testCheckInPatient() {
        QueueEntry entry = queueService.checkInPatient(appointment.getId(), patient);

        assertNotNull(entry);
        assertEquals("A-001", entry.getQueueNumber());
        assertEquals(QueueStatus.WAITING, entry.getStatus());
        assertEquals(1, entry.getSequenceNumber());

        Map<String, Object> pos = queueService.getPatientPositionInfo(appointment);
        assertTrue((Boolean) pos.get("checkedIn"));
        assertEquals("A-001", pos.get("queueNumber"));
        assertEquals(0L, pos.get("patientsAhead"));
    }

    @Test
    void testDoctorCallNextPatient() {
        queueService.checkInPatient(appointment.getId(), patient);

        QueueEntry called = queueService.callNextPatient(doctor);
        assertNotNull(called);
        assertEquals(QueueStatus.CALLED, called.getStatus());
        assertEquals(patient.getId(), called.getPatient().getId());
    }

    @Test
    void testSkipAndHoldPatient() {
        QueueEntry entry = queueService.checkInPatient(appointment.getId(), patient);

        QueueEntry held = queueService.holdPatient(entry.getId(), doctor, "Patient stepped away");
        assertEquals(QueueStatus.ON_HOLD, held.getStatus());

        QueueEntry resumed = queueService.resumePatient(entry.getId(), doctor);
        assertEquals(QueueStatus.WAITING, resumed.getStatus());

        QueueEntry skipped = queueService.skipPatient(entry.getId(), doctor, "Not present");
        assertEquals(QueueStatus.SKIPPED, skipped.getStatus());
    }
}
