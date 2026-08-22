package com.hospital.repository;

import com.hospital.model.VisitChecklistCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitChecklistCompletionRepository extends JpaRepository<VisitChecklistCompletion, Long> {
    List<VisitChecklistCompletion> findByAppointmentIdAndPatientId(Long appointmentId, Long patientId);
    Optional<VisitChecklistCompletion> findByAppointmentIdAndPatientIdAndItemKey(
            Long appointmentId, Long patientId, String itemKey);
}
