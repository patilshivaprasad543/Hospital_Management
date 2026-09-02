package com.hospital;

import com.hospital.model.Role;
import com.hospital.model.User;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ConsultationSuiteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User patient;
    private User admin;
    private MockHttpSession patientSession;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Test Suite Patient", "suitepatient_" + ts + "@hospital.com", "99" + (ts % 10000000), "password123", Role.PATIENT));
        admin = userRepository.save(new User("Test Suite Admin", "suiteadmin_" + ts + "@hospital.com", "98" + (ts % 10000000), "password123", Role.ADMIN));

        patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInUser", admin);
    }

    @Test
    void testPatientConsultationSuiteEndpoint() throws Exception {
        mockMvc.perform(get("/patient/consultation-suite").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/consultation-suite"))
                .andExpect(model().attributeExists("patient", "profile", "appointments", "prescriptions", "labRequests", "availableAmbulances", "doctors"));
    }

    @Test
    void testBedBookingRequest() throws Exception {
        mockMvc.perform(post("/patient/consultation-suite/bed-request")
                        .param("wardType", "ICU Bed")
                        .param("reason", "Post-op Monitoring")
                        .session(patientSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/consultation-suite?bedRequested=true"));
    }

    @Test
    void testAmbulanceAndInsuranceEndpoints() throws Exception {
        mockMvc.perform(get("/patient/ambulance").session(patientSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/patient/insurance").session(patientSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/ambulances").session(adminSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/insurance").session(adminSession))
                .andExpect(status().isOk());
    }
}
