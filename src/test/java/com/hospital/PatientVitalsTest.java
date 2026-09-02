package com.hospital;

import com.hospital.model.PatientVitals;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.PatientVitalsRepository;
import com.hospital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PatientVitalsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientVitalsRepository patientVitalsRepository;

    private User patient;
    private MockHttpSession patientSession;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Vitals Test Patient", "vitalspatient_" + ts + "@hospital.com", "99" + (ts % 10000000), "password123", Role.PATIENT));

        patientSession = new MockHttpSession();
        patientSession.setAttribute("loggedInUser", patient);
    }

    @Test
    void testRecordAndRetrieveVitals() {
        PatientVitals vitals = new PatientVitals(patient, 120, 80, 72, 95.0, 98.6, 98, 68.0, 175.0);
        vitals.setNotes("Baseline test vitals");
        patientVitalsRepository.save(vitals);

        List<PatientVitals> list = patientVitalsRepository.findByPatientOrderByRecordedAtDesc(patient);
        assertFalse(list.isEmpty());
        assertEquals(120, list.get(0).getSystolicBp());
        assertEquals(80, list.get(0).getDiastolicBp());
        assertEquals(22.2, list.get(0).getBmi());
    }

    @Test
    void testVitalsControllerRoute() throws Exception {
        mockMvc.perform(get("/patient/vitals").session(patientSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/vitals"))
                .andExpect(model().attributeExists("vitalsHistory", "latestVitals"));

        mockMvc.perform(post("/patient/vitals/add")
                        .param("systolicBp", "125")
                        .param("diastolicBp", "82")
                        .param("heartRate", "75")
                        .param("bloodGlucose", "98.0")
                        .param("spo2", "99")
                        .session(patientSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/vitals?success=true"));
    }
}
