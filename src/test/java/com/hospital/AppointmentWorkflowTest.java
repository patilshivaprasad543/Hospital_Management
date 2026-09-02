package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AppointmentWorkflowTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    private User doctor;
    private User patient;

    @BeforeEach
    void setUp() {
        doctor = new User("Dr. Appt Specialist", "apptdoc@hospital.com", "7776665550", "Pass@123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor = userRepository.save(doctor);

        patient = new User("Appt Patient", "apptpatient@hospital.com", "7776665551", "Pass@123", Role.PATIENT);
        patient = userRepository.save(patient);
    }

    @Test
    void testEndToEndAppointmentLifecycle() {
        // Create appointment
        Appointment appt = new Appointment(patient, doctor, LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Cardiology Consultation");
        appt.setDepartmentCategory("Cardiology");
        appt.setConsultationType(ConsultationType.IN_PERSON);
        appt.setStatus(AppointmentStatus.PENDING);
        appt = appointmentRepository.save(appt);

        assertNotNull(appt);
        assertEquals(AppointmentStatus.PENDING, appt.getStatus());

        // Doctor confirms appointment
        Appointment confirmed = appointmentService.updateAppointmentStatus(appt.getId(), AppointmentStatus.CONFIRMED, "Confirmed by Doctor");
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());

        // Verify patient appointments list
        List<Appointment> patientAppts = appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
        assertFalse(patientAppts.isEmpty());
        assertEquals(1, patientAppts.size());
    }
}
