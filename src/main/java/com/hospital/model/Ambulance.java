package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "ambulances")
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmbulanceType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmbulanceStatus status;

    private String driverName;
    private String driverContact;
    private String equipmentList;
    private Double basePrice;
    private String registrationNumber;
    private LocalDate insuranceExpiry;
    private LocalDate lastServiceDate;

    public Ambulance() {
        this.status = AmbulanceStatus.AVAILABLE;
        this.type = AmbulanceType.BASIC;
        this.basePrice = 1500.0;
    }

    public Ambulance(String vehicleNumber, AmbulanceType type, String driverName, String driverContact, Double basePrice) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
        this.driverName = driverName;
        this.driverContact = driverContact;
        this.basePrice = basePrice;
        this.status = AmbulanceStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public AmbulanceType getType() { return type; }
    public void setType(AmbulanceType type) { this.type = type; }

    public AmbulanceStatus getStatus() { return status; }
    public void setStatus(AmbulanceStatus status) { this.status = status; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverContact() { return driverContact; }
    public void setDriverContact(String driverContact) { this.driverContact = driverContact; }

    public String getEquipmentList() { return equipmentList; }
    public void setEquipmentList(String equipmentList) { this.equipmentList = equipmentList; }

    public Double getBasePrice() { return basePrice; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public LocalDate getInsuranceExpiry() { return insuranceExpiry; }
    public void setInsuranceExpiry(LocalDate insuranceExpiry) { this.insuranceExpiry = insuranceExpiry; }

    public LocalDate getLastServiceDate() { return lastServiceDate; }
    public void setLastServiceDate(LocalDate lastServiceDate) { this.lastServiceDate = lastServiceDate; }
}
