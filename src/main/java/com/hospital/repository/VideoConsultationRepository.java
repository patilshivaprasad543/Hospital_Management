package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.User;
import com.hospital.model.VideoConsultation;
import com.hospital.model.VideoRoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoConsultationRepository extends JpaRepository<VideoConsultation, Long> {
    Optional<VideoConsultation> findByAppointment(Appointment appointment);
    Optional<VideoConsultation> findByRoomId(String roomId);
    List<VideoConsultation> findByPatientOrderByScheduledStartDesc(User patient);
    List<VideoConsultation> findByDoctorOrderByScheduledStartDesc(User doctor);
    List<VideoConsultation> findByStatus(VideoRoomStatus status);
}
