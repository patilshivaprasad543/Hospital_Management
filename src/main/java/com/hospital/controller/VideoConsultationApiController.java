package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.service.VideoConsultationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-consultations")
public class VideoConsultationApiController {

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @GetMapping("/{appointmentId}")
    public ResponseEntity<?> getConsultationDetails(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        videoConsultationService.validateAccess(user, appointment);
        VideoConsultation videoRoom = videoConsultationService.getByAppointment(appointment);

        Map<String, Object> response = new HashMap<>();
        response.put("appointmentId", appointment.getId());
        response.put("consultationType", appointment.getConsultationType());
        response.put("doctorName", appointment.getDoctor().getFullName());
        response.put("patientName", appointment.getPatient().getFullName());
        response.put("date", appointment.getAppointmentDate().toString());
        response.put("time", appointment.getAppointmentTime().toString());
        response.put("status", appointment.getStatus());
        response.put("videoStatus", appointment.getVideoStatus());
        response.put("roomId", appointment.getVideoRoomId());
        response.put("availableFrom", appointment.getVideoJoinAvailableFrom());
        response.put("expiresAt", appointment.getVideoJoinExpiresAt());
        response.put("isJoinable", isJoinable(appointment));

        if (videoRoom != null) {
            response.put("patientJoined", videoRoom.isPatientJoined());
            response.put("doctorJoined", videoRoom.isDoctorJoined());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{appointmentId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        videoConsultationService.validateAccess(user, appointment);
        VideoConsultation room = videoConsultationService.getByAppointment(appointment);

        if (room == null) {
            room = videoConsultationService.createVideoRoom(appointment);
        }

        if (user.getRole() == Role.PATIENT) {
            room.setPatientJoined(true);
            room.setPatientJoinedAt(LocalDateTime.now());
        } else if (user.getRole() == Role.DOCTOR) {
            room.setDoctorJoined(true);
            room.setDoctorJoinedAt(LocalDateTime.now());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("roomId", room.getRoomId());
        res.put("role", user.getRole().name());
        res.put("userId", user.getId());
        res.put("userName", user.getFullName());

        return ResponseEntity.ok(res);
    }

    @PostMapping("/{appointmentId}/start")
    public ResponseEntity<?> startCall(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(403).body(Map.of("error", "Only doctors can start a consultation."));
        }

        VideoConsultation room = videoConsultationService.startConsultation(appointmentId, user);
        return ResponseEntity.ok(Map.of("success", true, "status", room.getStatus()));
    }

    @PostMapping("/{appointmentId}/end")
    public ResponseEntity<?> endCall(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        VideoConsultation room = videoConsultationService.endConsultation(appointmentId, user);
        return ResponseEntity.ok(Map.of("success", true, "status", room.getStatus()));
    }

    @GetMapping("/{appointmentId}/chat")
    public ResponseEntity<?> getChatMessages(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        videoConsultationService.validateAccess(user, appointment);
        List<VideoChatMessage> messages = videoConsultationService.getChatHistory(appointment.getVideoRoomId());
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{appointmentId}/chat")
    public ResponseEntity<?> postChatMessage(@PathVariable("appointmentId") Long appointmentId,
                                            @RequestParam("message") String messageText,
                                            HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        videoConsultationService.validateAccess(user, appointment);
        VideoChatMessage msg = videoConsultationService.saveChatMessage(appointment.getVideoRoomId(), user, messageText);

        return ResponseEntity.ok(Map.of("success", true, "id", msg.getId(), "sentAt", msg.getSentAt()));
    }

    private boolean isJoinable(Appointment appointment) {
        if (appointment.getConsultationType() != ConsultationType.VIDEO) return false;
        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.REJECTED) return false;

        LocalDateTime now = LocalDateTime.now();
        boolean afterFrom = appointment.getVideoJoinAvailableFrom() == null || !now.isBefore(appointment.getVideoJoinAvailableFrom());
        boolean beforeExpires = appointment.getVideoJoinExpiresAt() == null || !now.isAfter(appointment.getVideoJoinExpiresAt());

        return afterFrom && (beforeExpires || appointment.getStatus() == AppointmentStatus.IN_PROGRESS);
    }
}
