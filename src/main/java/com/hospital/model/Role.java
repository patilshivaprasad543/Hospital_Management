package com.hospital.model;

import java.util.EnumSet;
import java.util.Set;

public enum Role {
    ADMIN("Administrator", EnumSet.of(
            Permission.USER_VIEW, Permission.USER_EDIT, Permission.USER_DELETE,
            Permission.DOCTOR_APPROVE, Permission.VENDOR_APPROVE, Permission.PATIENT_VIEW,
            Permission.APPOINTMENT_MANAGE, Permission.PHARMACY_MANAGE, Permission.INVENTORY_MANAGE,
            Permission.BILLING_MANAGE, Permission.REPORT_VIEW, Permission.AUDIT_VIEW
    )),
    PATIENT("Patient", EnumSet.of(
            Permission.PROFILE_VIEW, Permission.PROFILE_EDIT, Permission.DOCTOR_SEARCH,
            Permission.APPOINTMENT_CREATE, Permission.APPOINTMENT_VIEW, Permission.PRESCRIPTION_VIEW,
            Permission.LAB_REPORT_VIEW, Permission.BILL_VIEW, Permission.PAYMENT_CREATE,
            Permission.DOCUMENT_DOWNLOAD
    )),
    DOCTOR("Doctor", EnumSet.of(
            Permission.PATIENT_VIEW, Permission.APPOINTMENT_VIEW, Permission.APPOINTMENT_MANAGE,
            Permission.CONSULTATION_CREATE, Permission.PRESCRIPTION_CREATE, Permission.LAB_REQUEST_CREATE,
            Permission.LAB_REPORT_VIEW
    )),
    VENDOR("Vendor", EnumSet.of(
            Permission.PROFILE_MANAGE, Permission.PRODUCT_MANAGE, Permission.PURCHASE_ORDER_VIEW,
            Permission.PURCHASE_ORDER_UPDATE, Permission.INVOICE_VIEW
    )),
    PHARMACY("Pharmacy", EnumSet.of(
            Permission.MEDICINE_VIEW, Permission.INVENTORY_MANAGE, Permission.PRESCRIPTION_PROCESS,
            Permission.MEDICINE_DISPENSE, Permission.PHARMACY_BILL_CREATE
    ));

    private final String label;
    private final Set<Permission> permissions;

    Role(String label, Set<Permission> permissions) {
        this.label = label;
        this.permissions = permissions;
    }

    public String getLabel() {
        return label;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }
}
