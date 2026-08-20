package com.hospital.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalRoleTest {

    @Test
    void pharmacyPortalMatchesVendorWithPharmacyType() {
        User pharmacy = new User("MediPlus", "shop@example.com", "9000000000", "x", Role.VENDOR);
        pharmacy.setVendorType(VendorType.PHARMACY);

        assertTrue(PortalRole.PHARMACY.matchesUser(pharmacy));
        assertFalse(PortalRole.VENDOR.matchesUser(pharmacy));
        assertFalse(PortalRole.PHARMACY.matchesUser(new User("Pat", "p@example.com", "9000000001", "x", Role.PATIENT)));
    }
}
