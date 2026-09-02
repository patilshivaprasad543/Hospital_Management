package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_pre_checkins")
public class PreCheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    private String confirmedAllergies;
    private String currentMedications;
    private String chiefComplaint;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private Boolean insuranceVerified;

    private LocalDateTime completedAt;

    public PreCheckIn() {}

    public PreCheckIn(Appointment appointment, User patient, String confirmedAllergies, String currentMedications, String chiefComplaint, String emergencyContactName, String emergencyContactPhone) {
        this.appointment = appointment;
        this.patient = patient;
        this.confirmedAllergies = confirmedAllergies;
        this.currentMedications = currentMedications;
        this.chiefComplaint = chiefComplaint;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.insuranceVerified = true;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getConfirmedAllergies() { return confirmedAllergies; }
    public void setConfirmedAllergies(String confirmedAllergies) { this.confirmedAllergies = confirmedAllergies; }

    public String getCurrentMedications() { return currentMedications; }
    public void setCurrentMedications(String currentMedications) { this.currentMedications = currentMedications; }

    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public Boolean getInsuranceVerified() { return insuranceVerified; }
    public void setInsuranceVerified(Boolean insuranceVerified) { this.insuranceVerified = insuranceVerified; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
