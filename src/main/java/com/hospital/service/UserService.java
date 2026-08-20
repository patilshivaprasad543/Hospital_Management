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
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Admin accounts cannot be self-registered. Contact system administrator.");
        }

        String email = normalizeEmail(user.getEmail());
        if (email.isBlank()) {
            throw new RuntimeException("Email address is required.");
        }
        user.setEmail(email);

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email address is already registered.");
        }

        String otp = generateOtp();
        user.setVerified(false);
        user.setAdminApproved(user.getRole() == Role.PATIENT);
        user.setApprovalStatus(ApprovalStatus.PENDING_OTP);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User savedUser = userRepository.save(user);

        if (user.getRole() == Role.PATIENT) {
            patientProfileRepository.save(new PatientProfile(savedUser));
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
            vendorProfile.setBusinessName(user.getFullName() + " Services");
            vendorProfileRepository.save(vendorProfile);
        }

        otpService.store(savedUser.getEmail(), otp, OtpPurpose.REGISTRATION);
        if (!notificationChannelService.sendOtp(savedUser.getEmail(), savedUser.getMobileNumber(), otp)) {
            String detail = mailDeliveryDiagnostics.getLastFailure();
            if (detail == null || detail.isBlank()) {
                detail = "Email is not configured on this server.";
            }
            throw new RuntimeException(
                    "We could not send a verification code to " + savedUser.getEmail() + ". " + detail);
        }
        auditLogService.log(savedUser, "USER_REGISTERED", "AUTH", "User registered, OTP sent via email");
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

    public void submitDocuments(Long userId, String documentInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.DOCTOR && user.getRole() != Role.VENDOR) {
            throw new RuntimeException("Document submission is only required for doctors and vendors.");
        }

        if (!user.isVerified()) {
            throw new RuntimeException("Please complete OTP verification first.");
        }

        user.setDocumentInfo(documentInfo);
        user.setApprovalStatus(ApprovalStatus.PENDING_ADMIN);
        userRepository.save(user);
        auditLogService.log(user, "DOCUMENTS_SUBMITTED", "AUTH", "Documents submitted for admin review");

        userRepository.findByRole(Role.ADMIN).forEach(admin ->
                notificationService.sendPortalNotification(
                        admin,
                        "New registration pending approval",
                        user.getFullName() + " (" + user.getRole() + ") submitted documents for review.",
                        NotificationCategory.SYSTEM,
                        "/admin/doctors"
                )
        );
    }

    public void approveUser(Long userId, User admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setAccountStatus("ACTIVE");
        userRepository.save(user);

        auditLogService.log(admin, "USER_APPROVED", "ADMIN",
                user.getRole().name(), userId, "Approved " + user.getFullName());
        notificationChannelService.sendApprovalNotice(user.getEmail(), user.getMobileNumber(), user.getFullName(), true);
    }

    public void rejectUser(Long userId, User admin, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAdminApproved(false);
        user.setApprovalStatus(ApprovalStatus.REJECTED);
        user.setAccountStatus("BLOCKED");
        userRepository.save(user);

        auditLogService.log(admin, "USER_REJECTED", "ADMIN",
                user.getRole().name(), userId, reason != null ? reason : "Rejected by admin");
        notificationChannelService.sendApprovalNotice(user.getEmail(), user.getMobileNumber(), user.getFullName(), false);
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

    public Optional<User> loginUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                auditLogService.log(user, "LOGIN", "AUTH", "User logged in");
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
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
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (!otpService.validate(email, resetOtp, OtpPurpose.PASSWORD_RESET)) {
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
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }

        User admin = new User(fullName, email, mobile, passwordEncoder.encode(password), Role.ADMIN);
        admin.setVerified(true);
        admin.setAdminApproved(true);
        admin.setApprovalStatus(ApprovalStatus.APPROVED);
        admin.setAccountStatus("ACTIVE");
        return userRepository.save(admin);
    }

    public User createSeedUser(String fullName, String email, String mobile, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }

        User user = new User(fullName, email, mobile, passwordEncoder.encode(password), role);
        user.setVerified(true);
        user.setAdminApproved(true);
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setAccountStatus("ACTIVE");
        user.setDocumentInfo("Seed data - pre-verified");
        return userRepository.save(user);
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
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
        return userRepository.findByRole(Role.VENDOR).stream()
                .filter(v -> v.getVendorType() == VendorType.PHARMACY)
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

        return doctorProfileRepository.save(existingProfile);
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

        return vendorProfileRepository.save(existingProfile);
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
}
