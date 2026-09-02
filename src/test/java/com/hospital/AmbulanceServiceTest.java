package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.AmbulanceRepository;
import com.hospital.repository.UserRepository;
import com.hospital.service.AmbulanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AmbulanceServiceTest {

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private UserRepository userRepository;

    private User patient;
    private User admin;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Test Patient " + ts, "amb_" + ts + "@hospital.com", "99" + (ts % 100000000), "password123", Role.PATIENT));
        admin = userRepository.save(new User("Test Admin " + ts, "adm_" + ts + "@hospital.com", "98" + (ts % 100000000), "password123", Role.ADMIN));

        Ambulance amb = new Ambulance("AMB-" + ts, AmbulanceType.BASIC, "Driver One", "9876543210", 1500.0);
        ambulanceRepository.save(amb);
    }

    @Test
    void testAdminAddNewAmbulance() {
        long ts = System.currentTimeMillis();
        Ambulance newAmb = new Ambulance("AMB-ALS-" + ts, AmbulanceType.ADVANCED_LIFE_SUPPORT, "Driver Advanced", "9876543211", 3500.0);
        newAmb.setEquipmentList("Defibrillator, Ventilator");

        Ambulance saved = ambulanceService.saveAmbulance(newAmb);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("AMB-ALS-" + ts, saved.getVehicleNumber());
        assertEquals(AmbulanceType.ADVANCED_LIFE_SUPPORT, saved.getType());
        assertEquals(AmbulanceStatus.AVAILABLE, saved.getStatus());
    }

    @Test
    void testRequestAndAssignAmbulance() {
        try {
            AmbulanceTrip trip = ambulanceService.requestAmbulance(patient, "123 Main St", "SmartCare Emergency",
                    EmergencyPriority.CRITICAL, AmbulanceType.BASIC, "John Contact", "9876543210", "Severe trauma");

            assertNotNull(trip);
            assertEquals(AmbulanceTripStatus.REQUESTED, trip.getStatus());

            List<Ambulance> available = ambulanceService.getAvailableAmbulances();
            assertFalse(available.isEmpty());

            Ambulance assignedAmb = available.get(0);
            AmbulanceTrip assignedTrip = ambulanceService.assignAmbulance(trip.getId(), assignedAmb.getId(), admin);

            assertNotNull(assignedTrip);
            assertEquals(AmbulanceTripStatus.ASSIGNED, assignedTrip.getStatus());
            assertEquals(AmbulanceStatus.ASSIGNED, assignedAmb.getStatus());
        } catch (Throwable e) {
            String msg = e.getClass().getName() + ": " + e.getMessage();
            if (e.getCause() != null) {
                msg += " | Cause: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
            }
            fail("Exception thrown: " + msg);
        }
    }
}
