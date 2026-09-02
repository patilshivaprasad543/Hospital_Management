package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.HealthLibraryService;
import com.hospital.service.SymptomTriageService;
import com.hospital.service.SymptomTriageService.TriageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MayoClinicFeatureAlignmentTest {

    @Autowired
    private SymptomTriageService symptomTriageService;

    @Autowired
    private HealthLibraryService healthLibraryService;

    @Autowired
    private PreCheckInRepository preCheckInRepository;

    @Autowired
    private PatientProxyRepository patientProxyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    private User patient1;
    private User patient2;
    private User doctor;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient1 = userRepository.save(new User("Mayo Patient 1 " + ts, "mpat1_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT));
        patient2 = userRepository.save(new User("Mayo Caregiver 2 " + ts, "mpat2_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.PATIENT));
        doctor = userRepository.save(new User("Dr. Mayo " + ts, "mdoc_" + ts + "@hospital.com", "97" + (ts % 100000000), "pass123", Role.DOCTOR));
    }

    @Test
    void testSymptomTriageEngine() {
        // Test Emergency Symptom (Chest Pain)
        TriageResult erResult = symptomTriageService.evaluateSymptoms(List.of("chest_pain", "shortness_of_breath"), 9, 45, false);
        assertEquals(SymptomTriageService.TriageLevel.EMERGENCY_ER, erResult.getLevel());
        assertFalse(erResult.getRedFlagAlerts().isEmpty());

        // Test Urgent Care Symptom (High Fever)
        TriageResult urgentResult = symptomTriageService.evaluateSymptoms(List.of("high_fever"), 7, 30, false);
        assertEquals(SymptomTriageService.TriageLevel.URGENT_CARE, urgentResult.getLevel());

        // Test Routine Symptom (Skin Rash)
        TriageResult routineResult = symptomTriageService.evaluateSymptoms(List.of("skin_rash"), 4, 25, false);
        assertEquals(SymptomTriageService.TriageLevel.ROUTINE_DOCTOR, routineResult.getLevel());
        assertEquals("Dermatology", routineResult.getRecommendedSpecialty());

        // Test Self-Care
        TriageResult selfCareResult = symptomTriageService.evaluateSymptoms(List.of("sore_throat"), 2, 20, false);
        assertEquals(SymptomTriageService.TriageLevel.SELF_CARE, selfCareResult.getLevel());
    }

    @Test
    void testAppointmentPreCheckInWorkflow() {
        Appointment appt = new Appointment(patient1, doctor, LocalDate.now(), LocalTime.of(10, 0), "Routine Checkup");
        appt = appointmentRepository.save(appt);

        PreCheckIn preCheckIn = new PreCheckIn(appt, patient1, "Penicillin", "Multivitamins", "Routine Health Checkup", "Jane Doe", "9876543210");
        PreCheckIn saved = preCheckInRepository.save(preCheckIn);

        assertNotNull(saved.getId());
        assertEquals("Penicillin", saved.getConfirmedAllergies());
        assertTrue(preCheckInRepository.existsByAppointment(appt));
    }

    @Test
    void testFamilyProxyAccessWorkflow() {
        PatientProxy proxy = new PatientProxy(patient1, patient2, "Spouse", "FULL_ACCESS");
        PatientProxy saved = patientProxyRepository.save(proxy);

        assertNotNull(saved.getId());
        assertTrue(patientProxyRepository.existsByPatientAndProxyUser(patient1, patient2));

        List<PatientProxy> proxiesOfPatient1 = patientProxyRepository.findByPatientAndStatus(patient1, "ACTIVE");
        assertEquals(1, proxiesOfPatient1.size());
        assertEquals(patient2.getId(), proxiesOfPatient1.get(0).getProxyUser().getId());
    }

    @Test
    void testHealthLibraryArticles() {
        List<HealthArticle> articles = healthLibraryService.getAllArticles();
        assertFalse(articles.isEmpty(), "Health library should seed articles");

        List<HealthArticle> searchResults = healthLibraryService.searchArticles("Hypertension");
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.get(0).getTitle().contains("Hypertension"));
    }
}
