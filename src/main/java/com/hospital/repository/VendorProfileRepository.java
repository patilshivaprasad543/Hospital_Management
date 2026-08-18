package com.hospital.repository;

import com.hospital.model.User;
import com.hospital.model.VendorProfile;
import com.hospital.model.VendorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {
    Optional<VendorProfile> findByUser(User user);
    Optional<VendorProfile> findByUserId(Long userId);
    List<VendorProfile> findByVendorType(VendorType vendorType);
}
