package com.hospital.config;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Seed Admin User
            User admin = new User("System Admin", "admin@hospital.com", "9876543210", "admin123", Role.ADMIN);
            admin.setVerified(true);
            userRepository.save(admin);

            // Seed Doctor 1
            User doc1 = new User("Dr. Sarah Jenkins", "sarah.jenkins@hospital.com", "9876543211", "doc123", Role.DOCTOR);
            doc1.setVerified(true);
            doc1 = userRepository.save(doc1);
            DoctorProfile dp1 = new DoctorProfile(doc1);
            dp1.setSpecialization("Cardiology");
            dp1.setQualification("MD, DM (Cardiology)");
            dp1.setExperienceYears(12);
            dp1.setConsultationFee(800.0);
            dp1.setAvailabilitySchedule("Mon - Sat (09:00 AM - 02:00 PM)");
            userService.updateDoctorProfile(doc1.getId(), dp1);

            // Seed Doctor 2
            User doc2 = new User("Dr. Robert Chen", "robert.chen@hospital.com", "9876543212", "doc123", Role.DOCTOR);
            doc2.setVerified(true);
            doc2 = userRepository.save(doc2);
            DoctorProfile dp2 = new DoctorProfile(doc2);
            dp2.setSpecialization("Neurology");
            dp2.setQualification("MBBS, M.Ch (Neurology)");
            dp2.setExperienceYears(9);
            dp2.setConsultationFee(1000.0);
            dp2.setAvailabilitySchedule("Mon - Fri (10:00 AM - 04:00 PM)");
            userService.updateDoctorProfile(doc2.getId(), dp2);

            // Seed Doctor 3
            User doc3 = new User("Dr. Emily Watson", "emily.watson@hospital.com", "9876543213", "doc123", Role.DOCTOR);
            doc3.setVerified(true);
            doc3 = userRepository.save(doc3);
            DoctorProfile dp3 = new DoctorProfile(doc3);
            dp3.setSpecialization("Pediatrics");
            dp3.setQualification("MBBS, DCH, MD (Pediatrics)");
            dp3.setExperienceYears(7);
            dp3.setConsultationFee(600.0);
            dp3.setAvailabilitySchedule("Mon - Sat (11:00 AM - 05:00 PM)");
            userService.updateDoctorProfile(doc3.getId(), dp3);

            // Seed Patient 1
            User patient1 = new User("John Doe", "patient@hospital.com", "9876543214", "patient123", Role.PATIENT);
            patient1.setVerified(true);
            patient1 = userRepository.save(patient1);
            PatientProfile pp1 = new PatientProfile(patient1);
            pp1.setAge(34);
            pp1.setBloodGroup("O+");
            pp1.setGender("Male");
            pp1.setAddress("123 Health Ave, Metro City");
            pp1.setMedicalHistory("No major chronic conditions.");
            userService.updatePatientProfile(patient1.getId(), pp1);

            // Seed Vendor 1 (Laboratory)
            User labVendor = new User("Apex Diagnostics Lab", "lab@hospital.com", "9876543215", "vendor123", Role.VENDOR);
            labVendor.setVendorType(VendorType.LABORATORY);
            labVendor.setVerified(true);
            labVendor = userRepository.save(labVendor);
            VendorProfile vp1 = new VendorProfile(labVendor);
            vp1.setBusinessName("Apex Pathology & Imaging Lab");
            vp1.setContactPhone("1800-LAB-TEST");
            vp1.setAddress("Block C, Medical Hub");
            vp1.setDescription("NABL Accredited Diagnostic Laboratory offering complete blood tests & body scans.");
            userService.updateVendorProfile(labVendor.getId(), vp1);

            // Seed Lab Tests
            vendorService.saveLabTest(new LabTest("Complete Blood Count (CBC)", "Blood Test", 450.0, "Includes RBC, WBC, Hemoglobin, Platelet count.", labVendor));
            vendorService.saveLabTest(new LabTest("Lipid Profile Test", "Cardiology", 900.0, "Measures Total Cholesterol, HDL, LDL, Triglycerides.", labVendor));
            vendorService.saveLabTest(new LabTest("Thyroid Profile (T3, T4, TSH)", "Endocrinology", 650.0, "Comprehensive evaluation of thyroid hormones.", labVendor));

            // Seed Vendor 2 (Pharmacy)
            User pharmacyVendor = new User("MediPlus Pharmacy", "pharmacy@hospital.com", "9876543216", "vendor123", Role.VENDOR);
            pharmacyVendor.setVendorType(VendorType.PHARMACY);
            pharmacyVendor.setVerified(true);
            pharmacyVendor = userRepository.save(pharmacyVendor);
            VendorProfile vp2 = new VendorProfile(pharmacyVendor);
            vp2.setBusinessName("MediPlus Central Pharmacy");
            vp2.setContactPhone("1800-MED-PLUS");
            vp2.setAddress("Ground Floor, Main Hospital Block");
            vp2.setDescription("24x7 Authorized Hospital Pharmacy providing essential prescription medicines.");
            userService.updateVendorProfile(pharmacyVendor.getId(), vp2);

            // Seed Pharmacy Items
            vendorService.savePharmacyItem(new PharmacyItem("Paracetamol 650mg", "Analgesic", 35.0, 500, "Pain relief & antipyretic strip of 15 tablets.", pharmacyVendor));
            vendorService.savePharmacyItem(new PharmacyItem("Amoxicillin 500mg", "Antibiotics", 120.0, 200, "Broad-spectrum antibiotic strip of 10 capsules.", pharmacyVendor));
            vendorService.savePharmacyItem(new PharmacyItem("Multivitamin & Mineral", "Supplements", 210.0, 150, "Daily health supplement bottle of 30 tablets.", pharmacyVendor));

            System.out.println(">>> Sample Hospital Data Initialized Successfully! <<<");
        }
    }
}
