package com.hospital.repository;

import com.hospital.model.PharmacyStockMovement;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyStockMovementRepository extends JpaRepository<PharmacyStockMovement, Long> {

    @Query("SELECT m FROM PharmacyStockMovement m LEFT JOIN FETCH m.item " +
           "WHERE m.vendor = :vendor ORDER BY m.createdAt DESC")
    List<PharmacyStockMovement> findByVendorOrderByCreatedAtDesc(@Param("vendor") User vendor);
}
