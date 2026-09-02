package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ambulance_trips")
public class AmbulanceTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambulance_id")
    private Ambulance ambulance;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private String destinationAddress;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private EmergencyPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AmbulanceType requestedType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmbulanceTripStatus status;

    private String contactPerson;
    private String contactMobile;
    private String reason;
    private Double estimatedFare;
    private LocalDateTime requestTime;
    private LocalDateTime dispatchTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime completionTime;

    public AmbulanceTrip() {
        this.requestTime = LocalDateTime.now();
        this.status = AmbulanceTripStatus.REQUESTED;
        this.priority = EmergencyPriority.MEDIUM;
    }

    public AmbulanceTrip(User patient, String pickupAddress, String destinationAddress, EmergencyPriority priority, AmbulanceType requestedType, Double estimatedFare) {
        this.patient = patient;
        this.pickupAddress = pickupAddress;
        this.destinationAddress = destinationAddress;
        this.priority = priority;
        this.requestedType = requestedType;
        this.estimatedFare = estimatedFare;
        this.requestTime = LocalDateTime.now();
        this.status = AmbulanceTripStatus.REQUESTED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }

    public Ambulance getAmbulance() { return ambulance; }
    public void setAmbulance(Ambulance ambulance) { this.ambulance = ambulance; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public String getDestinationAddress() { return destinationAddress; }
    public void setDestinationAddress(String destinationAddress) { this.destinationAddress = destinationAddress; }

    public EmergencyPriority getPriority() { return priority; }
    public void setPriority(EmergencyPriority priority) { this.priority = priority; }

    public AmbulanceType getRequestedType() { return requestedType; }
    public void setRequestedType(AmbulanceType requestedType) { this.requestedType = requestedType; }

    public AmbulanceTripStatus getStatus() { return status; }
    public void setStatus(AmbulanceTripStatus status) { this.status = status; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactMobile() { return contactMobile; }
    public void setContactMobile(String contactMobile) { this.contactMobile = contactMobile; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getEstimatedFare() { return estimatedFare; }
    public void setEstimatedFare(Double estimatedFare) { this.estimatedFare = estimatedFare; }

    public LocalDateTime getRequestTime() { return requestTime; }
    public void setRequestTime(LocalDateTime requestTime) { this.requestTime = requestTime; }

    public LocalDateTime getDispatchTime() { return dispatchTime; }
    public void setDispatchTime(LocalDateTime dispatchTime) { this.dispatchTime = dispatchTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public LocalDateTime getCompletionTime() { return completionTime; }
    public void setCompletionTime(LocalDateTime completionTime) { this.completionTime = completionTime; }
}
