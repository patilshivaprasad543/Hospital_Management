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
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private ConsultationService consultationService;

    private User getLoggedInDoctor(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.DOCTOR) {
            return user;
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        List<Appointment> pendingAppointments = appointmentService.getDoctorAppointmentsByStatus(doctor, AppointmentStatus.PENDING);
        List<Appointment> confirmedAppointments = appointmentService.getDoctorAppointmentsByStatus(doctor, AppointmentStatus.CONFIRMED);
        DoctorProfile profile = userService.getDoctorProfile(doctor).orElse(new DoctorProfile(doctor));

        // Filter checked-in patients for live queue
        List<Appointment> checkedInQueue = appointmentService.getDoctorAppointments(doctor).stream()
                .filter(a -> a.getState() == AppointmentState.CHECKED_IN || a.getState() == AppointmentState.IN_CONSULTATION)
                .toList();

        model.addAttribute("doctor", doctor);
        model.addAttribute("profile", profile);
        model.addAttribute("pendingAppointments", pendingAppointments);
        model.addAttribute("confirmedAppointments", confirmedAppointments);
        model.addAttribute("checkedInQueue", checkedInQueue);
        model.addAttribute("prescriptions", prescriptionService.getDoctorPrescriptions(doctor));

        return "doctor/dashboard";
    }

    @PostMapping("/appointment/{id}/start-consultation")
    public String startConsultation(@PathVariable("id") Long id,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";

        appointmentService.findById(id).ifPresent(app -> {
            app.setState(AppointmentState.IN_CONSULTATION);
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.CONFIRMED, "In Consultation");
            consultationService.startConsultation(app, doctor);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Consultation started!");
        return "redirect:/doctor/consultation/" + id;
    }

    @GetMapping("/consultation/{id}")
    public String consultationPage(@PathVariable Long id, HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        Appointment app = appointmentService.findById(id).orElse(null);
        if (app == null) return "redirect:/doctor/dashboard";
        model.addAttribute("appointment", app);
        model.addAttribute("patient", app.getPatient());
        model.addAttribute("patientProfile", userService.getPatientProfile(app.getPatient()).orElse(new PatientProfile(app.getPatient())));
        model.addAttribute("consultation", consultationService.findByAppointment(id).orElse(null));
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(app.getPatient()));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(app.getPatient()));
        return "doctor/consultation";
    }

    @PostMapping("/consultation/{id}/complete")
    public String completeConsultation(@PathVariable Long id,
                                       @RequestParam String symptoms,
                                       @RequestParam String diagnosis,
                                       @RequestParam String treatment,
                                       @RequestParam(required = false) String notes,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        consultationService.findByAppointment(id).ifPresent(c -> {
            consultationService.completeConsultation(c.getId(), symptoms, diagnosis, treatment, notes, followUpDate, doctor);
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.COMPLETED, "Consultation completed");
        });
        redirectAttributes.addFlashAttribute("successMessage", "Consultation completed and recorded.");
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/prescription/create")
    public String createPrescription(@RequestParam("appointmentId") Long appointmentId,
                                     @RequestParam("diagnosis") String diagnosis,
                                     @RequestParam("instructions") String instructions,
                                     @RequestParam(value = "followUpDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate,
                                     @RequestParam("medicineName") String[] medicineNames,
                                     @RequestParam("dosage") String[] dosages,
                                     @RequestParam("frequency") String[] frequencies,
                                     @RequestParam("duration") String[] durations,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        Appointment app = appointmentService.findById(appointmentId).orElse(null);
        if (app != null) {
            List<PrescriptionItem> items = new ArrayList<>();
            for (int i = 0; i < medicineNames.length; i++) {
                if (medicineNames[i] != null && !medicineNames[i].trim().isEmpty()) {
                    items.add(new PrescriptionItem(medicineNames[i], dosages[i], frequencies[i], durations[i], "Take after food"));
                }
            }
            prescriptionService.createPrescription(app, doctor, app.getPatient(), diagnosis, instructions, followUpDate, items);
            appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.COMPLETED, "Prescription issued");
            redirectAttributes.addFlashAttribute("successMessage", "Digital Prescription created and sent to Patient!");
        }
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/lab-request/create")
    public String createLabRequest(@RequestParam("patientId") Long patientId,
                                   @RequestParam("testName") String testName,
                                   @RequestParam("notes") String notes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        User patient = userService.findById(patientId).orElse(null);
        if (patient != null) {
            labWorkflowService.requestLabTest(doctor, patient, testName, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Diagnostic Lab Test (" + testName + ") requested for patient!");
        }
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String viewAppointments(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        model.addAttribute("appointments", appointmentService.getDoctorAppointments(doctor));
        return "doctor/appointments";
    }

    @PostMapping("/appointment/{id}/accept")
    public String acceptAppointment(@PathVariable("id") Long id,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        try {
            Appointment updated = appointmentService.updateAppointmentStatus(id, AppointmentStatus.CONFIRMED, notes);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Appointment confirmed! Automated email confirmation sent to " + updated.getPatient().getFullName() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/appointment/{id}/reject")
    public String rejectAppointment(@PathVariable("id") Long id,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        try {
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.REJECTED, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }

    @PostMapping("/appointment/{id}/complete")
    public String completeAppointment(@PathVariable("id") Long id,
                                      @RequestParam(value = "notes", required = false) String notes,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        try {
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.COMPLETED, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment marked as Completed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        DoctorProfile profile = userService.getDoctorProfile(doctor).orElse(new DoctorProfile(doctor));
        model.addAttribute("doctor", doctor);
        model.addAttribute("profile", profile);
        return "doctor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute DoctorProfile profile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        userService.updateDoctorProfile(doctor.getId(), profile);
        redirectAttributes.addFlashAttribute("successMessage", "Doctor profile & availability updated successfully!");
        return "redirect:/doctor/profile";
    }
}
