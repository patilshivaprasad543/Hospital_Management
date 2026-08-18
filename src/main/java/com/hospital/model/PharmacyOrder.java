package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_orders")
public class PharmacyOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_vendor_id")
    private User pharmacyVendor;

    private Double totalPrice;

    private String status = "PLACED"; // PLACED, ACCEPTED, READY_FOR_PICKUP, COMPLETED, CANCELLED

    @Column(length = 1000)
    private String orderSummary;

    private LocalDateTime createdAt = LocalDateTime.now();

    public PharmacyOrder() {}

    public PharmacyOrder(User patient, Prescription prescription, User pharmacyVendor, Double totalPrice, String orderSummary) {
        this.patient = patient;
        this.prescription = prescription;
        this.pharmacyVendor = pharmacyVendor;
        this.totalPrice = totalPrice;
        this.orderSummary = orderSummary;
        this.status = "PLACED";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public User getPharmacyVendor() {
        return pharmacyVendor;
    }

    public void setPharmacyVendor(User pharmacyVendor) {
        this.pharmacyVendor = pharmacyVendor;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderSummary() {
        return orderSummary;
    }

    public void setOrderSummary(String orderSummary) {
        this.orderSummary = orderSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
