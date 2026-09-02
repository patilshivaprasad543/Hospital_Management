package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorProfileRepository;
import com.hospital.repository.PatientProfileRepository;
import com.hospital.service.ConsultationService;
import com.hospital.service.VideoConsultationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class VideoConsultationViewController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private com.hospital.service.AppointmentService appointmentService;

    @GetMapping({"/patient/video-consultations", "/patient/video-consultation", "/patient/video-consultation/"})
    public String patientVideoList(HttpSession session, Model model) {
        User patient = com.hospital.service.UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) {
            patient = (User) session.getAttribute("loggedInUser");
        }
        if (patient == null || patient.getRole() != Role.PATIENT) {
            return "redirect:/login";
        }

        List<VideoConsultation> videoList = videoConsultationService.getPatientConsultations(patient);
        model.addAttribute("videoConsultations", videoList);
        model.addAttribute("loggedInUser", patient);
        return "patient/video-list";
    }

    @GetMapping("/patient/video-consultation/{appointmentId}/waiting-room")
    public String patientWaitingRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN) {
            return "redirect:/doctor/video-consultation/" + appointmentId + "/waiting-room";
        }

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment #" + appointmentId + " not found.");
                return "redirect:/patient/video-consultations";
            }

            if (appointment.getStatus() == AppointmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Waiting room opens after doctor accepts your video consultation request.");
                return "redirect:/patient/video-consultations";
            }

            videoConsultationService.validateAccess(user, appointment);
            VideoConsultation videoRoom = videoConsultationService.getByAppointment(appointment);
            if (videoRoom == null) {
                videoRoom = videoConsultationService.createVideoRoom(appointment);
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("videoRoom", videoRoom);
            model.addAttribute("loggedInUser", user);
            model.addAttribute("role", "PATIENT");
            return "video/waiting-room";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/video-consultations";
        }
    }

    @GetMapping("/patient/video-consultation/{appointmentId}")
    public String patientVideoRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN) {
            return "redirect:/doctor/video-consultation/" + appointmentId;
        }

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment #" + appointmentId + " not found.");
                return "redirect:/patient/video-consultations";
            }

            if (appointment.getStatus() == AppointmentStatus.PENDING) {
                redirectAttributes.addFlashAttribute("errorMessage", "Waiting room opens after doctor accepts your video consultation request.");
                return "redirect:/patient/video-consultations";
            }

            videoConsultationService.validateAccess(user, appointment);
            VideoConsultation videoRoom = videoConsultationService.getByAppointment(appointment);
            if (videoRoom == null) {
                videoRoom = videoConsultationService.createVideoRoom(appointment);
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("videoRoom", videoRoom);
            model.addAttribute("loggedInUser", user);
            model.addAttribute("role", "PATIENT");
            return "video/patient-room";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/patient/video-consultations";
        }
    }

    @GetMapping({"/doctor/video-consultations", "/doctor/video-consultation"})
    public String doctorVideoList(HttpSession session, Model model) {
        User doctor = com.hospital.service.UserSessionHelper.getLoggedInDoctor(session);
        if (doctor == null) {
            doctor = (User) session.getAttribute("loggedInUser");
        }
        if (doctor == null || (doctor.getRole() != Role.DOCTOR && doctor.getRole() != Role.ADMIN)) {
            return "redirect:/login";
        }

        List<VideoConsultation> videoList = videoConsultationService.getDoctorConsultations(doctor);
        model.addAttribute("videoConsultations", videoList);
        model.addAttribute("loggedInUser", doctor);
        return "doctor/video-list";
    }

    @PostMapping("/doctor/video-consultation/{appointmentId}/accept")
    public String doctorAcceptVideoConsultation(@PathVariable("appointmentId") Long appointmentId,
                                                HttpSession session,
                                                RedirectAttributes redirectAttributes) {
        User doctor = com.hospital.service.UserSessionHelper.getLoggedInDoctor(session);
        if (doctor == null) {
            doctor = (User) session.getAttribute("loggedInUser");
        }
        if (doctor == null || (doctor.getRole() != Role.DOCTOR && doctor.getRole() != Role.ADMIN)) {
            return "redirect:/login";
        }

        Appointment appt = appointmentRepository.findById(appointmentId).orElse(null);
        if (appt != null && appt.getDoctor().getId().equals(doctor.getId())) {
            appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.CONFIRMED, "Video consultation accepted by doctor");
            videoConsultationService.createVideoRoom(appt);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Video consultation accepted & confirmed! Waiting room is now open.");
        }
        return "redirect:/doctor/video-consultations";
    }

    @GetMapping("/doctor/video-consultation/{appointmentId}/waiting-room")
    public String doctorWaitingRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.PATIENT) {
            return "redirect:/patient/video-consultation/" + appointmentId + "/waiting-room";
        }

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment #" + appointmentId + " not found.");
                return "redirect:/doctor/video-consultations";
            }

            if (appointment.getStatus() == AppointmentStatus.PENDING) {
                appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.CONFIRMED, "Video consultation accepted by doctor");
            }

            videoConsultationService.validateAccess(user, appointment);
            VideoConsultation videoRoom = videoConsultationService.getByAppointment(appointment);
            if (videoRoom == null) {
                videoRoom = videoConsultationService.createVideoRoom(appointment);
            }

            model.addAttribute("appointment", appointment);
            model.addAttribute("videoRoom", videoRoom);
            model.addAttribute("loggedInUser", user);
            model.addAttribute("role", "DOCTOR");
            return "video/waiting-room";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/doctor/video-consultations";
        }
    }

    @GetMapping("/doctor/video-consultation/{appointmentId}")
    public String doctorVideoRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.PATIENT) {
            return "redirect:/patient/video-consultation/" + appointmentId;
        }

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId).orElse(null);
            if (appointment == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Appointment #" + appointmentId + " not found.");
                return "redirect:/doctor/video-consultations";
            }

            if (appointment.getStatus() == AppointmentStatus.PENDING) {
                appointmentService.updateAppointmentStatus(appointmentId, AppointmentStatus.CONFIRMED, "Video consultation accepted by doctor");
            }

            videoConsultationService.validateAccess(user, appointment);
            VideoConsultation videoRoom = videoConsultationService.getByAppointment(appointment);
            if (videoRoom == null) {
                videoRoom = videoConsultationService.createVideoRoom(appointment);
            }

            Optional<PatientProfile> profile = patientProfileRepository.findByUser(appointment.getPatient());
            profile.ifPresent(p -> model.addAttribute("patientProfile", p));

            model.addAttribute("appointment", appointment);
            model.addAttribute("videoRoom", videoRoom);
            model.addAttribute("loggedInUser", user);
            model.addAttribute("role", "DOCTOR");
            return "video/doctor-room";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/doctor/video-consultations";
        }
    }

    @GetMapping({"/video-consultation/{appointmentId}", "/video/room/{appointmentId}"})
    public String unifiedVideoRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN) {
            return "redirect:/doctor/video-consultation/" + appointmentId;
        } else {
            return "redirect:/patient/video-consultation/" + appointmentId;
        }
    }

    @GetMapping({"/video-consultation/{appointmentId}/waiting-room", "/video/waiting-room/{appointmentId}"})
    public String unifiedWaitingRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (user == null) {
            user = (User) session.getAttribute("loggedInUser");
        }
        if (user == null) {
            return "redirect:/login";
        }

        if (user.getRole() == Role.DOCTOR || user.getRole() == Role.ADMIN) {
            return "redirect:/doctor/video-consultation/" + appointmentId + "/waiting-room";
        } else {
            return "redirect:/patient/video-consultation/" + appointmentId + "/waiting-room";
        }
    }

    @PostMapping({"/doctor/consultation/save", "/consultation/save"})
    public String saveConsultationRecord(@RequestParam("appointmentId") Long appointmentId,
                                         @RequestParam(value = "symptoms", required = false) String symptoms,
                                         @RequestParam("diagnosis") String diagnosis,
                                         @RequestParam(value = "treatmentPlan", required = false) String treatmentPlan,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null) {
            return "redirect:/login";
        }

        Appointment app = appointmentRepository.findById(appointmentId).orElse(null);
        if (app != null) {
            Consultation consultation = consultationService.startConsultation(app, doctor);
            consultationService.completeConsultation(consultation.getId(), symptoms, diagnosis, treatmentPlan, null, null, null, doctor);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Consultation record saved successfully!");
        return "redirect:/doctor/dashboard";
    }
}
