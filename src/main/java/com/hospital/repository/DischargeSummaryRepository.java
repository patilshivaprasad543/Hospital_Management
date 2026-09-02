package com.hospital.repository;

import com.hospital.model.Admission;
import com.hospital.model.DischargeSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DischargeSummaryRepository extends JpaRepository<DischargeSummary, Long> {
    Optional<DischargeSummary> findByAdmission(Admission admission);
}
