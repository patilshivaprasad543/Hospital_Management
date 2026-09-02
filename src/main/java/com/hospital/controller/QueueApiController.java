package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.service.QueueService;
import com.hospital.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class QueueApiController {

    @Autowired
    private QueueService queueService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/api/queue/check-in/{appointmentId}")
    public ResponseEntity<?> checkIn(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User patient = (User) session.getAttribute("loggedInUser");
        if (patient == null || patient.getRole() != Role.PATIENT) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            QueueEntry entry = queueService.checkInPatient(appointmentId, patient);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "queueNumber", entry.getQueueNumber(),
                    "sequenceNumber", entry.getSequenceNumber(),
                    "status", entry.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/queue/my-position/{appointmentId}")
    public ResponseEntity<?> getPositionInfo(@PathVariable("appointmentId") Long appointmentId, HttpSession session) {
        User patient = (User) session.getAttribute("loggedInUser");
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (patient == null || (!patient.getId().equals(appointment.getPatient().getId()) && patient.getRole() != Role.ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        Map<String, Object> info = queueService.getPatientPositionInfo(appointment);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/api/doctor/queue/today")
    public ResponseEntity<?> getTodayQueue(HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<QueueEntry> queue = queueService.getTodayDoctorQueue(doctor);
        return ResponseEntity.ok(queue);
    }

    @PostMapping("/api/doctor/queue/call-next")
    public ResponseEntity<?> callNext(HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            QueueEntry entry = queueService.callNextPatient(doctor);
            return ResponseEntity.ok(Map.of("success", true, "calledQueueNumber", entry.getQueueNumber(), "patientName", entry.getPatient().getFullName()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/doctor/queue/{queueId}/recall")
    public ResponseEntity<?> recall(@PathVariable("queueId") Long queueId, HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.recallPatient(queueId, doctor);
        return ResponseEntity.ok(Map.of("success", true, "recalledQueueNumber", entry.getQueueNumber(), "recallCount", entry.getRecallCount()));
    }

    @PostMapping("/api/doctor/queue/{queueId}/start")
    public ResponseEntity<?> startConsultation(@PathVariable("queueId") Long queueId, HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.startConsultation(queueId, doctor);
        return ResponseEntity.ok(Map.of("success", true, "status", entry.getStatus().name()));
    }

    @PostMapping("/api/doctor/queue/{queueId}/complete")
    public ResponseEntity<?> completeConsultation(@PathVariable("queueId") Long queueId, HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.completeConsultation(queueId, doctor);
        return ResponseEntity.ok(Map.of("success", true, "status", entry.getStatus().name()));
    }

    @PostMapping("/api/doctor/queue/{queueId}/skip")
    public ResponseEntity<?> skip(@PathVariable("queueId") Long queueId,
                                 @RequestParam(value = "reason", required = false) String reason,
                                 HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.skipPatient(queueId, doctor, reason);
        return ResponseEntity.ok(Map.of("success", true, "status", entry.getStatus().name()));
    }

    @PostMapping("/api/doctor/queue/{queueId}/hold")
    public ResponseEntity<?> hold(@PathVariable("queueId") Long queueId,
                                 @RequestParam(value = "reason", required = false) String reason,
                                 HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.holdPatient(queueId, doctor, reason);
        return ResponseEntity.ok(Map.of("success", true, "status", entry.getStatus().name()));
    }

    @PostMapping("/api/doctor/queue/{queueId}/resume")
    public ResponseEntity<?> resume(@PathVariable("queueId") Long queueId, HttpSession session) {
        User doctor = (User) session.getAttribute("loggedInUser");
        if (doctor == null || doctor.getRole() != Role.DOCTOR) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        QueueEntry entry = queueService.resumePatient(queueId, doctor);
        return ResponseEntity.ok(Map.of("success", true, "status", entry.getStatus().name()));
    }

    @GetMapping("/api/queue/display/{doctorId}")
    public ResponseEntity<?> getPublicDisplay(@PathVariable("doctorId") Long doctorId) {
        User doctor = userService.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        List<QueueEntry> todayQueue = queueService.getTodayDoctorQueue(doctor);
        Optional<QueueEntry> nowServing = todayQueue.stream().filter(q -> q.getStatus() == QueueStatus.IN_PROGRESS || q.getStatus() == QueueStatus.CALLED).findFirst();
        List<String> nextQueueNumbers = todayQueue.stream().filter(q -> q.getStatus() == QueueStatus.WAITING).map(QueueEntry::getQueueNumber).limit(5).toList();

        Map<String, Object> display = new HashMap<>();
        display.put("doctorName", doctor.getFullName());
        display.put("department", "Cardiology & General Medicine");
        display.put("nowServing", nowServing.map(QueueEntry::getQueueNumber).orElse("---"));
        display.put("nextQueueNumbers", nextQueueNumbers);

        return ResponseEntity.ok(display);
    }
}
