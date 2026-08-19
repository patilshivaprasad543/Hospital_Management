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
    private AnnouncementService announcementService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private PatientTimelineService patientTimelineService;

    private User getLoggedInPatient(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.PATIENT) {
            return user;
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        List<Appointment> appointments = appointmentService.getPatientAppointments(patient);
        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));

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
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("department", department != null ? department : "General Consultation");
        model.addAttribute("minDate", LocalDate.now().toString());
        return "patient/book-appointment";
    }

    @PostMapping("/book-appointment")
    public String bookAppointment(@RequestParam("doctorId") Long doctorId,
                                  @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                  @RequestParam("appointmentTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime appointmentTime,
                                  @RequestParam(value = "reason", required = false) String reason,
                                  @RequestParam(value = "department", required = false) String department,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        try {
            appointmentService.bookAppointmentWithDepartment(patient.getId(), doctorId, appointmentDate, appointmentTime, reason, department);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment booked successfully! Waiting for Doctor confirmation.");
            return "redirect:/patient/appointments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/book-appointment?doctorId=" + doctorId;
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

    @GetMapping("/timeline")
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

        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("pharmacyVendors", userService.findVendors());
        return "patient/prescriptions";
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
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        Prescription rx = prescriptionService.getPatientPrescriptions(patient).stream()
                .filter(p -> p.getId().equals(prescriptionId))
                .findFirst().orElse(null);
        User vendor = userService.findById(pharmacyVendorId).orElse(null);

        if (rx != null && vendor != null) {
            String summary = "Prescription #" + rx.getId() + " - Diagnosis: " + rx.getDiagnosis();
            pharmacyWorkflowService.placeOrder(patient, rx, vendor, 250.00, summary);
            redirectAttributes.addFlashAttribute("successMessage", "Prescription medicine order placed with Pharmacy Vendor!");
        }
        return "redirect:/patient/dashboard";
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
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        userService.updatePatientProfile(patient.getId(), profile);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
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
}
