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

import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MultiHubChatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private User patient;
    private User doctor;
    private User admin;
    private User vendor;
    private MockHttpSession patientSession;
    private MockHttpSession doctorSession;
    private MockHttpSession adminSession;
    private MockHttpSession vendorSession;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Chat Patient", "cpat_" + ts + "@hospital.com", "99" + (ts % 10000000), "password123", Role.PATIENT));
        doctor = userRepository.save(new User("Chat Doctor", "cdoc_" + ts + "@hospital.com", "98" + (ts % 10000000), "password123", Role.DOCTOR));
        admin = userRepository.save(new User("Chat Admin", "cadm_" + ts + "@hospital.com", "97" + (ts % 10000000), "password123", Role.ADMIN));
        vendor = userRepository.save(new User("Chat Vendor", "cven_" + ts + "@hospital.com", "96" + (ts % 10000000), "password123", Role.VENDOR));

        patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);

        doctorSession = new MockHttpSession();
        doctorSession.setAttribute("loggedInUser", doctor);

        adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInUser", admin);

        vendorSession = new MockHttpSession();
        vendorSession.setAttribute("loggedInUser", vendor);
    }

    @Test
    void testPatientAndDoctorMultiHubChatRoutes() {
        try {
            mockMvc.perform(get("/patient/multihub-chat").session(patientSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("patient/multihub-chat"))
                    .andExpect(model().attributeExists("contacts", "contactRoleLabel", "loggedInUser"));

            mockMvc.perform(get("/doctor/multihub-chat").session(doctorSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("patient/multihub-chat"))
                    .andExpect(model().attributeExists("contacts", "contactRoleLabel", "loggedInUser"));

            mockMvc.perform(get("/admin/multihub-chat").session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("patient/multihub-chat"))
                    .andExpect(model().attributeExists("contacts", "loggedInUser"));

            mockMvc.perform(get("/vendor/multihub-chat").session(vendorSession))
                    .andExpect(status().isOk())
                    .andExpect(view().name("patient/multihub-chat"))
                    .andExpect(model().attributeExists("contacts", "loggedInUser"));
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
}
