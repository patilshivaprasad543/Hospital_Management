package com.hospital.repository;

import com.hospital.model.PatientVitals;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientVitalsRepository extends JpaRepository<PatientVitals, Long> {
    List<PatientVitals> findByPatientOrderByRecordedAtDesc(User patient);
}
