package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.AmbulanceRepository;
import com.hospital.repository.AmbulanceTripRepository;
import com.hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AmbulanceService {

    @Autowired
    private AmbulanceRepository ambulanceRepository;

    @Autowired
    private AmbulanceTripRepository ambulanceTripRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public List<Ambulance> getAllAmbulances() {
        return ambulanceRepository.findAll();
    }

    public List<Ambulance> getAvailableAmbulances() {
        return ambulanceRepository.findByStatus(AmbulanceStatus.AVAILABLE);
    }

    public Ambulance saveAmbulance(Ambulance ambulance) {
        return ambulanceRepository.save(ambulance);
    }

    @Transactional
    public AmbulanceTrip requestAmbulance(User patient, String pickupAddress, String destinationAddress,
                                          EmergencyPriority priority, AmbulanceType requestedType,
                                          String contactPerson, String contactMobile, String reason) {
        User managedPatient = userRepository.findById(patient.getId()).orElse(patient);
        Double estimatedFare = (requestedType != null && requestedType == AmbulanceType.ADVANCED_LIFE_SUPPORT) ? 3500.0 : 1500.0;

        AmbulanceTrip trip = new AmbulanceTrip(managedPatient, pickupAddress, destinationAddress, priority, requestedType, estimatedFare);
        trip.setContactPerson(contactPerson != null ? contactPerson : managedPatient.getFullName());
        trip.setContactMobile(contactMobile != null ? contactMobile : managedPatient.getMobileNumber());
        trip.setReason(reason);
        trip.setStatus(AmbulanceTripStatus.REQUESTED);

        AmbulanceTrip savedTrip = ambulanceTripRepository.save(trip);
        auditLogService.log(managedPatient, "AMBULANCE_REQUESTED", "AMBULANCE", "Ambulance requested for pickup: " + pickupAddress);

        notificationService.sendNotification(managedPatient, "Ambulance Request Received",
                "Your ambulance request #" + savedTrip.getId() + " has been received and is being dispatched.",
                NotificationCategory.AMBULANCE, "/patient/ambulance");

        return savedTrip;
    }

    @Transactional
    public AmbulanceTrip assignAmbulance(Long tripId, Long ambulanceId, User adminUser) {
        AmbulanceTrip trip = ambulanceTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Ambulance request not found: " + tripId));
        Ambulance ambulance = ambulanceRepository.findById(ambulanceId)
                .orElseThrow(() -> new RuntimeException("Ambulance not found: " + ambulanceId));

        if (ambulance.getStatus() != AmbulanceStatus.AVAILABLE) {
            throw new RuntimeException("Ambulance " + ambulance.getVehicleNumber() + " is currently unavailable.");
        }

        ambulance.setStatus(AmbulanceStatus.ASSIGNED);
        ambulanceRepository.save(ambulance);

        trip.setAmbulance(ambulance);
        trip.setStatus(AmbulanceTripStatus.ASSIGNED);
        trip.setDispatchTime(LocalDateTime.now());
        AmbulanceTrip updatedTrip = ambulanceTripRepository.save(trip);

        auditLogService.log(adminUser, "AMBULANCE_ASSIGNED", "AMBULANCE",
                "Assigned ambulance " + ambulance.getVehicleNumber() + " to trip #" + tripId);

        notificationService.sendNotification(trip.getPatient(), "Ambulance Dispatched",
                "Ambulance " + ambulance.getVehicleNumber() + " has been assigned to your request #" + tripId + ".",
                NotificationCategory.AMBULANCE, "/patient/ambulance");

        return updatedTrip;
    }

    @Transactional
    public AmbulanceTrip updateTripStatus(Long tripId, AmbulanceTripStatus status, User user) {
        AmbulanceTrip trip = ambulanceTripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Ambulance request not found: " + tripId));

        trip.setStatus(status);
        if (status == AmbulanceTripStatus.AT_PICKUP || status == AmbulanceTripStatus.ARRIVED) {
            trip.setArrivalTime(LocalDateTime.now());
        } else if (status == AmbulanceTripStatus.COMPLETED) {
            trip.setCompletionTime(LocalDateTime.now());
            if (trip.getAmbulance() != null) {
                trip.getAmbulance().setStatus(AmbulanceStatus.AVAILABLE);
                ambulanceRepository.save(trip.getAmbulance());
            }
        }
        AmbulanceTrip updatedTrip = ambulanceTripRepository.save(trip);

        auditLogService.log(user, "AMBULANCE_STATUS_UPDATED", "AMBULANCE",
                "Trip #" + tripId + " status updated to " + status);

        notificationService.sendNotification(trip.getPatient(), "Ambulance Update",
                "Trip #" + tripId + " status updated to: " + status,
                NotificationCategory.AMBULANCE, "/patient/ambulance");

        return updatedTrip;
    }

    public List<AmbulanceTrip> getPatientTrips(User patient) {
        return ambulanceTripRepository.findByPatientOrderByRequestTimeDesc(patient);
    }

    public List<AmbulanceTrip> getAllTrips() {
        return ambulanceTripRepository.findAllByOrderByRequestTimeDesc();
    }
}
