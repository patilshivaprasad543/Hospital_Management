package com.hospital.repository;

import com.hospital.model.LabRequest;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabRequestRepository extends JpaRepository<LabRequest, Long> {

    @Query("SELECT r FROM LabRequest r LEFT JOIN FETCH r.doctor LEFT JOIN FETCH r.labVendor WHERE r.patient = :patient ORDER BY r.createdAt DESC")
    List<LabRequest> findByPatientOrderByCreatedAtDesc(@Param("patient") User patient);

    @Query("SELECT r FROM LabRequest r LEFT JOIN FETCH r.patient LEFT JOIN FETCH r.labVendor WHERE r.doctor = :doctor ORDER BY r.createdAt DESC")
    List<LabRequest> findByDoctorOrderByCreatedAtDesc(@Param("doctor") User doctor);

    @Query("SELECT r FROM LabRequest r LEFT JOIN FETCH r.patient LEFT JOIN FETCH r.doctor WHERE r.labVendor = :labVendor ORDER BY r.createdAt DESC")
    List<LabRequest> findByLabVendorOrderByCreatedAtDesc(@Param("labVendor") User labVendor);

    List<LabRequest> findByStatusOrderByCreatedAtDesc(String status);

    long countByStatus(String status);
}
