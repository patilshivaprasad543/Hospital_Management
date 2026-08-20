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

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Consultation c LEFT JOIN FETCH c.patient LEFT JOIN FETCH c.doctor ORDER BY c.startedAt DESC")
    List<Consultation> findAllDetailed();

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Consultation c LEFT JOIN FETCH c.patient LEFT JOIN FETCH c.doctor LEFT JOIN FETCH c.appointment WHERE c.id = :id")
    Optional<Consultation> findDetailedById(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Consultation c LEFT JOIN FETCH c.patient LEFT JOIN FETCH c.doctor LEFT JOIN FETCH c.appointment WHERE c.appointment.id = :appointmentId")
    Optional<Consultation> findDetailedByAppointmentId(@org.springframework.data.repository.query.Param("appointmentId") Long appointmentId);
}
