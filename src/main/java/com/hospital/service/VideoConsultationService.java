package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VideoConsultationService {

    @Autowired
    private VideoConsultationRepository videoConsultationRepository;

    @Autowired
    private VideoChatMessageRepository videoChatMessageRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public VideoConsultation createVideoRoom(Appointment appointment) {
        if (appointment.getConsultationType() != ConsultationType.VIDEO) {
            appointment.setConsultationType(ConsultationType.VIDEO);
            appointmentRepository.save(appointment);
        }

        Optional<VideoConsultation> existing = videoConsultationRepository.findByAppointment(appointment);
        if (existing.isPresent()) {
            return existing.get();
        }

        String roomId = "VR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime startDateTime = LocalDateTime.of(
                appointment.getAppointmentDate() != null ? appointment.getAppointmentDate() : java.time.LocalDate.now(),
                appointment.getAppointmentTime() != null ? appointment.getAppointmentTime() : java.time.LocalTime.now()
        );
        LocalDateTime joinFrom = LocalDateTime.now().minusDays(1);
        LocalDateTime joinExpires = LocalDateTime.now().plusDays(30);

        appointment.setVideoRoomId(roomId);
        appointment.setVideoStatus(VideoRoomStatus.CREATED);
        appointment.setVideoJoinAvailableFrom(joinFrom);
        appointment.setVideoJoinExpiresAt(joinExpires);
        appointmentRepository.save(appointment);

        VideoConsultation consultation = new VideoConsultation(
                appointment, roomId, appointment.getPatient(), appointment.getDoctor(),
                startDateTime, startDateTime.plusMinutes(30), joinFrom, joinExpires
        );

        VideoConsultation saved = videoConsultationRepository.save(consultation);

        auditLogService.logAction(
                appointment.getPatient(),
                "VIDEO_ROOM_CREATED",
                "Video consultation room created for Appointment #" + appointment.getId()
        );

        notificationService.sendPortalNotification(
                appointment.getPatient(),
                "📹 Video Consultation Confirmed",
                "Your video appointment with Dr. " + appointment.getDoctor().getFullName() + " is ready.",
                NotificationCategory.APPOINTMENT,
                "/patient/video-consultation/" + appointment.getId()
        );

        notificationService.sendPortalNotification(
                appointment.getDoctor(),
                "📹 Video Appointment Scheduled",
                "Video consultation with " + appointment.getPatient().getFullName() + " is ready.",
                NotificationCategory.APPOINTMENT,
                "/doctor/video-consultation/" + appointment.getId()
        );

        return saved;
    }

    public void validateAccess(User user, Appointment appointment) {
        if (user == null || appointment == null) {
            throw new RuntimeException("Access denied: Invalid session or appointment.");
        }

        if (appointment.getConsultationType() != ConsultationType.VIDEO) {
            appointment.setConsultationType(ConsultationType.VIDEO);
            appointmentRepository.save(appointment);
        }

        boolean isPatient = user.getId().equals(appointment.getPatient().getId());
        boolean isDoctor = user.getId().equals(appointment.getDoctor().getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isPatient && !isDoctor && !isAdmin) {
            throw new RuntimeException("Access denied: You are not a participant in this consultation.");
        }

        if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new RuntimeException("Access denied: This appointment has been cancelled or rejected.");
        }
    }

    @Transactional
    public VideoConsultation startConsultation(Long appointmentId, User doctor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getDoctor().getId().equals(doctor.getId()) && doctor.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only the assigned doctor can start this consultation.");
        }

        VideoConsultation room = videoConsultationRepository.findByAppointment(appointment)
                .orElseGet(() -> createVideoRoom(appointment));

        room.setStatus(VideoRoomStatus.ACTIVE);
        room.setDoctorJoined(true);
        room.setDoctorJoinedAt(LocalDateTime.now());
        if (room.getActualStart() == null) {
            room.setActualStart(LocalDateTime.now());
        }
        videoConsultationRepository.save(room);

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        appointment.setVideoStatus(VideoRoomStatus.ACTIVE);
        appointmentRepository.save(appointment);

        auditLogService.logAction(doctor, "VIDEO_CONSULTATION_STARTED", "Doctor started video room " + room.getRoomId());

        notificationService.sendPortalNotification(
                appointment.getPatient(),
                "🟢 Doctor Has Joined Call",
                "Dr. " + doctor.getFullName() + " has started your video consultation. Click join to enter.",
                NotificationCategory.APPOINTMENT,
                "/patient/video-consultation/" + appointmentId
        );

        return room;
    }

    @Transactional
    public VideoConsultation endConsultation(Long appointmentId, User doctor) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        VideoConsultation room = videoConsultationRepository.findByAppointment(appointment)
                .orElseGet(() -> createVideoRoom(appointment));

        room.setStatus(VideoRoomStatus.COMPLETED);
        room.setActualEnd(LocalDateTime.now());
        videoConsultationRepository.save(room);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setVideoStatus(VideoRoomStatus.COMPLETED);
        appointmentRepository.save(appointment);

        auditLogService.logAction(doctor, "VIDEO_CONSULTATION_ENDED", "Video room ended for Appointment #" + appointmentId);

        notificationService.sendPortalNotification(
                appointment.getPatient(),
                "🏁 Video Consultation Ended",
                "Your video consultation with Dr. " + appointment.getDoctor().getFullName() + " has ended. Check your dashboard for records.",
                NotificationCategory.APPOINTMENT,
                "/patient/dashboard"
        );

        return room;
    }

    @Transactional
    public VideoChatMessage saveChatMessage(String roomId, User sender, String messageText) {
        VideoChatMessage msg = new VideoChatMessage(roomId, sender, messageText);
        return videoChatMessageRepository.save(msg);
    }

    public List<VideoChatMessage> getChatHistory(String roomId) {
        return videoChatMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);
    }

    public VideoConsultation getByAppointment(Appointment appointment) {
        return videoConsultationRepository.findByAppointment(appointment).orElse(null);
    }

    @Transactional
    public List<VideoConsultation> getPatientConsultations(User patient) {
        List<Appointment> patientAppts = appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
        List<VideoConsultation> list = new ArrayList<>();
        for (Appointment appt : patientAppts) {
            if (appt.getStatus() != AppointmentStatus.CANCELLED && appt.getStatus() != AppointmentStatus.REJECTED) {
                VideoConsultation room = videoConsultationRepository.findByAppointment(appt)
                        .orElseGet(() -> createVideoRoom(appt));
                list.add(room);
            }
        }
        return list;
    }

    @Transactional
    public List<VideoConsultation> getDoctorConsultations(User doctor) {
        List<Appointment> doctorAppts = appointmentRepository.findByDoctorOrderByCreatedAtDesc(doctor);
        List<VideoConsultation> list = new ArrayList<>();
        for (Appointment appt : doctorAppts) {
            if (appt.getStatus() != AppointmentStatus.CANCELLED && appt.getStatus() != AppointmentStatus.REJECTED) {
                VideoConsultation room = videoConsultationRepository.findByAppointment(appt)
                        .orElseGet(() -> createVideoRoom(appt));
                list.add(room);
            }
        }
        return list;
    }
}
