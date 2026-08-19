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

    @Enumerated(EnumType.STRING)
    private PharmacyOrderStatus status = PharmacyOrderStatus.PLACED;

    @Column(length = 1000)
    private String orderSummary;

    @Column(length = 500)
    private String deliveryAddress;

    @Column(length = 500)
    private String trackingNotes;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public PharmacyOrder() {}

    public PharmacyOrder(User patient, Prescription prescription, User pharmacyVendor,
                         Double totalPrice, String orderSummary, String deliveryAddress) {
        this.patient = patient;
        this.prescription = prescription;
        this.pharmacyVendor = pharmacyVendor;
        this.totalPrice = totalPrice;
        this.orderSummary = orderSummary;
        this.deliveryAddress = deliveryAddress;
        this.status = PharmacyOrderStatus.PLACED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public PharmacyOrderStatus getStatus() {
        return status;
    }

    public void setStatus(PharmacyOrderStatus status) {
        this.status = status;
    }

    public String getOrderSummary() {
        return orderSummary;
    }

    public void setOrderSummary(String orderSummary) {
        this.orderSummary = orderSummary;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getTrackingNotes() {
        return trackingNotes;
    }

    public void setTrackingNotes(String trackingNotes) {
        this.trackingNotes = trackingNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
