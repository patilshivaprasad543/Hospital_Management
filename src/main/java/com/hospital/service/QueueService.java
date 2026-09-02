package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.QueueEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class QueueService {

    private static final int DEFAULT_AVG_CONSULTATION_MINUTES = 15;
    private static final int CHECKIN_WINDOW_START_MINUTES = 30;
    private static final int CHECKIN_WINDOW_END_MINUTES = 60;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public QueueEntry checkInPatient(Long appointmentId, User patient) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Access denied: You do not own this appointment.");
        }

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED && appointment.getStatus() != AppointmentStatus.ACCEPTED) {
            throw new RuntimeException("Only confirmed appointments can be checked in.");
        }

        LocalDate today = LocalDate.now();
        if (!appointment.getAppointmentDate().equals(today)) {
            throw new RuntimeException("Check-in is only allowed on the scheduled date of your appointment (" + appointment.getAppointmentDate() + ").");
        }

        LocalTime appointmentTime = appointment.getAppointmentTime();
        LocalDateTime appointmentDateTime = LocalDateTime.of(today, appointmentTime);
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(appointmentDateTime.minusMinutes(CHECKIN_WINDOW_START_MINUTES))) {
            throw new RuntimeException("Check-in opens " + CHECKIN_WINDOW_START_MINUTES + " minutes before your appointment time.");
        }

        if (now.isAfter(appointmentDateTime.plusMinutes(CHECKIN_WINDOW_END_MINUTES))) {
            throw new RuntimeException("Check-in window has closed. Please contact hospital reception.");
        }

        Optional<QueueEntry> existing = queueEntryRepository.findByAppointment(appointment);
        if (existing.isPresent()) {
            return existing.get();
        }

        long countToday = queueEntryRepository.countByDoctorAndQueueDate(appointment.getDoctor(), today);
        int seq = (int) countToday + 1;
        String queueNum = "A-" + String.format("%03d", seq);

        QueueEntry entry = new QueueEntry(appointment, appointment.getDoctor(), patient, queueNum, seq, today);
        if (appointment.getDepartmentCategory() != null) {
            entry.setDepartmentName(appointment.getDepartmentCategory());
        }

        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointment.setCheckedInAt(now);
        appointmentRepository.save(appointment);

        QueueEntry saved = queueEntryRepository.save(entry);

        auditLogService.logAction(patient, "PATIENT_CHECKED_IN", "Queue ticket " + queueNum + " generated for Appointment #" + appointmentId);

        notificationService.sendPortalNotification(
                patient,
                "🎟 Checked In — Queue " + queueNum,
                "Your queue number is " + queueNum + ". Please wait in the lounge.",
                NotificationCategory.APPOINTMENT,
                "/patient/queue/" + appointment.getId()
        );

        broadcastQueueUpdate(appointment.getDoctor().getId());

        return saved;
    }

    @Transactional
    public synchronized QueueEntry callNextPatient(User doctor) {
        LocalDate today = LocalDate.now();
        Optional<QueueEntry> nextOpt = queueEntryRepository.findFirstByDoctorAndQueueDateAndStatusInOrderBySequenceNumberAsc(
                doctor, today, List.of(QueueStatus.WAITING, QueueStatus.ON_HOLD));

        if (nextOpt.isEmpty()) {
            throw new RuntimeException("No patients currently waiting in queue.");
        }

        QueueEntry next = nextOpt.get();
        next.setStatus(QueueStatus.CALLED);
        next.setCalledTime(LocalDateTime.now());
        next.setUpdatedAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(next);

        auditLogService.logAction(doctor, "DOCTOR_CALLED_PATIENT", "Doctor called Queue #" + saved.getQueueNumber());

        String msgText = (saved.getAppointment() != null && saved.getAppointment().getConsultationType() == ConsultationType.VIDEO)
                ? "Queue " + saved.getQueueNumber() + ": Your video consultation is ready. Please click Join Video Consultation."
                : "Queue " + saved.getQueueNumber() + ": Please proceed to Dr. " + doctor.getFullName() + "'s consultation room.";

        notificationService.sendPortalNotification(
                saved.getPatient(),
                "🔔 Your Queue Called!",
                msgText,
                NotificationCategory.APPOINTMENT,
                (saved.getAppointment() != null && saved.getAppointment().getConsultationType() == ConsultationType.VIDEO)
                        ? "/patient/video-consultation/" + saved.getAppointment().getId()
                        : "/patient/queue/" + (saved.getAppointment() != null ? saved.getAppointment().getId() : "")
        );

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry recallPatient(Long queueId, User doctor) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        if (!entry.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this queue.");
        }

        entry.setStatus(QueueStatus.CALLED);
        entry.setCalledTime(LocalDateTime.now());
        entry.setRecallCount((entry.getRecallCount() != null ? entry.getRecallCount() : 0) + 1);
        entry.setUpdatedAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "DOCTOR_RECALLED_PATIENT", "Recalled Queue #" + saved.getQueueNumber());

        notificationService.sendPortalNotification(
                saved.getPatient(),
                "🔔 Recalled to Room",
                "Queue " + saved.getQueueNumber() + " has been called again. Please proceed to the consultation room.",
                NotificationCategory.APPOINTMENT,
                "/patient/queue/" + (saved.getAppointment() != null ? saved.getAppointment().getId() : "")
        );

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry startConsultation(Long queueId, User doctor) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        if (!entry.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this queue.");
        }

        entry.setStatus(QueueStatus.IN_PROGRESS);
        entry.setStartedTime(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        Appointment app = entry.getAppointment();
        if (app != null) {
            app.setStatus(AppointmentStatus.IN_PROGRESS);
            appointmentRepository.save(app);
        }

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "CONSULTATION_STARTED", "Started consultation for Queue #" + saved.getQueueNumber());

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry completeConsultation(Long queueId, User doctor) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        if (!entry.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("Unauthorized: You do not own this queue.");
        }

        entry.setStatus(QueueStatus.COMPLETED);
        entry.setCompletedTime(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        Appointment app = entry.getAppointment();
        if (app != null) {
            app.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(app);
        }

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "CONSULTATION_COMPLETED", "Completed Queue #" + saved.getQueueNumber());

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry skipPatient(Long queueId, User doctor, String reason) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        entry.setStatus(QueueStatus.SKIPPED);
        entry.setSkipReason(reason != null ? reason : "Patient not present");
        entry.setUpdatedAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "PATIENT_SKIPPED", "Skipped Queue #" + saved.getQueueNumber() + ". Reason: " + reason);

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry holdPatient(Long queueId, User doctor, String reason) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        entry.setStatus(QueueStatus.ON_HOLD);
        entry.setHoldReason(reason != null ? reason : "Temporarily unavailable");
        entry.setUpdatedAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "PATIENT_HELD", "Placed Queue #" + saved.getQueueNumber() + " on hold");

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    @Transactional
    public QueueEntry resumePatient(Long queueId, User doctor) {
        QueueEntry entry = queueEntryRepository.findById(queueId)
                .orElseThrow(() -> new RuntimeException("Queue entry not found"));

        entry.setStatus(QueueStatus.WAITING);
        entry.setUpdatedAt(LocalDateTime.now());

        QueueEntry saved = queueEntryRepository.save(entry);
        auditLogService.logAction(doctor, "PATIENT_RESUMED", "Resumed Queue #" + saved.getQueueNumber());

        broadcastQueueUpdate(doctor.getId());
        return saved;
    }

    public Map<String, Object> getPatientPositionInfo(Appointment appointment) {
        if (appointment == null || appointment.getId() == null) {
            Map<String, Object> emptyMap = new HashMap<>();
            emptyMap.put("checkedIn", false);
            emptyMap.put("queueNumber", "---");
            emptyMap.put("status", "NOT CHECKED IN");
            emptyMap.put("currentlyServing", "---");
            emptyMap.put("patientsAhead", 0);
            emptyMap.put("estimatedWaitMinutes", 0);
            emptyMap.put("doctorName", "---");
            emptyMap.put("department", "General");
            return emptyMap;
        }

        Optional<QueueEntry> entryOpt = queueEntryRepository.findByAppointment(appointment);
        if (entryOpt.isEmpty()) {
            Map<String, Object> notCheckedInMap = new HashMap<>();
            notCheckedInMap.put("checkedIn", false);
            notCheckedInMap.put("queueNumber", "---");
            notCheckedInMap.put("status", "NOT CHECKED IN");
            notCheckedInMap.put("currentlyServing", "---");
            notCheckedInMap.put("patientsAhead", 0);
            notCheckedInMap.put("estimatedWaitMinutes", 0);
            notCheckedInMap.put("doctorName", appointment.getDoctor() != null ? appointment.getDoctor().getFullName() : "---");
            notCheckedInMap.put("department", appointment.getDepartmentCategory() != null ? appointment.getDepartmentCategory() : "General Consultation");
            return notCheckedInMap;
        }

        QueueEntry entry = entryOpt.get();
        LocalDate today = LocalDate.now();

        long aheadCount = queueEntryRepository.countByDoctorAndQueueDateAndStatusInAndSequenceNumberLessThan(
                entry.getDoctor(), today, List.of(QueueStatus.WAITING, QueueStatus.CALLED), entry.getSequenceNumber());

        Optional<QueueEntry> currentlyServing = queueEntryRepository.findFirstByDoctorAndQueueDateAndStatusInOrderBySequenceNumberAsc(
                entry.getDoctor(), today, List.of(QueueStatus.IN_PROGRESS, QueueStatus.CALLED));

        int estWait = (int) (aheadCount * DEFAULT_AVG_CONSULTATION_MINUTES);

        Map<String, Object> res = new HashMap<>();
        res.put("checkedIn", true);
        res.put("queueNumber", entry.getQueueNumber());
        res.put("sequenceNumber", entry.getSequenceNumber());
        res.put("status", entry.getStatus().name());
        res.put("patientsAhead", aheadCount);
        res.put("currentlyServing", currentlyServing.map(QueueEntry::getQueueNumber).orElse("None"));
        res.put("estimatedWaitMinutes", estWait);
        res.put("doctorName", entry.getDoctor() != null ? entry.getDoctor().getFullName() : "---");
        res.put("department", entry.getDepartmentName() != null ? entry.getDepartmentName() : "General Consultation");
        return res;
    }

    public List<QueueEntry> getTodayDoctorQueue(User doctor) {
        if (doctor == null) return List.of();
        return queueEntryRepository.findByDoctorAndQueueDateOrderBySequenceNumberAsc(doctor, LocalDate.now());
    }

    public void broadcastQueueUpdate(Long doctorId) {
        if (doctorId == null) return;
        try {
            messagingTemplate.convertAndSend("/topic/doctor/" + doctorId + "/queue", Map.of("type", "QUEUE_UPDATED", "timestamp", System.currentTimeMillis()));
            messagingTemplate.convertAndSend("/topic/public/queue", Map.of("type", "PUBLIC_QUEUE_UPDATED", "timestamp", System.currentTimeMillis()));
        } catch (Exception e) {
            // Silently swallow websocket errors if clients disconnected
        }
    }
}
