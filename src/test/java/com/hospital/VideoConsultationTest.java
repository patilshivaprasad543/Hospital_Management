package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.VideoConsultationService;
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
public class VideoConsultationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private VideoConsultationService videoConsultationService;

    private User doctor;
    private User patient;
    private Appointment appointment;
    private MockHttpSession patientSession;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        doctor = userRepository.save(new User("Dr. Video Doctor", "vdoc_" + ts + "@hospital.com", "98" + (ts % 10000000), "password123", Role.DOCTOR));
        patient = userRepository.save(new User("Video Patient", "vpat_" + ts + "@hospital.com", "99" + (ts % 10000000), "password123", Role.PATIENT));

        appointment = new Appointment(patient, doctor, LocalDate.now(), LocalTime.now(), "Video Consultation Test");
        appointment.setConsultationType(ConsultationType.VIDEO);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment = appointmentRepository.save(appointment);

        videoConsultationService.createVideoRoom(appointment);

        patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);
    }

    @Test
    void testPatientVideoConsultationRoutes() throws Exception {
        mockMvc.perform(get("/patient/video-consultations").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/video-list"))
                .andExpect(model().attributeExists("videoConsultations", "loggedInUser"));

        mockMvc.perform(get("/patient/video-consultation").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/video-list"));

        mockMvc.perform(get("/patient/video-consultation/" + appointment.getId() + "/waiting-room").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("video/waiting-room"))
                .andExpect(model().attributeExists("appointment", "videoRoom"));

        mockMvc.perform(get("/patient/video-consultation/" + appointment.getId()).session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("video/patient-room"))
                .andExpect(model().attributeExists("appointment", "videoRoom"));
    }
}
