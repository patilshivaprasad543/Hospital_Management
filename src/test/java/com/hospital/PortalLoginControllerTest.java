package com.hospital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
public class PortalLoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMainPortalLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/portal"));
    }

    @Test
    public void testAdminLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testPatientLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login/patient"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testDoctorLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login/doctor"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testVendorLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login/vendor"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testPharmacyLoginRendersOk() throws Exception {
        mockMvc.perform(get("/login/pharmacy"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testHealthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }
}
