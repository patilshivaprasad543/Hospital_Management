package com.hospital.repository;

import com.hospital.model.Admission;
import com.hospital.model.AdmissionStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionRepository extends JpaRepository<Admission, Long> {
    List<Admission> findByPatientOrderByAdmissionDateDesc(User patient);
    List<Admission> findByPatientOrderByIdDesc(User patient);
    List<Admission> findByDoctorOrderByAdmissionDateDesc(User doctor);
    List<Admission> findByStatus(AdmissionStatus status);
    List<Admission> findByStatusOrderByIdDesc(AdmissionStatus status);
    List<Admission> findAllByOrderByIdDesc();
    long countByStatus(AdmissionStatus status);
}
