package com.hospital;

import com.hospital.controller.VideoSignalingController;
import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.AppointmentService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class RealTimeTelemedicineAndChatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private VideoSignalingController videoSignalingController;

    private User patient;
    private User doctor;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("RealTime Patient " + ts, "rtp_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT));

        doctor = new User("Dr. RealTime Doc " + ts, "rtdoc_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor = userRepository.save(doctor);
        doctorProfileRepository.save(new DoctorProfile(doctor));
    }

    @Test
    void testRealTimeDoctorAcceptanceAndWaitingRoomGuard() throws Exception {
        // 1. Patient books video consultation
        Appointment appt = appointmentService.bookAppointmentWithDepartment(
                patient.getId(), doctor.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                "Realtime Cardiology Checkup", "Cardiology", ConsultationType.VIDEO
        );

        assertNotNull(appt);
        assertEquals(AppointmentStatus.PENDING, appt.getStatus());

        // 2. Patient attempts to access waiting room while PENDING -> Redirected back with message
        MockHttpSession patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);

        mockMvc.perform(get("/patient/video-consultation/" + appt.getId() + "/waiting-room").session(patientSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/video-consultations"))
                .andExpect(flash().attributeExists("errorMessage"));

        // 3. Doctor accepts the video consultation
        MockHttpSession doctorSession = new MockHttpSession();
        doctorSession.setAttribute("loggedInUser", doctor);

        mockMvc.perform(post("/doctor/video-consultation/" + appt.getId() + "/accept").session(doctorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/video-consultations"))
                .andExpect(flash().attributeExists("successMessage"));

        // Verify status is now CONFIRMED
        Appointment updatedAppt = appointmentService.findById(appt.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, updatedAppt.getStatus());

        // 4. Patient now successfully enters waiting room
        mockMvc.perform(get("/patient/video-consultation/" + appt.getId() + "/waiting-room").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("video/waiting-room"))
                .andExpect(model().attributeExists("videoRoom"));
    }

    @Test
    void testRealTimeInChatVideoRoomGeneration() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", patient);

        // Fetch instant video room for chat
        mockMvc.perform(get("/api/chat/video-room")
                        .param("targetUserId", String.valueOf(doctor.getId()))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomUrl").exists())
                .andExpect(jsonPath("$.roomId").exists())
                .andExpect(jsonPath("$.doctorName").value(doctor.getFullName()));
    }

    @Test
    void testRealTimeSTOMPSignalingBroadcast() {
        VideoSignalingPayload chatPayload = new VideoSignalingPayload("CHAT", "📹 VIDEO_CALL_INVITE:/patient/video-consultation/100", String.valueOf(patient.getId()), patient.getFullName(), "PATIENT");
        assertDoesNotThrow(() -> videoSignalingController.handleChat("VR-TEST-ROOM", chatPayload));

        VideoSignalingPayload offerPayload = new VideoSignalingPayload("OFFER", "{ sdp: 'offer_sdp' }", String.valueOf(patient.getId()), patient.getFullName(), "PATIENT");
        assertDoesNotThrow(() -> videoSignalingController.handleSignal("VR-TEST-ROOM", offerPayload));
    }
}
