package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.service.QueueService;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class QueueViewController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private UserService userService;

    @GetMapping({"/doctor/queue", "/doctor/live-queue"})
    public String doctorLiveQueue(HttpSession session, Model model) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return "redirect:/login/doctor";
        }

        List<QueueEntry> queueList = queueService.getTodayDoctorQueue(doctor);
        model.addAttribute("queueList", queueList != null ? queueList : List.of());
        model.addAttribute("loggedInUser", doctor);
        return "doctor/live-queue";
    }

    @GetMapping("/patient/queue")
    public String patientGeneralQueue(HttpSession session, Model model) {
        User patient = (User) session.getAttribute("loggedInUser");
        if (patient == null || patient.getRole() != Role.PATIENT) {
            return "redirect:/login/patient";
        }

        List<Appointment> todayAppointments = appointmentRepository.findByPatientAndAppointmentDate(patient, LocalDate.now());
        if (todayAppointments.isEmpty()) {
            List<Appointment> allAppointments = appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
            if (!allAppointments.isEmpty()) {
                todayAppointments = List.of(allAppointments.get(0));
            }
        }

        if (!todayAppointments.isEmpty()) {
            Appointment appointment = todayAppointments.get(0);
            return patientLiveQueue(appointment.getId(), session, model);
        }

        Map<String, Object> emptyQueueInfo = new HashMap<>();
        emptyQueueInfo.put("checkedIn", false);
        emptyQueueInfo.put("queueNumber", "---");
        emptyQueueInfo.put("status", "NO APPOINTMENT TODAY");
        emptyQueueInfo.put("currentlyServing", "---");
        emptyQueueInfo.put("patientsAhead", 0);
        emptyQueueInfo.put("estimatedWaitMinutes", 0);
        emptyQueueInfo.put("doctorName", "---");
        emptyQueueInfo.put("department", "General");

        model.addAttribute("appointment", new Appointment());
        model.addAttribute("queueInfo", emptyQueueInfo);
        model.addAttribute("loggedInUser", patient);
        return "patient/live-queue";
    }

    @GetMapping("/patient/queue/{appointmentId}")
    public String patientLiveQueue(@PathVariable("appointmentId") Long appointmentId, HttpSession session, Model model) {
        User patient = (User) session.getAttribute("loggedInUser");
        if (patient == null || patient.getRole() != Role.PATIENT) {
            return "redirect:/login/patient";
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElse(null);

        if (appointment == null || (appointment.getPatient() != null && !appointment.getPatient().getId().equals(patient.getId()))) {
            return "redirect:/patient/appointments";
        }

        Map<String, Object> queueInfo = queueService.getPatientPositionInfo(appointment);
        model.addAttribute("appointment", appointment);
        model.addAttribute("queueInfo", queueInfo);
        model.addAttribute("loggedInUser", patient);
        return "patient/live-queue";
    }

    @GetMapping({"/admin/queue", "/admin/queue/live"})
    public String adminLiveQueue(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("loggedInUser");
        if (admin == null || admin.getRole() != Role.ADMIN) {
            return "redirect:/login";
        }

        List<User> doctors = userService.findApprovedDoctors();
        model.addAttribute("doctors", doctors);
        if (!doctors.isEmpty()) {
            model.addAttribute("doctor", doctors.get(0));
        } else {
            model.addAttribute("doctor", new User());
        }
        model.addAttribute("loggedInUser", admin);
        return "queue/public-display";
    }

    @GetMapping("/queue/display/{doctorId}")
    public String publicHospitalDisplay(@PathVariable("doctorId") Long doctorId, Model model) {
        User doctor = userService.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        model.addAttribute("doctor", doctor);
        return "queue/public-display";
    }
}
