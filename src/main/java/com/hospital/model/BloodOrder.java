package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_orders")
public class BloodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodComponentType componentType;

    @Column(nullable = false)
    private Integer units = 1;

    private Double unitPrice = 1500.0;

    private Double totalPrice = 1500.0;

    @Column(length = 100)
    private String deliveryType = "HOSPITAL_WARD";

    @Column(length = 255)
    private String deliveryLocation;

    @Column(length = 50)
    private String patientContact;

    @Column(length = 1000)
    private String clinicalNotes;

    @Column(length = 50)
    private String urgencyLevel = "URGENT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodOrderStatus status = BloodOrderStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(length = 50)
    private String paymentMethod = "HOSPITAL_BILL";

    @Column(length = 500)
    private String allocatedUnitCodes;

    @Column(length = 1000)
    private String rejectionReason;

    private boolean prescriptionVerified = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    public BloodOrder() {}

    public BloodOrder(String orderNumber, User patient, Prescription prescription, User doctor,
                      BloodGroup bloodGroup, BloodComponentType componentType, Integer units,
                      Double unitPrice, String deliveryType, String deliveryLocation,
                      String patientContact, String clinicalNotes, String urgencyLevel, String paymentMethod) {
        this.orderNumber = orderNumber;
        this.patient = patient;
        this.prescription = prescription;
        this.doctor = doctor;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType;
        this.units = units != null && units > 0 ? units : 1;
        this.unitPrice = unitPrice != null ? unitPrice : 1500.0;
        this.totalPrice = this.unitPrice * this.units;
        this.deliveryType = deliveryType != null ? deliveryType : "HOSPITAL_WARD";
        this.deliveryLocation = deliveryLocation;
        this.patientContact = patientContact;
        this.clinicalNotes = clinicalNotes;
        this.urgencyLevel = urgencyLevel != null ? urgencyLevel : "URGENT";
        this.paymentMethod = paymentMethod != null ? paymentMethod : "HOSPITAL_BILL";
        this.status = BloodOrderStatus.REQUESTED;
        this.paymentStatus = PaymentStatus.PENDING;
        this.prescriptionVerified = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
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

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public BloodComponentType getComponentType() {
        return componentType;
    }

    public void setComponentType(BloodComponentType componentType) {
        this.componentType = componentType;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
        if (this.unitPrice != null && this.units != null) {
            this.totalPrice = this.unitPrice * this.units;
        }
    }

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
        if (this.unitPrice != null && this.units != null) {
            this.totalPrice = this.unitPrice * this.units;
        }
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getDeliveryLocation() {
        return deliveryLocation;
    }

    public void setDeliveryLocation(String deliveryLocation) {
        this.deliveryLocation = deliveryLocation;
    }

    public String getPatientContact() {
        return patientContact;
    }

    public void setPatientContact(String patientContact) {
        this.patientContact = patientContact;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public BloodOrderStatus getStatus() {
        return status;
    }

    public void setStatus(BloodOrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getAllocatedUnitCodes() {
        return allocatedUnitCodes;
    }

    public void setAllocatedUnitCodes(String allocatedUnitCodes) {
        this.allocatedUnitCodes = allocatedUnitCodes;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public boolean isPrescriptionVerified() {
        return prescriptionVerified;
    }

    public void setPrescriptionVerified(boolean prescriptionVerified) {
        this.prescriptionVerified = prescriptionVerified;
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
