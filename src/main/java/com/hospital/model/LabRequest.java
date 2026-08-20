package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_requests")
public class LabRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id")
    private LabTest labTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_vendor_id")
    private User labVendor;

    private String testName;

    private String status = "REQUESTED"; // REQUESTED, PROCESSING, REPORT_READY

    @Column(length = 2000)
    private String reportResult;

    @Column(length = 1000)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public LabRequest() {}

    public LabRequest(User doctor, User patient, String testName, String notes) {
        this.doctor = doctor;
        this.patient = patient;
        this.testName = testName;
        this.notes = notes;
        this.status = "REQUESTED";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LabTest getLabTest() {
        return labTest;
    }

    public void setLabTest(LabTest labTest) {
        this.labTest = labTest;
    }

    public User getLabVendor() {
        return labVendor;
    }

    public void setLabVendor(User labVendor) {
        this.labVendor = labVendor;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportResult() {
        return reportResult;
    }

    public void setReportResult(String reportResult) {
        this.reportResult = reportResult;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
