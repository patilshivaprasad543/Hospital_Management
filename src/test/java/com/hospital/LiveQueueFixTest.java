package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.QueueEntryRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.QueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LiveQueueFixTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private QueueService queueService;

    private User doctor;
    private User patient;
    private User admin;
    private Appointment appointment;
    private MockHttpSession doctorSession;
    private MockHttpSession patientSession;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        doctor = userRepository.save(new User("Dr. Queue Tester", "qdoc_" + ts + "@hospital.com", "98" + (ts % 10000000), "password123", Role.DOCTOR));
        patient = userRepository.save(new User("Queue Patient", "qpat_" + ts + "@hospital.com", "99" + (ts % 10000000), "password123", Role.PATIENT));
        admin = userRepository.save(new User("Queue Admin", "qadm_" + ts + "@hospital.com", "97" + (ts % 10000000), "password123", Role.ADMIN));

        appointment = new Appointment(patient, doctor, LocalDate.now(), LocalTime.now(), "Live Queue Test");
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepository.save(appointment);

        doctorSession = new MockHttpSession();
        doctorSession.setAttribute("loggedInUser", doctor);

        patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInUser", admin);
    }

    @Test
    void testDoctorQueueRoutes() throws Exception {
        mockMvc.perform(get("/doctor/queue").session(doctorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/live-queue"))
                .andExpect(model().attributeExists("queueList", "loggedInUser"));

        mockMvc.perform(get("/doctor/live-queue").session(doctorSession))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/live-queue"));
    }

    @Test
    void testPatientQueueRoute() throws Exception {
        mockMvc.perform(get("/patient/queue").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/live-queue"))
                .andExpect(model().attributeExists("appointment", "queueInfo", "loggedInUser"));
    }

    @Test
    void testAdminAndPublicQueueDisplayRoutes() {
        try {
            mockMvc.perform(get("/admin/queue").session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("queue/public-display"));

            mockMvc.perform(get("/queue/display/" + doctor.getId()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("queue/public-display"));
        } catch (Throwable e) {
            String msg = e.getClass().getName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                msg += " | Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
                if (e.getCause().getCause() != null) {
                    msg += " | RootCause: " + e.getCause().getCause().getClass().getName() + ": " + e.getCause().getCause().getMessage();
                }
            }
            fail("Exception thrown: " + msg);
        }
    }

    @Test
    void testQueueLifecycleActions() {
        QueueEntry entry = new QueueEntry(appointment, doctor, patient, "A-001", 1, LocalDate.now());
        entry = queueEntryRepository.save(entry);

        QueueEntry called = queueService.recallPatient(entry.getId(), doctor);
        assertEquals(QueueStatus.CALLED, called.getStatus());

        QueueEntry started = queueService.startConsultation(entry.getId(), doctor);
        assertEquals(QueueStatus.IN_PROGRESS, started.getStatus());

        QueueEntry completed = queueService.completeConsultation(entry.getId(), doctor);
        assertEquals(QueueStatus.COMPLETED, completed.getStatus());
    }
}
