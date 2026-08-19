package com.hospital.repository;

import com.hospital.model.PharmacyOrder;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyOrderRepository extends JpaRepository<PharmacyOrder, Long> {
    List<PharmacyOrder> findByPatientOrderByCreatedAtDesc(User patient);
    List<PharmacyOrder> findByPharmacyVendorOrderByCreatedAtDesc(User pharmacyVendor);

    long countByStatus(String status);
}
