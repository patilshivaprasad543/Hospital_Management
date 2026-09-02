package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discharge_summaries")
public class DischargeSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "admission_id", nullable = false)
    private Admission admission;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String finalDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String treatmentSummary;

    @Column(columnDefinition = "TEXT")
    private String dischargeMedications;

    @Column(columnDefinition = "TEXT")
    private String followUpInstructions;

    private LocalDateTime dischargeDate;

    public DischargeSummary() {}

    public DischargeSummary(Admission admission, String finalDiagnosis, String treatmentSummary, String dischargeMedications, String followUpInstructions) {
        this.admission = admission;
        this.finalDiagnosis = finalDiagnosis;
        this.treatmentSummary = treatmentSummary;
        this.dischargeMedications = dischargeMedications;
        this.followUpInstructions = followUpInstructions;
        this.dischargeDate = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Admission getAdmission() { return admission; }
    public void setAdmission(Admission admission) { this.admission = admission; }

    public String getFinalDiagnosis() { return finalDiagnosis; }
    public void setFinalDiagnosis(String finalDiagnosis) { this.finalDiagnosis = finalDiagnosis; }

    public String getTreatmentSummary() { return treatmentSummary; }
    public void setTreatmentSummary(String treatmentSummary) { this.treatmentSummary = treatmentSummary; }

    public String getDischargeMedications() { return dischargeMedications; }
    public void setDischargeMedications(String dischargeMedications) { this.dischargeMedications = dischargeMedications; }

    public String getFollowUpInstructions() { return followUpInstructions; }
    public void setFollowUpInstructions(String followUpInstructions) { this.followUpInstructions = followUpInstructions; }

    public LocalDateTime getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDateTime dischargeDate) { this.dischargeDate = dischargeDate; }
}
