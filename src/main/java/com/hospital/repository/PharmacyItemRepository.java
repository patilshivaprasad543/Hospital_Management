package com.hospital.repository;

import com.hospital.model.PharmacyItem;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyItemRepository extends JpaRepository<PharmacyItem, Long> {
    List<PharmacyItem> findByVendor(User vendor);

    @Query("SELECT i FROM PharmacyItem i LEFT JOIN FETCH i.vendor")
    List<PharmacyItem> findAllWithVendor();

    long countByStockQuantityGreaterThan(int quantity);

    long countByStockQuantityLessThanEqualAndStockQuantityGreaterThan(int max, int min);

    long countByStockQuantity(int quantity);
}
