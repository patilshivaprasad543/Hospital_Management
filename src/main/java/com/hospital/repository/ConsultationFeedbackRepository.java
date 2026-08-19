package com.hospital.repository;

import com.hospital.model.ConsultationFeedback;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationFeedbackRepository extends JpaRepository<ConsultationFeedback, Long> {
    Optional<ConsultationFeedback> findByAppointmentId(Long appointmentId);
    List<ConsultationFeedback> findByDoctorOrderByCreatedAtDesc(User doctor);
    boolean existsByAppointmentId(Long appointmentId);

    @Query("SELECT COALESCE(AVG(f.rating), 0) FROM ConsultationFeedback f WHERE f.doctor = :doctor")
    double averageRatingByDoctor(User doctor);

    @Query("SELECT COUNT(f) FROM ConsultationFeedback f WHERE f.doctor = :doctor")
    long countByDoctor(User doctor);
}
