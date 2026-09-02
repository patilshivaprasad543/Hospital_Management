package com.hospital.repository;

import com.hospital.model.Ambulance;
import com.hospital.model.AmbulanceStatus;
import com.hospital.model.AmbulanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByStatus(AmbulanceStatus status);
    List<Ambulance> findByTypeAndStatus(AmbulanceType type, AmbulanceStatus status);
    Optional<Ambulance> findByVehicleNumber(String vehicleNumber);
}
