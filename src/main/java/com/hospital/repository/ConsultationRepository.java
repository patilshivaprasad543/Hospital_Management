package com.hospital.repository;

import com.hospital.model.Consultation;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByPatientOrderByStartedAtDesc(User patient);
    List<Consultation> findByDoctorOrderByStartedAtDesc(User doctor);
    Optional<Consultation> findByAppointmentId(Long appointmentId);
}
