package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.LabRequestRepository;
import com.hospital.service.AppointmentService;
import com.hospital.service.UserService;
import com.hospital.service.UserSessionHelper;
import com.hospital.service.VideoConsultationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PatientMultiHubChatController {

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private LabRequestRepository labRequestRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private com.hospital.repository.DoctorProfileRepository doctorProfileRepository;

    private User getLoggedInPatient(HttpSession session) {
        return UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping("/api/chat/video-room")
    @ResponseBody
    public ResponseEntity<?> getOrCreateVideoRoomForChat(@RequestParam(value = "targetUserId", required = false) Long targetUserId, HttpSession session) {
        User currentUser = UserSessionHelper.getAnyLoggedInUser(session);
        if (currentUser == null) {
            currentUser = (User) session.getAttribute("loggedInUser");
        }
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        User targetUser = (targetUserId != null && targetUserId > 0) ? userService.findById(targetUserId).orElse(null) : null;
        if (targetUser == null) {
            List<User> docs = userService.findApprovedDoctors();
            if (!docs.isEmpty()) {
                targetUser = docs.get(0);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Target contact not specified and no doctors available."));
            }
        }

        User patient = (currentUser.getRole() == Role.PATIENT) ? currentUser : targetUser;
        User doctor = (currentUser.getRole() == Role.DOCTOR) ? currentUser : ((targetUser.getRole() == Role.DOCTOR) ? targetUser : null);

        if (doctor == null) {
            List<User> docs = userService.findApprovedDoctors();
            if (!docs.isEmpty()) {
                doctor = docs.get(0);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "No available doctors for video call"));
            }
        }

        if (doctorProfileRepository.findByUser(doctor).isEmpty()) {
            doctorProfileRepository.save(new DoctorProfile(doctor));
        }

        try {
            List<Appointment> appts = appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
            Appointment appt = null;
            for (Appointment a : appts) {
                if (a.getDoctor() != null && a.getDoctor().getId().equals(doctor.getId())
                        && a.getConsultationType() == ConsultationType.VIDEO
                        && a.getStatus() != AppointmentStatus.COMPLETED
                        && a.getStatus() != AppointmentStatus.CANCELLED) {
                    appt = a;
                    break;
                }
            }

            if (appt == null) {
                java.time.LocalTime callTime = java.time.LocalTime.now().withSecond(0).withNano(0);
                try {
                    appt = appointmentService.bookAppointmentWithDepartment(
                            patient.getId(), doctor.getId(), java.time.LocalDate.now(), callTime,
                            "Telemedicine Video Consultation via Chat", "General Medicine", ConsultationType.VIDEO
                    );
                } catch (Exception slotEx) {
                    Appointment directAppt = new Appointment(patient, doctor, java.time.LocalDate.now(),
                            callTime.plusMinutes((long) (Math.random() * 120 + 1)),
                            "Telemedicine Video Consultation via Chat");
                    directAppt.setDepartmentCategory("General Medicine");
                    directAppt.setConsultationType(ConsultationType.VIDEO);
                    directAppt.setStatus(AppointmentStatus.CONFIRMED);
                    directAppt.setState(com.hospital.model.AppointmentState.CONFIRMED);
                    appt = appointmentRepository.save(directAppt);
                }
                appointmentService.updateAppointmentStatus(appt.getId(), AppointmentStatus.CONFIRMED, "Accepted for chat video consultation");
            }

            if (appt.getStatus() == AppointmentStatus.PENDING) {
                appointmentService.updateAppointmentStatus(appt.getId(), AppointmentStatus.CONFIRMED, "Accepted for chat video consultation");
            }

            VideoConsultation videoRoom = videoConsultationService.getByAppointment(appt);
            if (videoRoom == null) {
                videoRoom = videoConsultationService.createVideoRoom(appt);
            }

            String roomUrl = "/video-consultation/" + appt.getId();
            String waitingRoomUrl = "/video-consultation/" + appt.getId() + "/waiting-room";
            String directRoomUrl = (currentUser.getRole() == Role.PATIENT)
                    ? "/patient/video-consultation/" + appt.getId()
                    : "/doctor/video-consultation/" + appt.getId();

            Map<String, Object> resp = new HashMap<>();
            resp.put("appointmentId", appt.getId());
            resp.put("roomId", videoRoom.getRoomId());
            resp.put("roomUrl", roomUrl);
            resp.put("directRoomUrl", directRoomUrl);
            resp.put("waitingRoomUrl", waitingRoomUrl);
            resp.put("doctorName", doctor.getFullName());
            resp.put("patientName", patient.getFullName());
            resp.put("userRole", currentUser.getRole().name());

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to create video room"));
        }
    }

    @GetMapping({"/patient/multihub-chat", "/patient/chat"})
    public String patientMultiHubChat(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        // 1. Consulted Doctors for this patient
        List<Appointment> patientAppts = appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
        List<User> consultedDoctors = patientAppts.stream()
                .map(Appointment::getDoctor)
                .filter(d -> d != null && d.getRole() == Role.DOCTOR)
                .distinct()
                .collect(Collectors.toList());

        if (consultedDoctors.isEmpty()) {
            consultedDoctors = userService.findApprovedDoctors();
        }

        // 2. Consulted Labs for this patient
        List<LabRequest> labReqs = labRequestRepository.findByPatientOrderByCreatedAtDesc(patient);
        List<User> consultedLabs = labReqs.stream()
                .map(LabRequest::getLabVendor)
                .filter(l -> l != null)
                .distinct()
                .collect(Collectors.toList());

        // Always include Support & Central Pathology Lab Desk
        List<User> supportContacts = getAdminAndSupportContacts();

        Set<User> contacts = new LinkedHashSet<>();
        contacts.addAll(supportContacts);
        contacts.addAll(consultedLabs);
        contacts.addAll(consultedDoctors);

        model.addAttribute("contacts", new ArrayList<>(contacts));
        model.addAttribute("contactRoleLabel", "🩺 Consulted Doctors, 🧪 Labs & Support Desk");
        model.addAttribute("loggedInUser", patient);
        return "patient/multihub-chat";
    }

    @GetMapping({"/doctor/multihub-chat", "/doctor/chat"})
    public String doctorMultiHubChat(HttpSession session, Model model) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return "redirect:/login";
        }

        // 1. Consulted Patients for this doctor
        List<Appointment> doctorAppts = appointmentRepository.findByDoctorOrderByCreatedAtDesc(doctor);
        List<User> consultedPatients = doctorAppts.stream()
                .map(Appointment::getPatient)
                .filter(p -> p != null && p.getRole() == Role.PATIENT)
                .distinct()
                .collect(Collectors.toList());

        if (consultedPatients.isEmpty()) {
            consultedPatients = userService.findAllUsers().stream()
                    .filter(u -> u.getRole() == Role.PATIENT)
                    .collect(Collectors.toList());
        }

        // 2. Consulted Labs for doctor's lab requests
        List<LabRequest> docLabReqs = labRequestRepository.findByDoctorOrderByCreatedAtDesc(doctor);
        List<User> consultedLabs = docLabReqs.stream()
                .map(LabRequest::getLabVendor)
                .filter(l -> l != null)
                .distinct()
                .collect(Collectors.toList());

        List<User> supportContacts = getAdminAndSupportContacts();

        Set<User> contacts = new LinkedHashSet<>();
        contacts.addAll(supportContacts);
        contacts.addAll(consultedLabs);
        contacts.addAll(consultedPatients);

        model.addAttribute("contacts", new ArrayList<>(contacts));
        model.addAttribute("contactRoleLabel", "🧑‍🦽 Consulted Patients, 🧪 Labs & Support Desk");
        model.addAttribute("loggedInUser", doctor);
        return "patient/multihub-chat";
    }

    @GetMapping({"/admin/multihub-chat", "/admin/chat", "/vendor/multihub-chat", "/vendor/chat", "/pharmacy/multihub-chat", "/pharmacy/chat"})
    public String portalMultiHubChat(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<User> allUsers = userService.findAllUsers().stream()
                .filter(u -> !u.getId().equals(user.getId()))
                .collect(Collectors.toList());

        // Ensure Central Pathology Lab Desk is included
        User labDesk = getLabDeskUser();
        if (allUsers.stream().noneMatch(u -> u.getEmail() != null && u.getEmail().equals(labDesk.getEmail()))) {
            allUsers.add(0, labDesk);
        }

        model.addAttribute("contacts", allUsers);
        model.addAttribute("contactRoleLabel", "💬 Hospital Network, 🧪 Labs & Support Contacts");
        model.addAttribute("loggedInUser", user);
        return "patient/multihub-chat";
    }

    private List<User> getAdminAndSupportContacts() {
        List<User> support = userService.findAllUsers().stream()
                .filter(u -> u.getRole() == Role.ADMIN || u.getRole() == Role.VENDOR)
                .collect(Collectors.toList());

        User labDesk = getLabDeskUser();
        if (support.stream().noneMatch(u -> u.getEmail() != null && u.getEmail().equals(labDesk.getEmail()))) {
            support.add(labDesk);
        }
        return support;
    }

    private User getLabDeskUser() {
        User lab = new User("Central Pathology & Diagnostic Lab", "lab@smartcare360.com", "1800-LAB-TEST", "lab123", Role.VENDOR);
        lab.setId(9999L);
        return lab;
    }
}
