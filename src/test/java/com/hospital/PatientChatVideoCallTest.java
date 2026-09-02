package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.DoctorProfileRepository;
import com.hospital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PatientChatVideoCallTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    private User patient;
    private User doctor;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Chat Video Patient " + ts, "cvp_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT));

        doctor = new User("Dr. Chat Video Specialist " + ts, "cvdoc_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.DOCTOR);
        doctor.setAdminApproved(true);
        doctor.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor = userRepository.save(doctor);
        doctorProfileRepository.save(new DoctorProfile(doctor));
    }

    @Test
    void testGetOrCreateVideoRoomForChat() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInUser", patient);

        mockMvc.perform(get("/api/chat/video-room")
                        .param("targetUserId", String.valueOf(doctor.getId()))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomUrl").exists())
                .andExpect(jsonPath("$.roomId").exists())
                .andExpect(jsonPath("$.doctorName").value(doctor.getFullName()));
    }
}
