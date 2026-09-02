package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergencies")
public class Emergency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;

    private String patientName; // If patient not registered yet

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmergencyPriority priority;

    @Column(nullable = false)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private User assignedDoctor;

    private LocalDateTime arrivalTime;
    private String vitals;
    private String initialTreatment;
    private String status; // ARRIVED, TRIACHED, IN_TREATMENT, ADMITTED, DISCHARGED

    public Emergency() {
        this.arrivalTime = LocalDateTime.now();
        this.status = "ARRIVED";
    }

    public Emergency(User patient, String patientName, EmergencyPriority priority, String reason, User assignedDoctor) {
        this.patient = patient;
        this.patientName = patientName;
        this.priority = priority;
        this.reason = reason;
        this.assignedDoctor = assignedDoctor;
        this.arrivalTime = LocalDateTime.now();
        this.status = "ARRIVED";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public EmergencyPriority getPriority() { return priority; }
    public void setPriority(EmergencyPriority priority) { this.priority = priority; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public User getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(User assignedDoctor) { this.assignedDoctor = assignedDoctor; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public String getVitals() { return vitals; }
    public void setVitals(String vitals) { this.vitals = vitals; }

    public String getInitialTreatment() { return initialTreatment; }
    public void setInitialTreatment(String initialTreatment) { this.initialTreatment = initialTreatment; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
