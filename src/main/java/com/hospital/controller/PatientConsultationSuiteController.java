package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/patient/consultation-suite")
public class PatientConsultationSuiteController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private AmbulanceService ambulanceService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private NotificationService notificationService;

    private User getLoggedInPatient(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/"})
    public String consultationSuite(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        List<Appointment> appointments = appointmentService.getPatientAppointments(patient);
        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patient);
        List<LabRequest> labRequests = labWorkflowService.getPatientLabRequests(patient);

        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("appointments", appointments);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("labRequests", labRequests);
        model.addAttribute("availableAmbulances", ambulanceService.getAvailableAmbulances());
        model.addAttribute("doctors", userService.findApprovedDoctors());
        model.addAttribute("loggedInUser", patient);

        return "patient/consultation-suite";
    }

    @PostMapping("/bed-request")
    public String requestBedBooking(@RequestParam("wardType") String wardType,
                                    @RequestParam("reason") String reason,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        notificationService.sendNotification(patient, "IPD Bed Allocation Requested",
                "Your IPD Bed reservation request for " + wardType + " ward has been submitted for hospital approval.",
                NotificationCategory.SYSTEM, "/patient/consultation-suite");

        return "redirect:/patient/consultation-suite?bedRequested=true";
    }
}
