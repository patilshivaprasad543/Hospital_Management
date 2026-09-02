package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "blood_units")
public class BloodUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String unitCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodGroup bloodGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodComponentType componentType;

    private String donorName;
    private String donorContact;
    private Integer volumeMl;
    private LocalDate collectionDate;
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BloodUnitStatus status;

    public BloodUnit() {
        this.status = BloodUnitStatus.AVAILABLE;
        this.collectionDate = LocalDate.now();
        this.expiryDate = LocalDate.now().plusDays(42);
        this.volumeMl = 450;
    }

    public BloodUnit(String unitCode, BloodGroup bloodGroup, BloodComponentType componentType, String donorName, String donorContact, Integer volumeMl, LocalDate expiryDate) {
        this.unitCode = unitCode;
        this.bloodGroup = bloodGroup;
        this.componentType = componentType;
        this.donorName = donorName;
        this.donorContact = donorContact;
        this.volumeMl = volumeMl != null ? volumeMl : 450;
        this.collectionDate = LocalDate.now();
        this.expiryDate = expiryDate != null ? expiryDate : LocalDate.now().plusDays(42);
        this.status = BloodUnitStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }

    public BloodComponentType getComponentType() { return componentType; }
    public void setComponentType(BloodComponentType componentType) { this.componentType = componentType; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }

    public String getDonorContact() { return donorContact; }
    public void setDonorContact(String driverContact) { this.donorContact = driverContact; }

    public Integer getVolumeMl() { return volumeMl; }
    public void setVolumeMl(Integer volumeMl) { this.volumeMl = volumeMl; }

    public LocalDate getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDate collectionDate) { this.collectionDate = collectionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BloodUnitStatus getStatus() { return status; }
    public void setStatus(BloodUnitStatus status) { this.status = status; }
}
