package com.hospital.model;

public enum PortalRole {
    ADMIN("Admin", "👨‍💼", "System administration & approvals"),
    PATIENT("Patient", "👤", "Appointments, prescriptions & records"),
    DOCTOR("Doctor", "👨‍⚕️", "Consultations & patient care"),
    VENDOR("Vendor", "🧪", "Laboratory & diagnostic services"),
    PHARMACY("Pharmacy", "💊", "Medicine inventory & dispensing");

    private final String label;
    private final String icon;
    private final String description;

    PortalRole(String label, String icon, String description) {
        this.label = label;
        this.icon = icon;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public static PortalRole fromPath(String path) {
        if (path == null) {
            return null;
        }
        try {
            return PortalRole.valueOf(path.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean matchesUser(User user) {
        if (user == null) {
            return false;
        }
        return switch (this) {
            case ADMIN -> user.getRole() == Role.ADMIN;
            case PATIENT -> user.getRole() == Role.PATIENT;
            case DOCTOR -> user.getRole() == Role.DOCTOR;
            case VENDOR -> user.getRole() == Role.VENDOR && user.getVendorType() == VendorType.LABORATORY;
            case PHARMACY -> user.getRole() == Role.VENDOR && user.getVendorType() == VendorType.PHARMACY;
        };
    }

    public boolean canSelfRegister() {
        return this != ADMIN;
    }

    public String getPathSegment() {
        return name().toLowerCase();
    }

    public void applyToUser(User user) {
        switch (this) {
            case PATIENT -> {
                user.setRole(Role.PATIENT);
                user.setVendorType(VendorType.NONE);
            }
            case DOCTOR -> {
                user.setRole(Role.DOCTOR);
                user.setVendorType(VendorType.NONE);
            }
            case VENDOR -> {
                user.setRole(Role.VENDOR);
                user.setVendorType(VendorType.LABORATORY);
            }
            case PHARMACY -> {
                user.setRole(Role.VENDOR);
                user.setVendorType(VendorType.PHARMACY);
            }
            default -> throw new IllegalStateException("Admin cannot self-register");
        }
    }

    public static String loginPathForUser(User user) {
        if (user == null) {
            return "/login";
        }
        if (user.getRole() == Role.ADMIN) {
            return "/login/admin";
        }
        if (user.getRole() == Role.PATIENT) {
            return "/login/patient";
        }
        if (user.getRole() == Role.DOCTOR) {
            return "/login/doctor";
        }
        if (user.getRole() == Role.VENDOR) {
            if (user.getVendorType() == VendorType.PHARMACY) {
                return "/login/pharmacy";
            }
            return "/login/vendor";
        }
        return "/login";
    }
}
