package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.PreCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreCheckInRepository extends JpaRepository<PreCheckIn, Long> {
    Optional<PreCheckIn> findByAppointment(Appointment appointment);
    boolean existsByAppointment(Appointment appointment);
}
