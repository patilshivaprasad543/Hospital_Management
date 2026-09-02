package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "video_consultations")
public class VideoConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Column(nullable = false, unique = true)
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private VideoRoomStatus status;

    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime joinAvailableFrom;
    private LocalDateTime joinExpiresAt;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;

    private boolean patientJoined = false;
    private boolean doctorJoined = false;
    private LocalDateTime patientJoinedAt;
    private LocalDateTime doctorJoinedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public VideoConsultation() {}

    public VideoConsultation(Appointment appointment, String roomId, User patient, User doctor,
                             LocalDateTime scheduledStart, LocalDateTime scheduledEnd,
                             LocalDateTime joinAvailableFrom, LocalDateTime joinExpiresAt) {
        this.appointment = appointment;
        this.roomId = roomId;
        this.patient = patient;
        this.doctor = doctor;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.joinAvailableFrom = joinAvailableFrom;
        this.joinExpiresAt = joinExpiresAt;
        this.status = VideoRoomStatus.CREATED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public VideoRoomStatus getStatus() { return status; }
    public void setStatus(VideoRoomStatus status) { this.status = status; }

    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }

    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }

    public LocalDateTime getJoinAvailableFrom() { return joinAvailableFrom; }
    public void setJoinAvailableFrom(LocalDateTime joinAvailableFrom) { this.joinAvailableFrom = joinAvailableFrom; }

    public LocalDateTime getJoinExpiresAt() { return joinExpiresAt; }
    public void setJoinExpiresAt(LocalDateTime joinExpiresAt) { this.joinExpiresAt = joinExpiresAt; }

    public LocalDateTime getActualStart() { return actualStart; }
    public void setActualStart(LocalDateTime actualStart) { this.actualStart = actualStart; }

    public LocalDateTime getActualEnd() { return actualEnd; }
    public void setActualEnd(LocalDateTime actualEnd) { this.actualEnd = actualEnd; }

    public boolean isPatientJoined() { return patientJoined; }
    public void setPatientJoined(boolean patientJoined) { this.patientJoined = patientJoined; }

    public boolean isDoctorJoined() { return doctorJoined; }
    public void setDoctorJoined(boolean doctorJoined) { this.doctorJoined = doctorJoined; }

    public LocalDateTime getPatientJoinedAt() { return patientJoinedAt; }
    public void setPatientJoinedAt(LocalDateTime patientJoinedAt) { this.patientJoinedAt = patientJoinedAt; }

    public LocalDateTime getDoctorJoinedAt() { return doctorJoinedAt; }
    public void setDoctorJoinedAt(LocalDateTime doctorJoinedAt) { this.doctorJoinedAt = doctorJoinedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
