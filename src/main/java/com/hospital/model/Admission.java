package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admissions")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_id")
    private Bed bed;

    @Column(nullable = false)
    private String reason;

    private LocalDateTime admissionDate;
    private LocalDateTime expectedDischargeDate;
    private LocalDateTime actualDischargeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdmissionStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Admission() {}

    public Admission(User patient, User doctor, String reason, String notes) {
        this.patient = patient;
        this.doctor = doctor;
        this.reason = reason;
        this.notes = notes;
        this.status = AdmissionStatus.REQUESTED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }

    public Bed getBed() { return bed; }
    public void setBed(Bed bed) { this.bed = bed; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDateTime admissionDate) { this.admissionDate = admissionDate; }

    public LocalDateTime getExpectedDischargeDate() { return expectedDischargeDate; }
    public void setExpectedDischargeDate(LocalDateTime expectedDischargeDate) { this.expectedDischargeDate = expectedDischargeDate; }

    public LocalDateTime getActualDischargeDate() { return actualDischargeDate; }
    public void setActualDischargeDate(LocalDateTime actualDischargeDate) { this.actualDischargeDate = actualDischargeDate; }

    public AdmissionStatus getStatus() { return status; }
    public void setStatus(AdmissionStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
