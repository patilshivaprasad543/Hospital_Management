package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.QueueEntry;
import com.hospital.model.QueueStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    Optional<QueueEntry> findByAppointment(Appointment appointment);

    List<QueueEntry> findByDoctorAndQueueDateOrderBySequenceNumberAsc(User doctor, LocalDate queueDate);

    List<QueueEntry> findByDoctorAndQueueDateAndStatusInOrderBySequenceNumberAsc(User doctor, LocalDate queueDate, List<QueueStatus> statuses);

    long countByDoctorAndQueueDate(User doctor, LocalDate queueDate);

    long countByDoctorAndQueueDateAndStatus(User doctor, LocalDate queueDate, QueueStatus status);

    long countByDoctorAndQueueDateAndStatusInAndSequenceNumberLessThan(User doctor, LocalDate queueDate, List<QueueStatus> statuses, Integer sequenceNumber);

    Optional<QueueEntry> findFirstByDoctorAndQueueDateAndStatusOrderBySequenceNumberAsc(User doctor, LocalDate queueDate, QueueStatus status);

    Optional<QueueEntry> findFirstByDoctorAndQueueDateAndStatusInOrderBySequenceNumberAsc(User doctor, LocalDate queueDate, List<QueueStatus> statuses);

    List<QueueEntry> findByPatientOrderByCreatedAtDesc(User patient);
}
