package com.hospital;

import com.hospital.model.ApprovalStatus;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorType;
import com.hospital.repository.UserRepository;
import com.hospital.service.UserSessionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MultiTabMultiPortalSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patient;
    private User doctor;
    private User admin;
    private User pharmacyVendor;

    private User createUser(String name, String email, String mobile, Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setMobileNumber(mobile);
        user.setPassword(passwordEncoder.encode("pass123"));
        user.setRole(role);
        user.setVerified(true);
        user.setAccountStatus("ACTIVE");
        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        return userRepository.save(user);
    }

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = createUser("Patient MultiTab " + ts, "mt_pat_" + ts + "@hospital.com", "99" + (ts % 100000000), Role.PATIENT);
        doctor = createUser("Doctor MultiTab " + ts, "mt_doc_" + ts + "@hospital.com", "98" + (ts % 100000000), Role.DOCTOR);
        admin = createUser("Admin MultiTab " + ts, "mt_adm_" + ts + "@hospital.com", "97" + (ts % 100000000), Role.ADMIN);

        User v = new User("Pharmacy MultiTab " + ts, "mt_phm_" + ts + "@hospital.com", "96" + (ts % 100000000), passwordEncoder.encode("pass123"), Role.VENDOR);
        v.setVendorType(VendorType.PHARMACY);
        v.setVerified(true);
        v.setAccountStatus("ACTIVE");
        v.setAdminApproved(true);
        v.setApprovalStatus(ApprovalStatus.APPROVED);
        pharmacyVendor = userRepository.save(v);
    }

    @Test
    void testConcurrentRoleLoginsInSingleHttpSession() throws Exception {
        MockHttpSession sharedSession = new MockHttpSession();

        // 1. Log in as Patient in Tab 1
        mockMvc.perform(post("/login")
                        .param("email", patient.getEmail())
                        .param("password", "pass123")
                        .param("portalRole", "patient")
                        .session(sharedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patient/dashboard"));

        // 2. Log in as Doctor in Tab 2
        mockMvc.perform(post("/login")
                        .param("email", doctor.getEmail())
                        .param("password", "pass123")
                        .param("portalRole", "doctor")
                        .session(sharedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctor/dashboard"));

        // 3. Log in as Admin in Tab 3
        mockMvc.perform(post("/login")
                        .param("email", admin.getEmail())
                        .param("password", "pass123")
                        .param("portalRole", "admin")
                        .session(sharedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        // 4. Log in as Pharmacy Vendor in Tab 4
        mockMvc.perform(post("/login")
                        .param("email", pharmacyVendor.getEmail())
                        .param("password", "pass123")
                        .param("portalRole", "pharmacy")
                        .session(sharedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vendor/dashboard"));

        // Assert all role attributes exist independently in the single shared HTTP session
        assertNotNull(UserSessionHelper.getLoggedInPatient(sharedSession));
        assertEquals(patient.getId(), UserSessionHelper.getLoggedInPatient(sharedSession).getId());

        assertNotNull(UserSessionHelper.getLoggedInDoctor(sharedSession));
        assertEquals(doctor.getId(), UserSessionHelper.getLoggedInDoctor(sharedSession).getId());

        assertNotNull(UserSessionHelper.getLoggedInAdmin(sharedSession));
        assertEquals(admin.getId(), UserSessionHelper.getLoggedInAdmin(sharedSession).getId());

        assertNotNull(UserSessionHelper.getLoggedInVendor(sharedSession));
        assertEquals(pharmacyVendor.getId(), UserSessionHelper.getLoggedInVendor(sharedSession).getId());
    }

    @Test
    void testPortalControllersIsolatedSessionResolution() throws Exception {
        MockHttpSession sharedSession = new MockHttpSession();

        // Bind all role users to the session
        UserSessionHelper.setLoggedInUserForRole(sharedSession, patient);
        UserSessionHelper.setLoggedInUserForRole(sharedSession, doctor);
        UserSessionHelper.setLoggedInUserForRole(sharedSession, admin);
        UserSessionHelper.setLoggedInUserForRole(sharedSession, pharmacyVendor);

        // Access Patient Dashboard (Tab 1)
        mockMvc.perform(get("/patient/dashboard").session(sharedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/dashboard"))
                .andExpect(model().attributeExists("patient"));

        // Access Doctor Dashboard (Tab 2)
        mockMvc.perform(get("/doctor/dashboard").session(sharedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/dashboard"))
                .andExpect(model().attributeExists("doctor"));

        // Access Admin Dashboard (Tab 3)
        mockMvc.perform(get("/admin/dashboard").session(sharedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("admin"));

        // Access Pharmacy Vendor Dashboard (Tab 4)
        mockMvc.perform(get("/vendor/dashboard").session(sharedSession))
                .andExpect(status().isOk())
                .andExpect(view().name("vendor/pharmacy-dashboard"))
                .andExpect(model().attributeExists("vendor"));
    }

    @Test
    void testRoleScopedLogoutIsolation() throws Exception {
        MockHttpSession sharedSession = new MockHttpSession();

        UserSessionHelper.setLoggedInUserForRole(sharedSession, patient);
        UserSessionHelper.setLoggedInUserForRole(sharedSession, doctor);
        UserSessionHelper.setLoggedInUserForRole(sharedSession, admin);

        // Logout Patient portal only (from Tab 1)
        mockMvc.perform(get("/logout/patient").session(sharedSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login/patient"));

        // Patient session is cleared
        assertNull(UserSessionHelper.getLoggedInPatient(sharedSession));

        // Doctor and Admin sessions remain active
        assertNotNull(UserSessionHelper.getLoggedInDoctor(sharedSession));
        assertNotNull(UserSessionHelper.getLoggedInAdmin(sharedSession));

        // Verify Doctor tab still works
        mockMvc.perform(get("/doctor/dashboard").session(sharedSession))
                .andExpect(status().isOk());

        // Verify Admin tab still works
        mockMvc.perform(get("/admin/dashboard").session(sharedSession))
                .andExpect(status().isOk());
    }
}
