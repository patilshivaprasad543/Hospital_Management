package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insurance_claims")
public class InsuranceClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    private Insurance insurance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Double claimAmount;

    @Column(nullable = false)
    private String status; // SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED

    private LocalDateTime claimDate;
    private String remarks;

    public InsuranceClaim() {
        this.claimDate = LocalDateTime.now();
        this.status = "SUBMITTED";
    }

    public InsuranceClaim(Insurance insurance, Invoice invoice, Double claimAmount, String remarks) {
        this.insurance = insurance;
        this.invoice = invoice;
        this.claimAmount = claimAmount;
        this.remarks = remarks;
        this.claimDate = LocalDateTime.now();
        this.status = "SUBMITTED";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Insurance getInsurance() { return insurance; }
    public void setInsurance(Insurance insurance) { this.insurance = insurance; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public Double getClaimAmount() { return claimAmount; }
    public void setClaimAmount(Double claimAmount) { this.claimAmount = claimAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDateTime claimDate) { this.claimDate = claimDate; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
