package com.hospital.repository;

import com.hospital.model.PharmacyOrder;
import com.hospital.model.PharmacyOrderStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {

    @Query("SELECT o FROM PharmacyOrder o " +
           "LEFT JOIN FETCH o.pharmacyVendor " +
           "WHERE o.patient = :patient ORDER BY o.createdAt DESC")
    List<PharmacyOrder> findByPatientOrderByCreatedAtDesc(@Param("patient") User patient);

    @Query("SELECT o FROM PharmacyOrder o " +
           "LEFT JOIN FETCH o.patient " +
           "WHERE o.pharmacyVendor = :pharmacyVendor ORDER BY o.createdAt DESC")
    List<PharmacyOrder> findByPharmacyVendorOrderByCreatedAtDesc(@Param("pharmacyVendor") User pharmacyVendor);

    long countByStatus(PharmacyOrderStatus status);
}
