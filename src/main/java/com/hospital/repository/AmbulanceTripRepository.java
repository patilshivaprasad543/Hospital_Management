package com.hospital.repository;

import com.hospital.model.AmbulanceTrip;
import com.hospital.model.AmbulanceTripStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmbulanceTripRepository extends JpaRepository<AmbulanceTrip, Long> {
    List<AmbulanceTrip> findByPatientOrderByRequestTimeDesc(User patient);
    List<AmbulanceTrip> findByStatus(AmbulanceTripStatus status);
    List<AmbulanceTrip> findAllByOrderByRequestTimeDesc();
}
