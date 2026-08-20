package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private AnnouncementService announcementService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PdfService pdfService;

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
        List<Appointment> prescribableAppointments = appointmentService.getDoctorAppointments(doctor).stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getState() == AppointmentState.CHECKED_IN
                        || a.getState() == AppointmentState.IN_CONSULTATION)
                .toList();
        DoctorProfile profile = userService.getDoctorProfile(doctor).orElse(new DoctorProfile(doctor));

        // Filter checked-in patients for live queue
        List<Appointment> checkedInQueue = appointmentService.getDoctorAppointments(doctor).stream()
                .filter(a -> a.getState() == AppointmentState.CHECKED_IN || a.getState() == AppointmentState.IN_CONSULTATION)
                .toList();

        model.addAttribute("doctor", doctor);
        model.addAttribute("profile", profile);
        model.addAttribute("pendingAppointments", pendingAppointments);
        model.addAttribute("confirmedAppointments", confirmedAppointments);
        model.addAttribute("prescribableAppointments", prescribableAppointments);
        model.addAttribute("checkedInQueue", checkedInQueue);
        model.addAttribute("prescriptions", prescriptionService.getDoctorPrescriptions(doctor));
        model.addAttribute("announcements", announcementService.getActiveForRole("DOCTOR"));

        return "doctor/dashboard";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) {
            return "redirect:/login/doctor";
        }
        model.addAttribute("notifications", notificationService.getNotificationsForUser(doctor));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(doctor));
        model.addAttribute("loggedInUser", doctor);
        return "doctor/notifications";
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
                                       @RequestParam(required = false) String observations,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate followUpDate,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        consultationService.findByAppointment(id).ifPresent(c -> {
            consultationService.completeConsultation(c.getId(), symptoms, diagnosis, treatment, notes, observations, followUpDate, doctor);
            appointmentService.updateAppointmentStatus(id, AppointmentStatus.COMPLETED, "Consultation completed");
        });
        redirectAttributes.addFlashAttribute("successMessage",
                "Consultation completed. Issue a digital prescription below so the patient can order medicines.");
        return "redirect:/doctor/consultation/" + id;
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
        if (app == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Appointment not found.");
            return "redirect:/doctor/dashboard";
        }
        List<PrescriptionItem> items = new ArrayList<>();
            for (int i = 0; i < medicineNames.length; i++) {
                if (medicineNames[i] != null && !medicineNames[i].trim().isEmpty()) {
                    items.add(new PrescriptionItem(medicineNames[i], dosages[i], frequencies[i], durations[i], "Take after food"));
                }
            }
            if (items.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Add at least one medicine to the prescription.");
                return "redirect:/doctor/consultation/" + appointmentId;
            }
            prescriptionService.createPrescription(app, doctor, app.getPatient(), diagnosis, instructions, followUpDate, items);
            appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.COMPLETED, "Prescription issued");
            redirectAttributes.addFlashAttribute("successMessage",
                    "Digital prescription sent to " + app.getPatient().getFullName()
                            + ". The patient can order medicines from the Prescriptions page.");
        return "redirect:/doctor/consultation/" + appointmentId;
    }

    @PostMapping("/lab-request/create")
    public String createLabRequest(@RequestParam("patientId") Long patientId,
                                   @RequestParam("testName") String testName,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   @RequestParam(value = "appointmentId", required = false) Long appointmentId,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        User patient = userService.findById(patientId).orElse(null);
        if (patient != null) {
            labWorkflowService.requestLabTest(doctor, patient, testName, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Diagnostic Lab Test (" + testName + ") requested for patient!");
        }
        if (appointmentId != null) {
            return "redirect:/doctor/consultation/" + appointmentId;
        }
        return "redirect:/doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String viewAppointments(@RequestParam(value = "status", required = false) String status,
                                   @RequestParam(value = "patient", required = false) String patient,
                                   @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login";

        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor);
        if (status != null && !status.isBlank()) {
            appointments = appointments.stream()
                    .filter(a -> a.getStatus().name().equalsIgnoreCase(status))
                    .toList();
        }
        if (patient != null && !patient.isBlank()) {
            String q = patient.toLowerCase();
            appointments = appointments.stream()
                    .filter(a -> a.getPatient().getFullName().toLowerCase().contains(q))
                    .toList();
        }
        if (date != null) {
            appointments = appointments.stream()
                    .filter(a -> date.equals(a.getAppointmentDate()))
                    .toList();
        }
        model.addAttribute("appointments", appointments);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterPatient", patient);
        model.addAttribute("filterDate", date);
        model.addAttribute("minDate", LocalDate.now().toString());
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
        model.addAttribute("upcomingLeaves", doctorScheduleService.getUpcomingLeaves(doctor));
        model.addAttribute("averageRating", feedbackService.getDoctorAverageRating(doctor));
        model.addAttribute("ratingCount", feedbackService.getDoctorRatingCount(doctor));
        model.addAttribute("departments", departmentService.getActiveDepartments());
        return "doctor/profile";
    }

    @PostMapping("/leave/add")
    public String addLeave(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveDate,
                           @RequestParam(required = false) String reason,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        try {
            doctorScheduleService.addLeave(doctor, leaveDate, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Leave marked for " + leaveDate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/profile";
    }

    @PostMapping("/leave/{id}/delete")
    public String deleteLeave(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        try {
            doctorScheduleService.removeLeave(id, doctor);
            redirectAttributes.addFlashAttribute("successMessage", "Leave removed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/profile";
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

    @PostMapping("/appointment/{id}/reschedule")
    public String rescheduleAppointment(@PathVariable Long id,
                                        @RequestParam("appointmentDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appointmentDate,
                                        @RequestParam("appointmentTime") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.TIME) java.time.LocalTime appointmentTime,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        try {
            appointmentService.rescheduleByDoctor(id, doctor, appointmentDate, appointmentTime);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment rescheduled. Patient has been notified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/doctor/appointments";
    }

    @GetMapping("/patients")
    public String patients(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        Map<Long, User> unique = new LinkedHashMap<>();
        appointmentService.getDoctorAppointments(doctor)
                .forEach(a -> unique.putIfAbsent(a.getPatient().getId(), a.getPatient()));
        model.addAttribute("patients", unique.values());
        return "doctor/patients";
    }

    @GetMapping("/patients/{id}/records")
    public String patientRecords(@PathVariable Long id, HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        User patient = userService.findById(id).orElse(null);
        if (patient == null) return "redirect:/doctor/patients";
        boolean assigned = appointmentService.getDoctorAppointments(doctor).stream()
                .anyMatch(a -> a.getPatient().getId().equals(patient.getId()));
        if (!assigned) {
            return "redirect:/doctor/patients";
        }
        model.addAttribute("patient", patient);
        model.addAttribute("patientProfile", userService.getPatientProfile(patient).orElse(new PatientProfile(patient)));
        model.addAttribute("appointments", appointmentService.getPatientAppointments(patient).stream()
                .filter(a -> a.getDoctor().getId().equals(doctor.getId()))
                .toList());
        model.addAttribute("consultations", consultationService.getPatientConsultations(patient).stream()
                .filter(c -> c.getDoctor().getId().equals(doctor.getId()))
                .toList());
        model.addAttribute("prescriptions", prescriptionService.getPatientPrescriptions(patient));
        model.addAttribute("labRequests", labWorkflowService.getPatientLabRequests(patient));
        return "doctor/patient-records";
    }

    @GetMapping("/prescriptions")
    public String prescriptions(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        model.addAttribute("prescriptions", prescriptionService.getDoctorPrescriptions(doctor));
        return "doctor/prescriptions";
    }

    @GetMapping("/lab-tests")
    public String labTests(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        model.addAttribute("labRequests", labWorkflowService.getDoctorLabRequests(doctor));
        return "doctor/lab-tests";
    }

    @GetMapping("/earnings")
    public String earnings(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor);
        model.addAttribute("earnings", billingService.getDoctorEarnings(doctor, appointments));
        model.addAttribute("profile", userService.getDoctorProfile(doctor).orElse(new DoctorProfile(doctor)));
        return "doctor/earnings";
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return "redirect:/login/doctor";
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor);
        model.addAttribute("appointments", appointments);
        model.addAttribute("earnings", billingService.getDoctorEarnings(doctor, appointments));
        model.addAttribute("pendingCount", appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).count());
        model.addAttribute("acceptedCount", appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count());
        model.addAttribute("completedCount", appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count());
        model.addAttribute("cancelledCount", appointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.REJECTED).count());
        return "doctor/reports";
    }

    @GetMapping("/prescription/{id}/pdf")
    public ResponseEntity<byte[]> downloadPrescriptionPdf(@PathVariable Long id, HttpSession session) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return ResponseEntity.status(401).build();
        return prescriptionService.findByIdForDoctor(id, doctor)
                .map(rx -> pdfService.download(pdfService.generatePrescriptionPdf(rx, rx.getDoctor(), rx.getPatient()),
                        "prescription-" + rx.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/lab-report/{id}/pdf")
    public ResponseEntity<byte[]> downloadLabReportPdf(@PathVariable Long id, HttpSession session) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return ResponseEntity.status(401).build();
        return labWorkflowService.findByIdForDoctor(id, doctor)
                .filter(lab -> lab.getReportResult() != null)
                .map(lab -> pdfService.download(pdfService.generateLabReportPdf(lab, lab.getPatient()),
                        "lab-report-" + lab.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/consultation/{id}/pdf")
    public ResponseEntity<byte[]> downloadConsultationPdf(@PathVariable Long id, HttpSession session) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return ResponseEntity.status(401).build();
        return consultationService.findByAppointment(id)
                .filter(c -> c.getDoctor() != null && c.getDoctor().getId().equals(doctor.getId()))
                .map(c -> pdfService.download(pdfService.generateConsultationPdf(c),
                        "consultation-" + c.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/consultations/{id}/pdf")
    public ResponseEntity<byte[]> downloadConsultationRecordPdf(@PathVariable Long id, HttpSession session) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return ResponseEntity.status(401).build();
        return consultationService.findById(id)
                .filter(c -> c.getDoctor() != null && c.getDoctor().getId().equals(doctor.getId()))
                .map(c -> pdfService.download(pdfService.generateConsultationPdf(c),
                        "consultation-" + c.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/appointment/{id}/pdf")
    public ResponseEntity<byte[]> downloadAppointmentPdf(@PathVariable Long id, HttpSession session) {
        User doctor = getLoggedInDoctor(session);
        if (doctor == null) return ResponseEntity.status(401).build();
        return appointmentService.findById(id)
                .filter(app -> app.getDoctor() != null && app.getDoctor().getId().equals(doctor.getId()))
                .map(app -> pdfService.download(pdfService.generateAppointmentPdf(app),
                        "appointment-" + app.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }
}
