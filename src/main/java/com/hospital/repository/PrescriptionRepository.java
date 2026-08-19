package com.hospital.repository;

import com.hospital.model.Prescription;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.doctor " +
           "LEFT JOIN FETCH p.items " +
           "WHERE p.patient = :patient ORDER BY p.createdAt DESC")
    List<Prescription> findByPatientOrderByCreatedAtDesc(@Param("patient") User patient);

    @Query("SELECT DISTINCT p FROM Prescription p " +
           "LEFT JOIN FETCH p.patient " +
           "LEFT JOIN FETCH p.items " +
           "WHERE p.doctor = :doctor ORDER BY p.createdAt DESC")
    List<Prescription> findByDoctorOrderByCreatedAtDesc(@Param("doctor") User doctor);
}
