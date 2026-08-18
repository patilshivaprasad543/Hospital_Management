package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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
        if (patient == null) return "redirect:/login";

        List<Appointment> appointments = appointmentService.getPatientAppointments(patient);
        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));

        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", userService.findDoctors());
        model.addAttribute("unreadNotifications", notificationService.getUnreadCount(patient));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        model.addAttribute("pharmacyOrders", pharmacyWorkflowService.getPatientOrders(patient));

        return "patient/dashboard";
    }

    @GetMapping("/symptom-wizard")
    public String symptomWizard(@RequestParam(value = "category", required = false) String category,
                                HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

        if (category != null && !category.isEmpty()) {
            model.addAttribute("matchedDoctors", smartMatchingService.findRecommendedDoctors(category));
            model.addAttribute("selectedCategory", category);
        }

        return "patient/symptom-wizard";
    }

    @GetMapping("/doctors")
    public String viewDoctors(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

        model.addAttribute("doctors", userService.findDoctors());
        return "patient/doctors";
    }

    @GetMapping("/book-appointment")
    public String showBookAppointmentForm(@RequestParam(value = "doctorId", required = false) Long doctorId,
                                           @RequestParam(value = "department", required = false) String department,
                                           HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

        model.addAttribute("doctors", userService.findDoctors());
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("department", department != null ? department : "General Consultation");
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
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

        model.addAttribute("patient", patient);
        model.addAttribute("appointments", appointmentService.getPatientAppointments(patient));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        return "patient/timeline";
    }

    @GetMapping("/prescriptions")
    public String viewPrescriptions(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("pharmacyVendors", userService.findVendors());
        return "patient/prescriptions";
    }

    @GetMapping("/lab-reports")
    public String viewLabReports(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

        model.addAttribute("appointments", appointmentService.getPatientAppointments(patient));
        return "patient/appointments";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) return "redirect:/login";

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
        if (patient == null) return "redirect:/login";

        userService.updatePatientProfile(patient.getId(), profile);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/patient/profile";
    }
}
