package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.Prescription;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientOrderByCreatedAtDesc(User patient);
    List<Prescription> findByDoctorOrderByCreatedAtDesc(User doctor);
    Optional<Prescription> findByAppointment(Appointment appointment);
}
