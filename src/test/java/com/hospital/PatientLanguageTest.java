package com.hospital;

import com.hospital.model.PatientProfile;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.PatientProfileRepository;
import com.hospital.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PatientLanguageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    private User patient;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = new User("Lang Patient " + ts, "langpat_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT);
        patient = userRepository.save(patient);

        PatientProfile profile = new PatientProfile(patient);
        profile.setPreferredLanguage("en");
        patientProfileRepository.save(profile);

        session = new MockHttpSession();
        session.setAttribute("loggedInUser", patient);
    }

    @Test
    void testGetLanguageSettingsPage() throws Exception {
        mockMvc.perform(get("/patient/language").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/language"))
                .andExpect(model().attributeExists("currentLang"))
                .andExpect(model().attribute("currentLang", "en"));
    }

    @Test
    void testPostLanguagePreference() throws Exception {
        mockMvc.perform(post("/patient/language")
                        .param("lang", "hi")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/language"))
                .andExpect(flash().attributeExists("successMessage"));

        PatientProfile updated = patientProfileRepository.findByUser(patient).orElseThrow();
        assertEquals("hi", updated.getPreferredLanguage());
    }

    @Test
    void testUpdateLanguageApi() throws Exception {
        mockMvc.perform(post("/patient/api/language")
                        .param("lang", "es")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.language").value("es"));

        PatientProfile updated = patientProfileRepository.findByUser(patient).orElseThrow();
        assertEquals("es", updated.getPreferredLanguage());
    }

    @Test
    void testKannadaLanguagePreference() throws Exception {
        mockMvc.perform(post("/patient/api/language")
                        .param("lang", "kn")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.language").value("kn"));

        PatientProfile updated = patientProfileRepository.findByUser(patient).orElseThrow();
        assertEquals("kn", updated.getPreferredLanguage());
    }
}
