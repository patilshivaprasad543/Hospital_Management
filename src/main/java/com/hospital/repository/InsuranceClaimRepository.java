package com.hospital.repository;

import com.hospital.model.Insurance;
import com.hospital.model.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Long> {
    List<InsuranceClaim> findByInsurance(Insurance insurance);
    List<InsuranceClaim> findByStatus(String status);
    long countByStatus(String status);
    List<InsuranceClaim> findAllByOrderByClaimDateDesc();
}
