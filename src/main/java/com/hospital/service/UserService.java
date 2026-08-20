package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.mail.MailDeliveryDiagnostics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    @Autowired
    private NotificationChannelService notificationChannelService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private OtpService otpService;

    @Autowired
    private MailDeliveryDiagnostics mailDeliveryDiagnostics;

    @Transactional
    public User registerUser(User user) {
        return registerUser(user, null, null, null);
    }

    @Transactional
    public User registerUser(User user, java.time.LocalDate dateOfBirth, String gender, String address) {
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin accounts cannot be self-registered. Contact system administrator.");
        }

        String email = normalizeEmail(user.getEmail());
        if (email.isBlank()) {
            throw new RuntimeException("Email address is required.");
        }
        user.setEmail(email);

        String mobile = normalizeMobile(user.getMobileNumber());
        if (mobile.isBlank()) {
            throw new RuntimeException("Mobile number is required.");
        }
        user.setMobileNumber(mobile);

        if (userRepository.existsByEmail(email) || userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("This email is already registered. Please sign in with your password.");
        }
        if (userRepository.existsByMobileNumber(mobile)) {
            throw new RuntimeException("This mobile number is already registered. Please sign in with your password.");
        }

        String otp = generateOtp();
        user.setVerified(false);
        user.setAccountStatus("PENDING");
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setApprovalStatus(ApprovalStatus.PENDING_OTP);
        user.setAdminApproved(user.getRole() == Role.PATIENT);
        if (user.getRole() == Role.VENDOR && (user.getVendorType() == null || user.getVendorType() == VendorType.NONE)) {
            throw new RuntimeException("Select Pharmacy or Laboratory when creating this account.");
        }

        User savedUser = userRepository.save(user);

        if (user.getRole() == Role.PATIENT) {
            PatientProfile profile = new PatientProfile(savedUser);
            profile.setDateOfBirth(dateOfBirth);
            profile.setGender(gender);
            profile.setAddress(address);
            if (dateOfBirth != null) {
                profile.setAge(java.time.Period.between(dateOfBirth, java.time.LocalDate.now()).getYears());
            }
            patientProfileRepository.save(profile);
        } else if (user.getRole() == Role.DOCTOR) {
            DoctorProfile doctorProfile = new DoctorProfile(savedUser);
            doctorProfile.setSpecialization("General Physician");
            doctorProfile.setQualification("MBBS");
            doctorProfile.setExperienceYears(5);
            doctorProfile.setConsultationFee(500.00);
            doctorProfile.setAvailabilitySchedule("Mon - Fri (09:00 AM - 05:00 PM)");
            doctorProfileRepository.save(doctorProfile);
        } else if (user.getRole() == Role.VENDOR) {
            VendorProfile vendorProfile = new VendorProfile(savedUser);
            vendorProfile.setVendorType(savedUser.getVendorType());
            vendorProfile.setBusinessName(user.getFullName() + " Services");
            vendorProfile.setOwnerName(user.getFullName());
            vendorProfile.setContactPhone(user.getMobileNumber());
            if (address != null && !address.isBlank()) {
                vendorProfile.setAddress(address.trim());
            }
            vendorProfileRepository.save(vendorProfile);
        }

        otpService.store(savedUser.getEmail(), otp, OtpPurpose.REGISTRATION);
        boolean sent = notificationChannelService.sendOtp(savedUser.getEmail(), savedUser.getMobileNumber(), otp);
        auditLogService.log(savedUser, "USER_REGISTERED", "AUTH",
                sent ? "User registered, OTP sent" : "User registered, OTP send failed");
        savedUser.setOtpDelivered(sent);
        if (!sent) {
            String detail = mailDeliveryDiagnostics.getLastFailure();
            savedUser.setDocumentInfo(detail != null ? detail : "OTP email was not delivered");
            savedUser.setPendingOtpHint(otp);
            userRepository.save(savedUser);
        }
        return savedUser;
    }

    public boolean verifyOtp(Long userId, String enteredOtp) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (!otpService.validate(user.getEmail(), enteredOtp, OtpPurpose.REGISTRATION)) {
                return false;
            }
            user.setVerified(true);

            if (user.getRole() == Role.PATIENT) {
                user.setApprovalStatus(ApprovalStatus.APPROVED);
                user.setAdminApproved(true);
                user.setAccountStatus("ACTIVE");
            } else if (user.getRole() == Role.DOCTOR || user.getRole() == Role.VENDOR) {
                user.setApprovalStatus(ApprovalStatus.PENDING_DOCUMENTS);
                user.setAdminApproved(false);
            }

            userRepository.save(user);
            auditLogService.log(user, "OTP_VERIFIED", "AUTH", "Account OTP verified");
            return true;
        }
        return false;
    }

    public void submitDocuments(Long userId, String documentInfo, String licenseFileName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.VENDOR) {
            throw new RuntimeException("Document submission is only required for doctors and vendors.");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Please complete OTP verification first.");
        }

        user.setDocumentInfo(documentInfo);
        user.setRejectionReason(null);
        user.setApprovalStatus(ApprovalStatus.PENDING_ADMIN);
        user.setAccountStatus("PENDING");
        userRepository.save(user);

        if (user.getRole() == Role.DOCTOR && licenseFileName != null) {
            DoctorProfile profile = doctorProfileRepository.findByUser(user).orElseGet(() -> new DoctorProfile(user));
            profile.setLicenseFileName(licenseFileName);
            doctorProfileRepository.save(profile);
        }
        if (user.getRole() == Role.VENDOR && licenseFileName != null) {
            VendorProfile profile = vendorProfileRepository.findByUser(user).orElseGet(() -> new VendorProfile(user));
            profile.setLicenseFileName(licenseFileName);
            vendorProfileRepository.save(profile);
        }

        auditLogService.log(user, "DOCUMENTS_SUBMITTED", "AUTH", "Documents submitted for admin review");

        String reviewPath = user.getRole() == Role.DOCTOR ? "/admin/doctors" : "/admin/vendors";
        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.sendPortalNotification(
                        admin,
                        "New registration pending approval",
                        user.getFullName() + " (" + user.getRole() + ") submitted documents for review.",
                        NotificationCategory.SYSTEM,
                        reviewPath
                )
        );
    }

    public void submitDocuments(Long userId, String documentInfo) {
        submitDocuments(userId, documentInfo, null);
    }

    public void approveUser(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setAccountStatus("ACTIVE");
        user.setRejectionReason(null);
        userRepository.save(user);

        auditLogService.log(admin, "USER_APPROVED", "ADMIN",
                user.getRole().name(), userId, "Approved " + user.getFullName());
        notificationChannelService.sendApprovalNotice(user.getEmail(), user.getMobileNumber(), user.getFullName(), true);
        notificationService.sendPortalNotification(user, "Account Approved",
                "Your SmartCare 360 account has been approved. You can now sign in.",
                NotificationCategory.SYSTEM, PortalRole.loginPathForUser(user));
    }

    public void rejectUser(Long userId, User admin, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAdminApproved(false);
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        user.setAccountStatus("PENDING");
        user.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Documents did not meet requirements.");
        userRepository.save(user);

        auditLogService.log(admin, "USER_REJECTED", "ADMIN",
                user.getRole().name(), userId, user.getRejectionReason());
        notificationChannelService.sendApprovalNotice(user.getEmail(), user.getMobileNumber(), user.getFullName(), false);
        notificationService.sendPortalNotification(user, "Application Rejected",
                "Your registration was not approved. Reason: " + user.getRejectionReason()
                        + " You may update documents and resubmit.",
                NotificationCategory.SYSTEM, "/submit-documents?userId=" + user.getId());
    }

    public void resendOtp(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for OTP resend."));

        String newOtp = generateOtp();
        otpService.store(user.getEmail(), newOtp, OtpPurpose.REGISTRATION);
        if (!notificationChannelService.sendOtp(user.getEmail(), user.getMobileNumber(), newOtp)) {
            String detail = mailDeliveryDiagnostics.getLastFailure();
            if (detail == null || detail.isBlank()) {
                detail = "Email is not configured on this server.";
            }
            throw new RuntimeException("Could not send a verification code to " + user.getEmail() + ". " + detail);
        }
    }

    public Optional<User> loginUser(String emailOrMobile, String password) {
        Optional<User> userOptional = authenticate(emailOrMobile, password);
        userOptional.ifPresent(this::recordSuccessfulLogin);
        return userOptional;
    }

    public Optional<User> authenticate(String emailOrMobile, String password) {
        Optional<User> userOptional = findByEmailOrMobile(emailOrMobile);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }
        User user = userOptional.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Optional.empty();
        }
        return Optional.of(user);
    }

    public boolean passwordMatches(User user, String rawPassword) {
        return user != null && rawPassword != null && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public void recordSuccessfulLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(user, "LOGIN", "AUTH", "User logged in");
    }

    /**
     * Pharmacy portal login requires User.vendorType = PHARMACY. New registrations
     * sometimes kept NONE, so a valid new email was rejected as the wrong role.
     */
    public User reconcileVendorType(User user, PortalRole portalRole) {
        if (user == null || user.getRole() != Role.VENDOR) {
            return user;
        }
        VendorType profileType = getVendorProfile(user)
                .map(VendorProfile::getVendorType)
                .orElse(null);
        VendorType current = user.getVendorType();
        VendorType resolved = current;
        if (resolved == null || resolved == VendorType.NONE) {
            if (profileType == VendorType.PHARMACY || profileType == VendorType.LABORATORY) {
                resolved = profileType;
            } else if (portalRole == PortalRole.PHARMACY) {
                resolved = VendorType.PHARMACY;
            } else if (portalRole == PortalRole.VENDOR) {
                resolved = VendorType.LABORATORY;
            }
        }
        if (resolved != null && resolved != VendorType.NONE && resolved != current) {
            user.setVendorType(resolved);
            userRepository.save(user);
            VendorProfile profile = getVendorProfile(user).orElseGet(() -> new VendorProfile(user));
            profile.setVendorType(resolved);
            vendorProfileRepository.save(profile);
        }
        return user;
    }

    public Optional<User> findByEmailOrMobile(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        if (trimmed.contains("@")) {
            return userRepository.findByEmail(normalizeEmail(trimmed))
                    .or(() -> userRepository.findByEmailIgnoreCase(trimmed.trim()));
        }
        Optional<User> byMobile = userRepository.findByMobileNumber(normalizeMobile(trimmed));
        if (byMobile.isPresent()) {
            return byMobile;
        }
        return userRepository.findByEmail(normalizeEmail(trimmed));
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin password cannot be reset online. Contact system administrator.");
        }

        String resetOtp = generateOtp();
        otpService.store(user.getEmail(), resetOtp, OtpPurpose.PASSWORD_RESET);
        if (!notificationChannelService.sendPasswordResetOtp(user.getEmail(), user.getMobileNumber(), resetOtp)) {
            throw new RuntimeException(
                    "Could not send a password reset code to your email. Please try again later or contact support.");
        }
        auditLogService.log(user, "PASSWORD_RESET_REQUESTED", "AUTH", "Password reset OTP sent via email");
    }

    public boolean resetPassword(String email, String resetOtp, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOptional = userRepository.findByEmail(normalizedEmail);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!otpService.validate(normalizedEmail, resetOtp, OtpPurpose.PASSWORD_RESET)) {
                return false;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            auditLogService.log(user, "PASSWORD_RESET", "AUTH", "Password reset completed");
            return true;
        }
        return false;
    }

    public User createAdminAccount(String email, String password, String fullName, String mobile) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            User admin = existing.get();
            syncBootstrapAccount(admin, password);
            return userRepository.save(admin);
        }

        User admin = new User(fullName, normalizedEmail, mobile, passwordEncoder.encode(password), Role.ADMIN);
        admin.setVerified(true);
        admin.setAdminApproved(true);
        admin.setApprovalStatus(ApprovalStatus.APPROVED);
        admin.setAccountStatus("ACTIVE");
        return userRepository.save(admin);
    }

    public User createSeedUser(String fullName, String email, String mobile, String password, Role role) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            syncBootstrapAccount(user, password);
            return userRepository.save(user);
        }

        User user = new User(fullName, normalizedEmail, mobile, passwordEncoder.encode(password), role);
        user.setVerified(true);
        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setAccountStatus("ACTIVE");
        user.setDocumentInfo("Seed data - pre-verified");
        return userRepository.save(user);
    }

    private void syncBootstrapAccount(User user, String plainPassword) {
        if (!passwordEncoder.matches(plainPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(plainPassword));
        }
        user.setVerified(true);
        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setAccountStatus("ACTIVE");
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public List<User> findDoctors() {
        return userRepository.findByRole(Role.DOCTOR);
    }

    public List<User> findApprovedDoctors() {
        return userRepository.findByRoleAndAdminApprovedTrueAndApprovalStatus(Role.DOCTOR, ApprovalStatus.APPROVED);
    }

    public List<User> findPatients() {
        return userRepository.findByRole(Role.PATIENT);
    }

    public List<User> findVendors() {
        return userRepository.findByRole(Role.VENDOR);
    }

    public List<User> findPharmacyVendors() {
        return findVendorsByType(VendorType.PHARMACY).stream()
                .filter(User::isAdminApproved)
                .filter(v -> v.getApprovalStatus() == ApprovalStatus.APPROVED)
                .toList();
    }

    public List<User> findPendingApprovals() {
        return userRepository.findByApprovalStatus(ApprovalStatus.PENDING_ADMIN);
    }

    public List<User> findPendingDoctors() {
        return userRepository.findByRoleAndApprovalStatus(Role.DOCTOR, ApprovalStatus.PENDING_ADMIN);
    }

    public List<User> findPendingVendors() {
        return userRepository.findByRoleAndApprovalStatus(Role.VENDOR, ApprovalStatus.PENDING_ADMIN);
    }

    public List<User> findVendorsByType(VendorType vendorType) {
        return userRepository.findByRole(Role.VENDOR).stream()
                .filter(v -> vendorTypeMatches(v, vendorType))
                .toList();
    }

    public List<User> findPendingVendorsByType(VendorType vendorType) {
        return findPendingVendors().stream()
                .filter(v -> vendorTypeMatches(v, vendorType))
                .toList();
    }

    private boolean vendorTypeMatches(User vendor, VendorType vendorType) {
        if (vendor.getVendorType() == vendorType) {
            return true;
        }
        return getVendorProfile(vendor)
                .map(VendorProfile::getVendorType)
                .filter(type -> type == vendorType)
                .isPresent();
    }

    public PatientProfile updatePatientProfile(Long userId, PatientProfile updatedProfile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PatientProfile existingProfile = patientProfileRepository.findByUser(user)
                .orElseGet(() -> new PatientProfile(user));

        existingProfile.setAge(updatedProfile.getAge());
        existingProfile.setBloodGroup(updatedProfile.getBloodGroup());
        existingProfile.setGender(updatedProfile.getGender());
        existingProfile.setAddress(updatedProfile.getAddress());
        existingProfile.setDateOfBirth(updatedProfile.getDateOfBirth());
        existingProfile.setEmergencyContactName(updatedProfile.getEmergencyContactName());
        existingProfile.setEmergencyContactPhone(updatedProfile.getEmergencyContactPhone());
        existingProfile.setAllergies(updatedProfile.getAllergies());
        existingProfile.setMedicalHistory(updatedProfile.getMedicalHistory());
        if (updatedProfile.getPhotoFileName() != null) {
            existingProfile.setPhotoFileName(updatedProfile.getPhotoFileName());
        }

        return patientProfileRepository.save(existingProfile);
    }

    public DoctorProfile updateDoctorProfile(Long userId, DoctorProfile updatedProfile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DoctorProfile existingProfile = doctorProfileRepository.findByUser(user)
                .orElseGet(() -> new DoctorProfile(user));

        existingProfile.setSpecialization(updatedProfile.getSpecialization());
        existingProfile.setQualification(updatedProfile.getQualification());
        existingProfile.setExperienceYears(updatedProfile.getExperienceYears());
        existingProfile.setConsultationFee(updatedProfile.getConsultationFee());
        existingProfile.setAvailabilitySchedule(updatedProfile.getAvailabilitySchedule());
        existingProfile.setSlotDurationMinutes(updatedProfile.getSlotDurationMinutes());
        existingProfile.setWorkingDays(updatedProfile.getWorkingDays());
        existingProfile.setWorkStartTime(updatedProfile.getWorkStartTime());
        existingProfile.setWorkEndTime(updatedProfile.getWorkEndTime());
        existingProfile.setMaxAppointmentsPerDay(updatedProfile.getMaxAppointmentsPerDay());
        existingProfile.setDepartment(updatedProfile.getDepartment());
        existingProfile.setHospitalName(updatedProfile.getHospitalName());
        existingProfile.setClinicAddress(updatedProfile.getClinicAddress());
        existingProfile.setLicenseNumber(updatedProfile.getLicenseNumber());
        if (updatedProfile.getLicenseFileName() != null) {
            existingProfile.setLicenseFileName(updatedProfile.getLicenseFileName());
        }

        return doctorProfileRepository.save(existingProfile);
    }

    public void applyDoctorRegistrationDetails(User doctor, String specialization, String qualification,
                                               Integer experienceYears, String hospitalName, String clinicAddress,
                                               String licenseNumber) {
        DoctorProfile profile = doctorProfileRepository.findByUser(doctor).orElseGet(() -> new DoctorProfile(doctor));
        if (specialization != null && !specialization.isBlank()) {
            profile.setSpecialization(specialization.trim());
        }
        if (qualification != null && !qualification.isBlank()) {
            profile.setQualification(qualification.trim());
        }
        if (experienceYears != null) {
            profile.setExperienceYears(experienceYears);
        }
        profile.setHospitalName(hospitalName);
        profile.setClinicAddress(clinicAddress);
        profile.setLicenseNumber(licenseNumber);
        doctorProfileRepository.save(profile);
    }

    public VendorProfile updateVendorProfile(Long userId, VendorProfile updatedProfile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        VendorProfile existingProfile = vendorProfileRepository.findByUser(user)
                .orElseGet(() -> new VendorProfile(user));

        existingProfile.setBusinessName(updatedProfile.getBusinessName());
        existingProfile.setVendorType(updatedProfile.getVendorType());
        existingProfile.setContactPhone(updatedProfile.getContactPhone());
        existingProfile.setAddress(updatedProfile.getAddress());
        existingProfile.setDescription(updatedProfile.getDescription());
        existingProfile.setOwnerName(updatedProfile.getOwnerName());
        existingProfile.setLicenseNumber(updatedProfile.getLicenseNumber());
        existingProfile.setWorkingHours(updatedProfile.getWorkingHours());
        existingProfile.setDeliveryArea(updatedProfile.getDeliveryArea());
        if (updatedProfile.getLicenseFileName() != null && !updatedProfile.getLicenseFileName().isBlank()) {
            existingProfile.setLicenseFileName(updatedProfile.getLicenseFileName());
        }
        if (updatedProfile.getVendorType() == VendorType.PHARMACY
                || updatedProfile.getVendorType() == VendorType.LABORATORY) {
            existingProfile.setVendorType(updatedProfile.getVendorType());
            user.setVendorType(updatedProfile.getVendorType());
            userRepository.save(user);
        }

        return vendorProfileRepository.save(existingProfile);
    }

    public void applyVendorRegistrationDetails(User vendor, String businessName, String ownerName,
                                               String address, String licenseNumber,
                                               String workingHours, String deliveryArea) {
        VendorProfile profile = vendorProfileRepository.findByUser(vendor).orElseGet(() -> new VendorProfile(vendor));
        if (vendor.getVendorType() == VendorType.PHARMACY || vendor.getVendorType() == VendorType.LABORATORY) {
            profile.setVendorType(vendor.getVendorType());
        }
        if (businessName != null && !businessName.isBlank()) {
            profile.setBusinessName(businessName.trim());
        }
        if (ownerName != null && !ownerName.isBlank()) {
            profile.setOwnerName(ownerName.trim());
        } else if (profile.getOwnerName() == null) {
            profile.setOwnerName(vendor.getFullName());
        }
        if (address != null && !address.isBlank()) {
            profile.setAddress(address.trim());
        }
        if (licenseNumber != null && !licenseNumber.isBlank()) {
            profile.setLicenseNumber(licenseNumber.trim());
        }
        profile.setWorkingHours(workingHours);
        profile.setDeliveryArea(deliveryArea);
        if (profile.getContactPhone() == null) {
            profile.setContactPhone(vendor.getMobileNumber());
        }
        vendorProfileRepository.save(profile);
    }

    public Optional<PatientProfile> getPatientProfile(User user) {
        return patientProfileRepository.findByUser(user);
    }

    public Optional<DoctorProfile> getDoctorProfile(User user) {
        return doctorProfileRepository.findByUser(user);
    }

    public Optional<VendorProfile> getVendorProfile(User user) {
        return vendorProfileRepository.findByUser(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(900000) + 100000);
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String normalizeMobile(String mobile) {
        if (mobile == null) {
            return "";
        }
        return mobile.replaceAll("[^0-9]", "");
    }
}
