package com.hospital.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PortalMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicHomeAndLoginPagesLoad() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(containsString("SmartCare")));
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/login/patient")).andExpect(status().isOk());
        mockMvc.perform(get("/about")).andExpect(status().isOk());
    }

    @Test
    void patientTimelineRequiresLogin() throws Exception {
        mockMvc.perform(get("/patient/timeline"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/patient"));
    }

    @Test
    void patientTimelineLoadsAfterDemoLogin() throws Exception {
        MvcResult login = mockMvc.perform(post("/login")
                        .param("email", "patient@smartcare360.com")
                        .param("password", "patient123")
                        .param("portalRole", "PATIENT"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(get("/patient/timeline").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Timeline")));

        mockMvc.perform(get("/patient/notifications").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Notification")));

        mockMvc.perform(get("/notifications").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/notifications"));
    }
}
