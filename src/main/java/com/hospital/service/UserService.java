package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private EmailService emailService;

    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email address is already registered.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(900000) + 100000);
        user.setOtpCode(otp);
        user.setVerified(false);

        User savedUser = userRepository.save(user);

        // Create initial profiles based on Role
        if (user.getRole() == Role.PATIENT) {
            PatientProfile patientProfile = new PatientProfile(savedUser);
            patientProfileRepository.save(patientProfile);
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

        emailService.sendOtpEmail(user.getEmail(), otp);
        return savedUser;
    }

    public boolean verifyOtp(Long userId, String enteredOtp) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getOtpCode() != null && user.getOtpCode().equals(enteredOtp.trim())) {
                user.setVerified(true);
                user.setOtpCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public String resendOtp(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            String newOtp = String.format("%06d", new Random().nextInt(900000) + 100000);
            user.setOtpCode(newOtp);
            userRepository.save(user);
            emailService.sendOtpEmail(user.getEmail(), newOtp);
            return newOtp;
        }
        throw new RuntimeException("User not found for OTP resend.");
    }

    public Optional<User> loginUser(String email, String password) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getPassword().equals(password)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
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

    public List<User> findPatients() {
        return userRepository.findByRole(Role.PATIENT);
    }

    public List<User> findVendors() {
        return userRepository.findByRole(Role.VENDOR);
    }

    // Patient Profile Update
    public PatientProfile updatePatientProfile(Long userId, PatientProfile updatedProfile) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        PatientProfile existingProfile = patientProfileRepository.findByUser(user)
                .orElseGet(() -> new PatientProfile(user));

        existingProfile.setAge(updatedProfile.getAge());
        existingProfile.setBloodGroup(updatedProfile.getBloodGroup());
        existingProfile.setGender(updatedProfile.getGender());
        existingProfile.setAddress(updatedProfile.getAddress());
        existingProfile.setMedicalHistory(updatedProfile.getMedicalHistory());

        return patientProfileRepository.save(existingProfile);
    }

    // Doctor Profile Update
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

        return doctorProfileRepository.save(existingProfile);
    }

    // Vendor Profile Update
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
}
