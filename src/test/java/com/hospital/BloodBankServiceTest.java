package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.BloodUnitRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.BloodBankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BloodBankServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BloodBankService bloodBankService;

    @Autowired
    private BloodUnitRepository bloodUnitRepository;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        admin = userRepository.save(new User("Blood Bank Admin", "bbadmin_" + ts + "@hospital.com", "98" + (ts % 10000000), "password123", Role.ADMIN));
        session = new MockHttpSession();
        session.setAttribute("loggedInUser", admin);
    }

    @Test
    void testRegisterAndIssueBloodUnit() {
        long ts = System.currentTimeMillis();
        String unitCode = "BLD-O-NEG-" + ts;
        BloodUnit registered = bloodBankService.registerBloodUnit(unitCode, BloodGroup.O_NEGATIVE, BloodComponentType.PACKED_RED_CELLS,
                "Universal Donor", "9876543210", 450, LocalDate.now().plusDays(42), admin);

        assertNotNull(registered);
        assertNotNull(registered.getId());
        assertEquals(BloodUnitStatus.AVAILABLE, registered.getStatus());

        Map<BloodGroup, Long> counts = bloodBankService.getAvailableCountsGrouped();
        assertTrue(counts.get(BloodGroup.O_NEGATIVE) >= 1);

        BloodUnit issued = bloodBankService.issueBloodUnit(registered.getId(), null, admin);
        assertEquals(BloodUnitStatus.ISSUED, issued.getStatus());
    }

    @Test
    void testBloodBankViewControllerEndpoints() throws Exception {
        mockMvc.perform(get("/blood-bank/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("blood-bank/dashboard"))
                .andExpect(model().attributeExists("units", "groupCounts", "availableUnits"));

        mockMvc.perform(post("/blood-bank/add")
                        .param("unitCode", "BLD-TEST-99")
                        .param("bloodGroup", "A_POSITIVE")
                        .param("componentType", "WHOLE_BLOOD")
                        .param("donorName", "Donor Test")
                        .param("donorContact", "9876543210")
                        .session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blood-bank/dashboard?added=true"));
    }
}
