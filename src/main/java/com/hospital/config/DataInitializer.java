package com.hospital.config;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.AnnouncementService;
import com.hospital.service.DepartmentService;
import com.hospital.service.HospitalSettingService;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import com.hospital.service.VideoConsultationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${smartcare.admin.email}")
    private String adminEmail;

    @Value("${smartcare.admin.password}")
    private String adminPassword;

    @Value("${smartcare.admin.name}")
    private String adminName;

    @Value("${smartcare.admin.mobile}")
    private String adminMobile;

    @Autowired
    private UserService userService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private HospitalSettingService hospitalSettingService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private com.hospital.service.AdmissionService admissionService;

    @Autowired
    private com.hospital.service.BloodBankService bloodBankService;

    @Autowired
    private com.hospital.service.PrescriptionService prescriptionService;

    @Autowired
    private com.hospital.repository.PrescriptionRepository prescriptionRepository;

    @Autowired
    private com.hospital.service.InsuranceService insuranceService;

    @Autowired
    private com.hospital.repository.InsuranceRepository insuranceRepository;

    @Autowired
    private com.hospital.repository.InsuranceClaimRepository insuranceClaimRepository;

    @Autowired
    private com.hospital.service.BillingService billingService;

    @Autowired
    private com.hospital.repository.InvoiceRepository invoiceRepository;

    @Override
    public void run(String... args) {
        userService.createAdminAccount(adminEmail, adminPassword, adminName, adminMobile);
        departmentService.seedDepartmentsIfEmpty();
        hospitalSettingService.ensureDefaults();
        admissionService.seedWardsAndBedsIfEmpty();
        seedAnnouncementsIfEmpty();
        seedDemoAccountsIfMissing();

        System.out.println(">>> SmartCare 360 bootstrap complete <<<");
        System.out.println(">>> Admin login: " + adminEmail + " <<<");
    }

    private void seedDemoAccountsIfMissing() {
        Department cardiology = departmentService.getAllDepartments().stream()
                .filter(d -> d.getName().equals("Cardiology")).findFirst().orElse(null);
        Department neurology = departmentService.getAllDepartments().stream()
                .filter(d -> d.getName().equals("Neurology")).findFirst().orElse(null);
        Department pediatrics = departmentService.getAllDepartments().stream()
                .filter(d -> d.getName().equals("Pediatrics")).findFirst().orElse(null);
        Department general = departmentService.getAllDepartments().stream()
                .filter(d -> d.getName().equals("General Medicine")).findFirst().orElse(null);

        User doc1 = createApprovedDoctor("Dr. Sarah Jenkins", "sarah.jenkins@smartcare360.com", "9876543211", "doc123",
                "Cardiology", "MD, DM (Cardiology)", 12, 800.0, "Mon - Sat (09:00 AM - 02:00 PM)", cardiology);
        User doc2 = createApprovedDoctor("Dr. Robert Chen", "robert.chen@smartcare360.com", "9876543212", "doc123",
                "Neurology", "MBBS, M.Ch (Neurology)", 9, 1000.0, "Mon - Fri (10:00 AM - 04:00 PM)", neurology);
        User doc3 = createApprovedDoctor("Dr. Emily Watson", "emily.watson@smartcare360.com", "9876543213", "doc123",
                "Pediatrics", "MBBS, DCH, MD (Pediatrics)", 7, 600.0, "Mon - Sat (11:00 AM - 05:00 PM)", pediatrics);
        createApprovedDoctor("Dr. Anita Mehra", "anita.mehra@smartcare360.com", "9876543217", "doc123",
                "General Medicine", "MBBS, MD (Medicine)", 10, 500.0, "Mon - Sat (09:00 AM - 05:00 PM)", general);

        User pat = createApprovedPatient("John Doe", "patient@smartcare360.com", "9876543214", "patient123",
                34, "O+", "Male", "123 Health Ave, Metro City", "No major chronic conditions.");

        User labVendor = createApprovedVendor("Apex Diagnostics Lab", "lab@smartcare360.com", "9876543215", "vendor123",
                VendorType.LABORATORY, "Apex Pathology & Imaging Lab", "1800-LAB-TEST", "Block C, Medical Hub",
                "NABL Accredited Diagnostic Laboratory offering complete blood tests & body scans.");
        seedLabTestsIfMissing(labVendor);

        User pharmacyVendor = createApprovedVendor("MediPlus Pharmacy", "pharmacy@smartcare360.com", "9876543216", "vendor123",
                VendorType.PHARMACY, "MediPlus Central Pharmacy", "1800-MED-PLUS", "Ground Floor, Main Hospital Block",
                "24x7 Authorized Hospital Pharmacy providing essential prescription medicines.");
        VendorProfile pharmacyProfile = userService.getVendorProfile(pharmacyVendor).orElse(new VendorProfile(pharmacyVendor));
        pharmacyProfile.setOwnerName("MediPlus Pharmacy");
        pharmacyProfile.setLicenseNumber("DL-PHARM-360");
        pharmacyProfile.setWorkingHours("24x7");
        pharmacyProfile.setDeliveryArea("Hospital campus and 10 km city radius");
        userService.updateVendorProfile(pharmacyVendor.getId(), pharmacyProfile);
        seedPharmacyItemsIfMissing(pharmacyVendor);
        seedSampleVideoConsultation(doc1, pat);
        seedBloodBankDataIfMissing(doc1, pat);
        seedInsuranceDataIfMissing(pat);
    }

    private void seedInsuranceDataIfMissing(User patient) {
        if (insuranceRepository.findAll().isEmpty()) {
            Insurance starInsurance = insuranceService.registerPolicy(
                    patient,
                    "Star Health & Allied Insurance",
                    "STAR-HLTH-882910",
                    "Comprehensive Family Floater",
                    java.time.LocalDate.now().minusMonths(6),
                    java.time.LocalDate.now().plusMonths(6),
                    500000.0
            );

            // Create sample invoices for claims
            Invoice inv1 = billingService.createInvoice(patient, "CONSULTATION", "Specialist Cardiology Consultation & Diagnostic Workup", 14500.0, null);
            Invoice inv2 = billingService.createInvoice(patient, "LABORATORY", "Comprehensive Metabolic Panel, Echocardiogram & Blood Cross-Match", 28000.0, null);
            Invoice inv3 = billingService.createInvoice(patient, "PHARMACY", "Post-Op Cardiac Medications & Anticoagulant Therapy", 8200.0, null);
            Invoice inv4 = billingService.createInvoice(patient, "OTHER", "Inpatient High-Dependency Ward Stay & Transfusion Setup", 12000.0, null);

            // Create sample claims in different states
            insuranceService.submitClaim(starInsurance, inv1, 14500.0, "Pre-authorization claim submitted for OPD diagnostics", patient);

            InsuranceClaim claim2 = insuranceService.submitClaim(starInsurance, inv2, 28000.0, "Emergency diagnostics and pre-surgical workup", patient);
            claim2.setStatus("UNDER_REVIEW");
            claim2.setRemarks("Insurer requested clinical history sheets and lab verification");
            insuranceClaimRepository.save(claim2);

            InsuranceClaim claim3 = insuranceService.submitClaim(starInsurance, inv3, 8200.0, "Critical post-operative specialty medication", patient);
            claim3.setStatus("APPROVED");
            claim3.setRemarks("Pre-authorized 100% cashless approval granted by TPA");
            insuranceClaimRepository.save(claim3);

            InsuranceClaim claim4 = insuranceService.submitClaim(starInsurance, inv4, 12000.0, "HD Care bed charges", patient);
            claim4.setStatus("SETTLED");
            claim4.setRemarks("Claim settled directly to hospital account via NEFT");
            insuranceClaimRepository.save(claim4);
        }
    }

    private void seedBloodBankDataIfMissing(User doctor, User patient) {
        if (bloodBankService.getAllUnits().isEmpty()) {
            bloodBankService.registerBloodUnit("BLD-O-1001", BloodGroup.O_POSITIVE, BloodComponentType.PACKED_RED_CELLS, "David Miller", "9876500111", 450, java.time.LocalDate.now().plusDays(35), null);
            bloodBankService.registerBloodUnit("BLD-O-1002", BloodGroup.O_POSITIVE, BloodComponentType.WHOLE_BLOOD, "Sarah Connor", "9876500112", 450, java.time.LocalDate.now().plusDays(30), null);
            bloodBankService.registerBloodUnit("BLD-A-1003", BloodGroup.A_POSITIVE, BloodComponentType.PACKED_RED_CELLS, "Robert Chen", "9876500113", 450, java.time.LocalDate.now().plusDays(40), null);
            bloodBankService.registerBloodUnit("BLD-B-1004", BloodGroup.B_POSITIVE, BloodComponentType.PLATELETS, "Anita Sharma", "9876500114", 300, java.time.LocalDate.now().plusDays(5), null);
            bloodBankService.registerBloodUnit("BLD-AB-1005", BloodGroup.AB_POSITIVE, BloodComponentType.FRESH_FROZEN_PLASMA, "Michael Scott", "9876500115", 250, java.time.LocalDate.now().plusDays(180), null);
        }

        if (prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient).isEmpty()) {
            List<PrescriptionItem> items = new java.util.ArrayList<>();
            items.add(new PrescriptionItem("Iron Complex & Folic Acid", "100mg", "1-0-0", "30 Days", "Take with meals"));
            prescriptionService.createPrescription(
                    null, doctor, patient,
                    "Severe Microcytic Anemia & Post-Surgical Recovery",
                    "Requires 2 units packed red cells transfusion prior to elective surgery. Ensure cross-matching.",
                    java.time.LocalDate.now().plusDays(14),
                    items,
                    BloodGroup.O_POSITIVE,
                    BloodComponentType.PACKED_RED_CELLS,
                    2,
                    "Pre-operative blood transfusion for Hb optimization (Current Hb 6.8 g/dL)"
            );
        }
    }

    private void seedSampleVideoConsultation(User doctor, User patient) {
        if (appointmentRepository.findByDoctorOrderByCreatedAtDesc(doctor).isEmpty()) {
            Appointment appt = new Appointment();
            appt.setDoctor(doctor);
            appt.setPatient(patient);
            appt.setAppointmentDate(java.time.LocalDate.now());
            appt.setAppointmentTime(java.time.LocalTime.of(10, 0));
            appt.setReason("Cardiology Follow-up Video Consultation");
            appt.setStatus(AppointmentStatus.CONFIRMED);
            appt.setConsultationType(ConsultationType.VIDEO);
            appointmentRepository.save(appt);
            videoConsultationService.createVideoRoom(appt);
        }
    }

    private void seedLabTestsIfMissing(User labVendor) {
        if (vendorService.getLabTestsByVendor(labVendor).isEmpty()) {
            vendorService.saveLabTest(new LabTest("Complete Blood Count (CBC)", "Blood Test", 450.0, "Includes RBC, WBC, Hemoglobin, Platelet count.", labVendor));
            vendorService.saveLabTest(new LabTest("Lipid Profile Test", "Cardiology", 900.0, "Measures Total Cholesterol, HDL, LDL, Triglycerides.", labVendor));
            vendorService.saveLabTest(new LabTest("Thyroid Profile (T3, T4, TSH)", "Endocrinology", 650.0, "Comprehensive evaluation of thyroid hormones.", labVendor));
        }
    }

    private void seedPharmacyItemsIfMissing(User pharmacyVendor) {
        if (vendorService.getPharmacyItemsByVendor(pharmacyVendor).isEmpty()) {
            PharmacyItem para = new PharmacyItem("Paracetamol 650mg", "Analgesic", 35.0, 500, "Pain relief & antipyretic strip of 15 tablets.", pharmacyVendor);
            para.setManufacturer("Cipla");
            para.setBatchNumber("PCM-650-A1");
            para.setExpiryDate(java.time.LocalDate.now().plusMonths(18));
            vendorService.savePharmacyItem(para);
            PharmacyItem amox = new PharmacyItem("Amoxicillin 500mg", "Antibiotics", 120.0, 200, "Broad-spectrum antibiotic strip of 10 capsules.", pharmacyVendor);
            amox.setManufacturer("Sun Pharma");
            amox.setBatchNumber("AMX-500-B2");
            amox.setExpiryDate(java.time.LocalDate.now().plusMonths(12));
            vendorService.savePharmacyItem(amox);
            PharmacyItem multi = new PharmacyItem("Multivitamin & Mineral", "Supplements", 210.0, 150, "Daily health supplement bottle of 30 tablets.", pharmacyVendor);
            multi.setManufacturer("Himalaya");
            multi.setBatchNumber("VIT-30-C3");
            multi.setExpiryDate(java.time.LocalDate.now().plusMonths(24));
            vendorService.savePharmacyItem(multi);
        }
    }

    private User createApprovedDoctor(String name, String email, String mobile, String password,
                                      String specialization, String qualification, int experience,
                                      double fee, String schedule, Department department) {
        User doc = userService.createSeedUser(name, email, mobile, password, Role.DOCTOR);
        DoctorProfile profile = new DoctorProfile(doc);
        profile.setSpecialization(specialization);
        profile.setQualification(qualification);
        profile.setExperienceYears(experience);
        profile.setConsultationFee(fee);
        profile.setAvailabilitySchedule(schedule);
        profile.setDepartment(department);
        profile.setWorkingDays("MON,TUE,WED,THU,FRI,SAT");
        profile.setWorkStartTime("09:00");
        profile.setWorkEndTime("17:00");
        profile.setSlotDurationMinutes(30);
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
        profile.setEmergencyContactName("Emergency Contact");
        profile.setEmergencyContactPhone("9999999998");
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

    private void seedAnnouncementsIfEmpty() {
        if (announcementService.getAll().isEmpty()) {
            announcementService.create("Welcome to SmartCare 360",
                    "Our digital hospital platform is now live. Book appointments, view prescriptions, and access lab reports online.",
                    "ALL", null);
            announcementService.create("Free Health Checkup Camp",
                    "Free general health screening camp on the first Saturday of every month. Register at the reception desk.",
                    "PATIENT", null);
            announcementService.create("Doctor Schedule Update",
                    "Please update your weekly availability and mark leave dates in your profile to avoid booking conflicts.",
                    "DOCTOR", null);
        }
    }
}
