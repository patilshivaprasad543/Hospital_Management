package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_vitals")
public class PatientVitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer heartRate;
    private Double bloodGlucose;
    private Double bodyTemperature;
    private Integer spo2;
    private Double weightKg;
    private Double heightCm;
    private Double bmi;

    private String notes;
    private LocalDateTime recordedAt = LocalDateTime.now();

    public PatientVitals() {}

    public PatientVitals(User patient, Integer systolicBp, Integer diastolicBp, Integer heartRate, Double bloodGlucose, Double bodyTemperature, Integer spo2, Double weightKg, Double heightCm) {
        this.patient = patient;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.heartRate = heartRate;
        this.bloodGlucose = bloodGlucose;
        this.bodyTemperature = bodyTemperature;
        this.spo2 = spo2;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        if (weightKg != null && heightCm != null && heightCm > 0) {
            double heightM = heightCm / 100.0;
            this.bmi = Math.round((weightKg / (heightM * heightM)) * 10.0) / 10.0;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public Integer getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Integer systolicBp) { this.systolicBp = systolicBp; }

    public Integer getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Integer diastolicBp) { this.diastolicBp = diastolicBp; }

    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }

    public Double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }

    public Double getBodyTemperature() { return bodyTemperature; }
    public void setBodyTemperature(Double bodyTemperature) { this.bodyTemperature = bodyTemperature; }

    public Integer getSpo2() { return spo2; }
    public void setSpo2(Integer spo2) { this.spo2 = spo2; }

    public Double getWeightKg() { return weightKg; }
    public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getBmi() { return bmi; }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
