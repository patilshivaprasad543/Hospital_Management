package com.hospital.config;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${smartcare.admin.email:admin@smartcare360.com}")
    private String adminEmail;

    @Value("${smartcare.admin.password:Admin@360}")
    private String adminPassword;

    @Value("${smartcare.admin.name:System Administrator}")
    private String adminName;

    @Value("${smartcare.admin.mobile:9999999999}")
    private String adminMobile;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @Override
    public void run(String... args) {
        userService.createAdminAccount(adminEmail, adminPassword, adminName, adminMobile);

        if (userRepository.count() <= 1) {
            User doc1 = createApprovedDoctor("Dr. Sarah Jenkins", "sarah.jenkins@smartcare360.com", "9876543211", "doc123",
                    "Cardiology", "MD, DM (Cardiology)", 12, 800.0, "Mon - Sat (09:00 AM - 02:00 PM)");
            User doc2 = createApprovedDoctor("Dr. Robert Chen", "robert.chen@smartcare360.com", "9876543212", "doc123",
                    "Neurology", "MBBS, M.Ch (Neurology)", 9, 1000.0, "Mon - Fri (10:00 AM - 04:00 PM)");
            User doc3 = createApprovedDoctor("Dr. Emily Watson", "emily.watson@smartcare360.com", "9876543213", "doc123",
                    "Pediatrics", "MBBS, DCH, MD (Pediatrics)", 7, 600.0, "Mon - Sat (11:00 AM - 05:00 PM)");

            User patient1 = createApprovedPatient("John Doe", "patient@smartcare360.com", "9876543214", "patient123",
                    34, "O+", "Male", "123 Health Ave, Metro City", "No major chronic conditions.");

            User labVendor = createApprovedVendor("Apex Diagnostics Lab", "lab@smartcare360.com", "9876543215", "vendor123",
                    VendorType.LABORATORY, "Apex Pathology & Imaging Lab", "1800-LAB-TEST", "Block C, Medical Hub",
                    "NABL Accredited Diagnostic Laboratory offering complete blood tests & body scans.");
            vendorService.saveLabTest(new LabTest("Complete Blood Count (CBC)", "Blood Test", 450.0, "Includes RBC, WBC, Hemoglobin, Platelet count.", labVendor));
            vendorService.saveLabTest(new LabTest("Lipid Profile Test", "Cardiology", 900.0, "Measures Total Cholesterol, HDL, LDL, Triglycerides.", labVendor));
            vendorService.saveLabTest(new LabTest("Thyroid Profile (T3, T4, TSH)", "Endocrinology", 650.0, "Comprehensive evaluation of thyroid hormones.", labVendor));

            User pharmacyVendor = createApprovedVendor("MediPlus Pharmacy", "pharmacy@smartcare360.com", "9876543216", "vendor123",
                    VendorType.PHARMACY, "MediPlus Central Pharmacy", "1800-MED-PLUS", "Ground Floor, Main Hospital Block",
                    "24x7 Authorized Hospital Pharmacy providing essential prescription medicines.");
            vendorService.savePharmacyItem(new PharmacyItem("Paracetamol 650mg", "Analgesic", 35.0, 500, "Pain relief & antipyretic strip of 15 tablets.", pharmacyVendor));
            vendorService.savePharmacyItem(new PharmacyItem("Amoxicillin 500mg", "Antibiotics", 120.0, 200, "Broad-spectrum antibiotic strip of 10 capsules.", pharmacyVendor));
            vendorService.savePharmacyItem(new PharmacyItem("Multivitamin & Mineral", "Supplements", 210.0, 150, "Daily health supplement bottle of 30 tablets.", pharmacyVendor));

            System.out.println(">>> SmartCare 360 sample data initialized successfully! <<<");
            System.out.println(">>> Admin login: " + adminEmail + " <<<");
        }
    }

    private User createApprovedDoctor(String name, String email, String mobile, String password,
                                      String specialization, String qualification, int experience,
                                      double fee, String schedule) {
        User doc = userService.createSeedUser(name, email, mobile, password, Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doc);
        profile.setSpecialization(specialization);
        profile.setQualification(qualification);
        profile.setExperienceYears(experience);
        profile.setConsultationFee(fee);
        profile.setAvailabilitySchedule(schedule);
        userService.updateDoctorProfile(doc.getId(), profile);
        return doc;
    }

    private User createApprovedPatient(String name, String email, String mobile, String password,
                                       int age, String bloodGroup, String gender, String address, String history) {
        User patient = userService.createSeedUser(name, email, mobile, password, Role.PATIENT);
        PatientProfile profile = new PatientProfile(patient);
        profile.setAge(age);
        profile.setBloodGroup(bloodGroup);
        profile.setGender(gender);
        profile.setAddress(address);
        profile.setMedicalHistory(history);
        userService.updatePatientProfile(patient.getId(), profile);
        return patient;
    }

    private User createApprovedVendor(String name, String email, String mobile, String password,
                                      VendorType vendorType, String businessName, String phone,
                                      String address, String description) {
        User vendor = userService.createSeedUser(name, email, mobile, password, Role.VENDOR);
        vendor.setVendorType(vendorType);
        userService.saveUser(vendor);
        VendorProfile profile = new VendorProfile(vendor);
        profile.setBusinessName(businessName);
        profile.setVendorType(vendorType);
        profile.setContactPhone(phone);
        profile.setAddress(address);
        profile.setDescription(description);
        userService.updateVendorProfile(vendor.getId(), profile);
        return vendor;
    }
}
