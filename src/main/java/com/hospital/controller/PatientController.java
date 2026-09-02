package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.PrescriptionRepository;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient")
public class PatientController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private SmartMatchingService smartMatchingService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private PharmacyWorkflowService pharmacyWorkflowService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PatientTimelineService patientTimelineService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private AdmissionService admissionService;

    private User getLoggedInPatient(HttpSession session) {
        return UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/"})
    public String patientRootRedirect() {
        return "redirect:/patient/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<Appointment> appointments = appointmentService.getPatientAppointments(patient);
        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        appointmentService.sendDueReminders(patient);

        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", userService.findApprovedDoctors());
        model.addAttribute("unreadNotifications", notificationService.getUnreadCount(patient));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        model.addAttribute("pharmacyOrders", pharmacyWorkflowService.getPatientOrders(patient));
        model.addAttribute("announcements", announcementService.getActiveForRole("PATIENT"));

        return "patient/dashboard";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }
        model.addAttribute("notifications", notificationService.getNotificationsForUser(patient));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(patient));
        model.addAttribute("loggedInUser", patient);
        return "patient/notifications";
    }

    @GetMapping("/symptom-wizard")
    public String symptomWizard(@RequestParam(value = "category", required = false) String category,
                                HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        if (category != null && !category.isEmpty()) {
            model.addAttribute("matchedDoctors", smartMatchingService.findRecommendedDoctors(category));
            model.addAttribute("selectedCategory", category);
        }

        return "patient/symptom-wizard";
    }

    @GetMapping("/doctors")
    public String viewDoctors(@RequestParam(value = "department", required = false) String department,
                              @RequestParam(value = "search", required = false) String search,
                              HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<User> doctors = userService.findApprovedDoctors();
        Map<Long, DoctorProfile> doctorProfiles = new HashMap<>();
        Map<Long, Double> doctorRatings = new HashMap<>();

        for (User doc : doctors) {
            userService.getDoctorProfile(doc).ifPresent(p -> doctorProfiles.put(doc.getId(), p));
            doctorRatings.put(doc.getId(), feedbackService.getDoctorAverageRating(doc));
        }

        if (department != null && !department.isBlank()) {
            doctors = doctors.stream().filter(doc -> {
                DoctorProfile p = doctorProfiles.get(doc.getId());
                if (p == null) return false;
                if (p.getDepartment() != null && p.getDepartment().getName().equalsIgnoreCase(department)) {
                    return true;
                }
                return p.getSpecialization() != null && p.getSpecialization().equalsIgnoreCase(department);
            }).toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            doctors = doctors.stream().filter(doc -> {
                DoctorProfile p = doctorProfiles.get(doc.getId());
                return doc.getFullName().toLowerCase().contains(q)
                        || (p != null && p.getSpecialization() != null && p.getSpecialization().toLowerCase().contains(q));
            }).toList();
        }

        model.addAttribute("doctors", doctors);
        model.addAttribute("doctorProfiles", doctorProfiles);
        model.addAttribute("doctorRatings", doctorRatings);
        model.addAttribute("departments", departmentService.getActiveDepartments());
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("searchQuery", search);
        return "patient/doctors";
    }

    @GetMapping("/doctors/{id}")
    public String viewDoctorProfile(@PathVariable Long id, HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        User doctor = userService.findById(id).orElse(null);
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return "redirect:/patient/doctors";
        }
        model.addAttribute("doctor", doctor);
        model.addAttribute("profile", userService.getDoctorProfile(doctor).orElse(new DoctorProfile(doctor)));
        model.addAttribute("rating", feedbackService.getDoctorAverageRating(doctor));
        model.addAttribute("ratingCount", feedbackService.getDoctorRatingCount(doctor));
        return "patient/doctor-profile";
    }

    @GetMapping("/book-appointment")
    public String showBookAppointmentForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                           @RequestParam(value = "department", required = false) String department,
                                           HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        Map<Long, String> doctorDepartments = new HashMap<>();
        for (User doc : userService.findApprovedDoctors()) {
            userService.getDoctorProfile(doc).ifPresent(p -> {
                if (p.getDepartment() != null) {
                    doctorDepartments.put(doc.getId(), p.getDepartment().getName());
                } else if (p.getSpecialization() != null) {
                    doctorDepartments.put(doc.getId(), p.getSpecialization());
                }
            });
        }

        model.addAttribute("doctors", userService.findApprovedDoctors());
        model.addAttribute("departments", departmentService.getActiveDepartments());
        model.addAttribute("doctorDepartments", doctorDepartments);
        model.addAttribute("doctorProfiles", doctorProfilesForBooking(userService.findApprovedDoctors()));
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("department", department);
        model.addAttribute("minDate", LocalDate.now().toString());
        return "patient/book-appointment";
    }

    private Map<Long, DoctorProfile> doctorProfilesForBooking(List<User> doctors) {
        Map<Long, DoctorProfile> map = new HashMap<>();
        for (User doc : doctors) {
            userService.getDoctorProfile(doc).ifPresent(p -> map.put(doc.getId(), p));
        }
        return map;
    }

    @PostMapping("/book-appointment")
    public String bookAppointment(@RequestParam("doctorId") Long doctorId,
                                  @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                  @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime,
                                  @RequestParam(value = "reason", required = false) String reason,
                                  @RequestParam(value = "department", required = false) String department,
                                  @RequestParam(value = "consultationType", required = false) ConsultationType consultationType,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            Appointment appt = appointmentService.bookAppointmentWithDepartment(patient.getId(), doctorId, appointmentDate, appointmentTime, reason, department, consultationType != null ? consultationType : ConsultationType.IN_PERSON);
            if (consultationType == ConsultationType.VIDEO) {
                videoConsultationService.createVideoRoom(appt);
                redirectAttributes.addFlashAttribute("successMessage", "📹 Video consultation booked successfully! Video room generated.");
                return "redirect:/patient/video-consultations";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully! Waiting for Doctor confirmation.");
            return "redirect:/patient/appointments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/book-appointment?doctorId=" + doctorId;
        }
    }

    @GetMapping("/book-video-consultation")
    public String showBookVideoConsultationForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                               HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<User> approvedDoctors = userService.findApprovedDoctors();

        model.addAttribute("patient", patient);
        model.addAttribute("loggedInUser", patient);
        model.addAttribute("doctors", approvedDoctors);
        model.addAttribute("doctorProfiles", doctorProfilesForBooking(approvedDoctors));
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("minDate", LocalDate.now().toString());
        return "patient/book-video-consultation";
    }

    @PostMapping("/book-video-consultation")
    public String processBookVideoConsultation(@RequestParam("doctorId") Long doctorId,
                                              @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                              @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime,
                                              @RequestParam(value = "reason", required = false) String reason,
                                              HttpSession session,
                                              RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            User doctor = userService.findById(doctorId).orElseThrow(() -> new RuntimeException("Doctor not found"));
            String deptName = "General Medicine";
            java.util.Optional<DoctorProfile> prof = userService.getDoctorProfile(doctor);
            if (prof.isPresent()) {
                if (prof.get().getDepartment() != null) deptName = prof.get().getDepartment().getName();
                else if (prof.get().getSpecialization() != null) deptName = prof.get().getSpecialization();
            }

            Appointment appt = appointmentService.bookAppointmentWithDepartment(
                    patient.getId(), doctorId, appointmentDate, appointmentTime,
                    (reason != null && !reason.isBlank() ? reason : "Telemedicine Video Consultation"),
                    deptName, ConsultationType.VIDEO
            );

            videoConsultationService.createVideoRoom(appt);

            redirectAttributes.addFlashAttribute("successMessage", "📹 Video consultation with Dr. " + doctor.getFullName() + " booked successfully! Video room link ready.");
            return "redirect:/patient/video-consultations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/book-video-consultation?doctorId=" + doctorId;
        }
    }

    @PostMapping("/check-in/{id}")
    public String digitalCheckIn(@PathVariable("id") Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            Appointment updated = appointmentService.checkInPatient(id);
            redirectAttributes.addFlashAttribute("successMessage", "Digital Check-In Successful! Your Queue Ticket is " + updated.getQueueTicket() + ". Please wait to be called.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/appointments/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id, HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        Appointment app = appointmentService.findById(id).orElse(null);
        if (app == null || !app.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/appointments";
        }
        model.addAttribute("appointment", app);
        model.addAttribute("minDate", LocalDate.now().toString());
        return "patient/reschedule";
    }

    @PostMapping("/appointments/{id}/reschedule")
    public String rescheduleAppointment(@PathVariable Long id,
                                        @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                        @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        try {
            appointmentService.rescheduleAppointment(id, patient, appointmentDate, appointmentTime);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment rescheduled. Waiting for doctor confirmation.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/records")
    public String medicalRecords(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointmentService.getPatientAppointments(patient));
        model.addAttribute("consultations", consultationService.getPatientConsultations(patient));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        model.addAttribute("invoices", billingService.getPatientInvoices(patient));
        model.addAttribute("timelineEvents", patientTimelineService.buildTimeline(patient));
        return "patient/records";
    }
    public String viewHealthTimeline(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("patient", patient);
        model.addAttribute("timelineEvents", patientTimelineService.buildTimeline(patient));
        return "patient/timeline";
    }

    @GetMapping("/prescriptions")
    public String viewPrescriptions(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patient);
        List<User> pharmacyVendors = userService.findPharmacyVendors();

        Map<Long, PharmacyOrder> latestOrderByPrescription = new HashMap<>();
        for (PharmacyOrder order : pharmacyWorkflowService.getPatientOrders(patient)) {
            if (order.getPrescription() != null) {
                latestOrderByPrescription.putIfAbsent(order.getPrescription().getId(), order);
            }
        }

        Map<Long, Map<Long, Double>> priceEstimates = new HashMap<>();
        for (Prescription rx : prescriptions) {
            Map<Long, Double> vendorPrices = new HashMap<>();
            for (User vendor : pharmacyVendors) {
                vendorPrices.put(vendor.getId(), pharmacyWorkflowService.estimateOrderTotal(rx, vendor));
            }
            priceEstimates.put(rx.getId(), vendorPrices);
        }

        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("pharmacyVendors", pharmacyVendors);
        model.addAttribute("patientProfile", profile);
        model.addAttribute("latestOrderByPrescription", latestOrderByPrescription);
        model.addAttribute("priceEstimates", priceEstimates);
        return "patient/prescriptions";
    }

    @GetMapping("/pharmacy-orders")
    public String viewPharmacyOrders(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("pharmacyOrders", pharmacyWorkflowService.getPatientOrders(patient));
        return "patient/pharmacy-orders";
    }

    @GetMapping("/lab-reports")
    public String viewLabReports(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        model.addAttribute("labVendors", userService.findVendors());
        return "patient/lab-reports";
    }

    @PostMapping("/lab-request/{id}/select-vendor")
    public String selectLabVendor(@PathVariable("id") Long id,
                                  @RequestParam("labVendorId") Long labVendorId,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        User labVendor = userService.findById(labVendorId).orElse(null);
        labWorkflowService.assignVendorAndBook(id, labVendor);
        redirectAttributes.addFlashAttribute("successMessage", "Laboratory vendor assigned! Sample processing requested.");
        return "redirect:/patient/lab-reports";
    }

    @PostMapping("/order-pharmacy")
    public String placePharmacyOrder(@RequestParam("prescriptionId") Long prescriptionId,
                                     @RequestParam("pharmacyVendorId") Long pharmacyVendorId,
                                     @RequestParam(value = "deliveryAddress", required = false) String deliveryAddress,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        Prescription rx = prescriptionService.findByIdForPatient(prescriptionId, patient).orElse(null);
        User vendor = userService.findById(pharmacyVendorId).orElse(null);

        if (rx == null || vendor == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid prescription or pharmacy vendor.");
            return "redirect:/patient/prescriptions";
        }

        if (vendor.getVendorType() != VendorType.PHARMACY) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a pharmacy vendor.");
            return "redirect:/patient/prescriptions";
        }

        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
            deliveryAddress = profile.getAddress();
        }
        if (deliveryAddress == null || deliveryAddress.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please provide a delivery address.");
            return "redirect:/patient/prescriptions";
        }

        try {
            pharmacyWorkflowService.placeOrder(patient, rx, vendor, deliveryAddress);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Medicine order placed successfully! Track delivery status below.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/pharmacy-orders";
    }

    @GetMapping("/appointments")
    public String viewAppointments(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("appointments", appointmentService.getPatientAppointments(patient));
        model.addAttribute("feedbackSubmitted", appointmentService.getPatientAppointments(patient).stream()
                .map(Appointment::getId)
                .filter(feedbackService::hasFeedback)
                .collect(java.util.stream.Collectors.toSet()));
        return "patient/appointments";
    }

    @PostMapping("/appointments/{id}/cancel")
    public String cancelAppointment(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        try {
            appointmentService.cancelAppointment(id, patient);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @PostMapping("/appointments/{id}/feedback")
    public String submitFeedback(@PathVariable Long id,
                                 @RequestParam int rating,
                                 @RequestParam(required = false) String comment,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        try {
            Appointment app = appointmentService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Appointment not found"));
            feedbackService.submitFeedback(app, patient, rating, comment);
            redirectAttributes.addFlashAttribute("successMessage", "Thank you for your feedback!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    @GetMapping("/language")
    public String languageSettingsPage(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("currentLang", profile.getPreferredLanguage() != null ? profile.getPreferredLanguage() : "en");
        model.addAttribute("activePage", "language");
        return "patient/language";
    }

    @PostMapping("/language")
    public String updateLanguagePreference(@RequestParam("lang") String lang,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        profile.setPreferredLanguage(lang);
        userService.updatePatientProfile(patient.getId(), profile);
        session.setAttribute("smartcare_lang", lang);

        redirectAttributes.addFlashAttribute("successMessage", "✅ Language preference successfully updated!");
        return "redirect:/patient/language";
    }

    @PostMapping("/api/language")
    @ResponseBody
    public ResponseEntity<?> updateLanguageApi(@RequestParam("lang") String lang, HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient != null) {
            PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
            profile.setPreferredLanguage(lang);
            userService.updatePatientProfile(patient.getId(), profile);
            session.setAttribute("smartcare_lang", lang);
        }
        return ResponseEntity.ok(Map.of("status", "success", "language", lang));
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        return "patient/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute PatientProfile profile,
                                @RequestParam(value = "photo", required = false) org.springframework.web.multipart.MultipartFile photo,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            if (photo != null && !photo.isEmpty()) {
                profile.setPhotoFileName(fileStorageService.storePatientPhoto(patient.getId(), photo));
            }
            userService.updatePatientProfile(patient.getId(), profile);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/profile";
    }

    @GetMapping("/bills")
    public String viewBills(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        model.addAttribute("invoices", billingService.getPatientInvoices(patient));
        return "patient/bills";
    }

    @PostMapping("/bills/{id}/pay")
    public String payBill(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";
        try {
            billingService.payInvoice(id, patient);
            redirectAttributes.addFlashAttribute("successMessage", "Payment successful!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/bills";
    }

    @GetMapping("/invoice/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long id, HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return ResponseEntity.status(401).build();
        return billingService.getPatientInvoices(patient).stream()
                .filter(i -> i.getId().equals(id))
                .findFirst()
                .map(inv -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + inv.getInvoiceNumber() + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfService.generateInvoicePdf(inv, patient)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/prescription/{id}/pdf")
    public ResponseEntity<byte[]> downloadPrescriptionPdf(@PathVariable Long id, HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return ResponseEntity.status(401).build();
        return prescriptionService.getPatientPrescriptions(patient).stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(rx -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prescription-" + rx.getId() + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfService.generatePrescriptionPdf(rx, rx.getDoctor(), patient)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/lab-report/{id}/pdf")
    public ResponseEntity<byte[]> downloadLabReportPdf(@PathVariable Long id, HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return ResponseEntity.status(401).build();
        return labWorkflowService.getPatientLabRequests(patient).stream()
                .filter(r -> r.getId().equals(id) && r.getReportResult() != null)
                .findFirst()
                .map(lab -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=lab-report-" + lab.getId() + ".pdf")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdfService.generateLabReportPdf(lab, patient)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/beds")
    public String viewBeds(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("patient", patient);
        model.addAttribute("availableBeds", admissionService.getAvailableBeds());
        model.addAttribute("totalBedsCount", admissionService.getTotalBedCount());
        model.addAttribute("availableBedsCount", admissionService.getAvailableBedCount());
        model.addAttribute("occupiedBedsCount", admissionService.getOccupiedBedCount());
        model.addAttribute("wards", admissionService.getAllWards());
        model.addAttribute("patientAdmissions", admissionService.getAdmissionsByPatient(patient));
        return "patient/beds";
    }

    @GetMapping("/book-bed")
    public String showBookBedForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                  @RequestParam(value = "bedId", required = false) Long bedId,
                                  HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("patient", patient);
        model.addAttribute("doctors", userService.findApprovedDoctors());
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("selectedBedId", bedId);
        model.addAttribute("availableBeds", admissionService.getAvailableBeds());
        model.addAttribute("wards", admissionService.getAllWards());
        return "patient/book-bed";
    }

    @PostMapping("/book-bed")
    public String processBookBed(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                 @RequestParam(value = "bedId", required = false) Long bedId,
                                 @RequestParam("reason") String reason,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            admissionService.requestBedBooking(patient, doctorId, bedId, reason, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Bed booking request submitted for your selected bed! Pending admin allocation.");
            return "redirect:/patient/beds";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/book-bed";
        }
    }

    @PostMapping("/beds/{id}/cancel")
    public String cancelBedBooking(@PathVariable("id") Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            admissionService.cancelBedBooking(id, patient);
            redirectAttributes.addFlashAttribute("successMessage", "Bed booking request cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/patient/beds";
    }

    @Autowired
    private com.hospital.repository.PreCheckInRepository preCheckInRepository;

    @GetMapping("/appointments/{id}/pre-checkin")
    public String showPreCheckIn(@PathVariable("id") Long id, HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        Appointment appt = appointmentService.getAppointmentById(id);
        if (appt == null || !appt.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/appointments";
        }

        model.addAttribute("patient", patient);
        model.addAttribute("loggedInUser", patient);
        model.addAttribute("appointment", appt);
        model.addAttribute("preCheckIn", preCheckInRepository.findByAppointment(appt).orElse(null));
        return "patient/pre-checkin";
    }

    @PostMapping("/appointments/{id}/pre-checkin")
    public String processPreCheckIn(@PathVariable("id") Long id,
                                   @RequestParam(value = "confirmedAllergies", required = false) String confirmedAllergies,
                                   @RequestParam(value = "currentMedications", required = false) String currentMedications,
                                   @RequestParam(value = "chiefComplaint", required = false) String chiefComplaint,
                                   @RequestParam(value = "emergencyContactName", required = false) String emergencyContactName,
                                   @RequestParam(value = "emergencyContactPhone", required = false) String emergencyContactPhone,
                                   HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        Appointment appt = appointmentService.getAppointmentById(id);
        if (appt == null || !appt.getPatient().getId().equals(patient.getId())) {
            return "redirect:/patient/appointments";
        }

        com.hospital.model.PreCheckIn preCheckIn = preCheckInRepository.findByAppointment(appt)
                .orElseGet(() -> new com.hospital.model.PreCheckIn());

        preCheckIn.setAppointment(appt);
        preCheckIn.setPatient(patient);
        preCheckIn.setConfirmedAllergies(confirmedAllergies);
        preCheckIn.setCurrentMedications(currentMedications);
        preCheckIn.setChiefComplaint(chiefComplaint);
        preCheckIn.setEmergencyContactName(emergencyContactName);
        preCheckIn.setEmergencyContactPhone(emergencyContactPhone);
        preCheckIn.setCompletedAt(java.time.LocalDateTime.now());
        preCheckInRepository.save(preCheckIn);

        redirectAttributes.addFlashAttribute("successMessage", "Pre-Check-In completed successfully! Your doctor has been notified.");
        return "redirect:/patient/appointments";
    }
}
