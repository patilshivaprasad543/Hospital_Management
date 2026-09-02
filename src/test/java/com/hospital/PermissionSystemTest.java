package com.hospital;

import com.hospital.model.Permission;
import com.hospital.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionSystemTest {

    @Test
    void testFivePrimaryRolesAndPermissions() {
        // Exactly 5 primary roles
        assertEquals(5, Role.values().length);

        // ADMIN permissions
        assertTrue(Role.ADMIN.hasPermission(Permission.USER_VIEW));
        assertTrue(Role.ADMIN.hasPermission(Permission.DOCTOR_APPROVE));
        assertTrue(Role.ADMIN.hasPermission(Permission.AUDIT_VIEW));

        // DOCTOR permissions
        assertTrue(Role.DOCTOR.hasPermission(Permission.CONSULTATION_CREATE));
        assertTrue(Role.DOCTOR.hasPermission(Permission.PRESCRIPTION_CREATE));
        assertFalse(Role.DOCTOR.hasPermission(Permission.USER_DELETE));

        // PATIENT permissions
        assertTrue(Role.PATIENT.hasPermission(Permission.APPOINTMENT_CREATE));
        assertTrue(Role.PATIENT.hasPermission(Permission.DOCUMENT_DOWNLOAD));

        // VENDOR permissions
        assertTrue(Role.VENDOR.hasPermission(Permission.PRODUCT_MANAGE));
        assertTrue(Role.VENDOR.hasPermission(Permission.INVOICE_VIEW));

        // PHARMACY permissions
        assertTrue(Role.PHARMACY.hasPermission(Permission.MEDICINE_DISPENSE));
        assertTrue(Role.PHARMACY.hasPermission(Permission.PRESCRIPTION_PROCESS));
    }
}
