package com.hospital.service;

import com.hospital.dto.PatientIntake;
import com.hospital.model.PatientProfile;
import com.hospital.model.PharmacyItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorProfile;
import com.hospital.model.VendorType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@Transactional
class HospitalRecordAccessTest {

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @Test
    void storesAndReadsPatientDetails() {
        User incoming = new User("Stored Patient", "stored.access@example.com", "9111000001", "Secret123", Role.PATIENT);
        User saved = userService.registerUser(incoming, new PatientIntake(
                LocalDate.of(1990, 1, 2), "Male", "10 Record Street",
                "O+", "Pat Contact", "9111000099", "None", "Hypertension"));

        User loaded = userService.findById(saved.getId()).orElseThrow();
        PatientProfile profile = userService.getPatientProfile(loaded).orElseThrow();

        assertEquals("stored.access@example.com", loaded.getEmail());
        assertEquals("10 Record Street", profile.getAddress());
        assertEquals("O+", profile.getBloodGroup());
        assertEquals("Pat Contact", profile.getEmergencyContactName());
        assertEquals("Hypertension", profile.getMedicalHistory());
    }

    @Test
    void storesAndReadsPharmacyInventoryForVendor() {
        User pharmacy = userService.findByEmail("pharmacy@smartcare360.com").orElseThrow();
        VendorProfile profile = userService.getVendorProfile(pharmacy).orElseThrow();
        List<PharmacyItem> items = vendorService.getPharmacyItemsByVendor(pharmacy);

        assertEquals(VendorType.PHARMACY, pharmacy.getVendorType());
        assertEquals("MediPlus Central Pharmacy", profile.getBusinessName());
        assertFalse(items.isEmpty());
        assertTrue(items.stream().anyMatch(item -> "Paracetamol 650mg".equals(item.getItemName())));
    }
}
