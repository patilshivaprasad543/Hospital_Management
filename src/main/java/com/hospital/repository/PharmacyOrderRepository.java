package com.hospital.repository;

import com.hospital.model.PharmacyOrder;
import com.hospital.model.PharmacyOrderStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.hospital.model.Prescription;

@Repository
public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {

    @Query("SELECT o FROM PharmacyOrder o " +
           "LEFT JOIN FETCH o.pharmacyVendor " +
           "LEFT JOIN FETCH o.prescription " +
           "WHERE o.patient = :patient ORDER BY o.createdAt DESC")
    List<PharmacyOrder> findByPatientOrderByCreatedAtDesc(@Param("patient") User patient);

    @Query("SELECT o FROM PharmacyOrder o " +
           "LEFT JOIN FETCH o.patient " +
           "LEFT JOIN FETCH o.pharmacyVendor " +
           "LEFT JOIN FETCH o.prescription p " +
           "LEFT JOIN FETCH p.doctor " +
           "LEFT JOIN FETCH p.items " +
           "WHERE o.id = :id")
    Optional<PharmacyOrder> findDetailedById(@Param("id") Long id);

    boolean existsByPrescriptionAndStatusNotIn(Prescription prescription, List<PharmacyOrderStatus> statuses);

    Optional<PharmacyOrder> findFirstByPrescriptionOrderByCreatedAtDesc(Prescription prescription);

    @Query("SELECT o FROM PharmacyOrder o " +
           "LEFT JOIN FETCH o.patient " +
           "WHERE o.pharmacyVendor = :pharmacyVendor ORDER BY o.createdAt DESC")
    List<PharmacyOrder> findByPharmacyVendorOrderByCreatedAtDesc(@Param("pharmacyVendor") User pharmacyVendor);

    long countByStatus(PharmacyOrderStatus status);
}
