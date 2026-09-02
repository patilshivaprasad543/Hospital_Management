package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    private String diagnosis;

    @Column(length = 1000)
    private String instructions;

    private LocalDate followUpDate;

    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    private BloodComponentType bloodComponentType;

    private Integer bloodUnits;

    @Column(length = 500)
    private String bloodTransfusionReason;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionItem> items = new ArrayList<>();

    public Prescription() {}

    public Prescription(Appointment appointment, User doctor, User patient, String diagnosis, String instructions, LocalDate followUpDate) {
        this.appointment = appointment;
        this.doctor = doctor;
        this.patient = patient;
        this.diagnosis = diagnosis;
        this.instructions = instructions;
        this.followUpDate = followUpDate;
        this.createdAt = LocalDateTime.now();
    }

    public void addItem(PrescriptionItem item) {
        items.add(item);
        item.setPrescription(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> items) {
        this.items = items;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public BloodComponentType getBloodComponentType() {
        return bloodComponentType;
    }

    public void setBloodComponentType(BloodComponentType bloodComponentType) {
        this.bloodComponentType = bloodComponentType;
    }

    public Integer getBloodUnits() {
        return bloodUnits;
    }

    public void setBloodUnits(Integer bloodUnits) {
        this.bloodUnits = bloodUnits;
    }

    public String getBloodTransfusionReason() {
        return bloodTransfusionReason;
    }

    public void setBloodTransfusionReason(String bloodTransfusionReason) {
        this.bloodTransfusionReason = bloodTransfusionReason;
    }

    public boolean hasBloodPrescription() {
        return (bloodUnits != null && bloodUnits > 0) || bloodGroup != null;
    }
}
