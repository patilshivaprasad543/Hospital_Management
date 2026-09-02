package com.hospital.repository;

import com.hospital.model.Insurance;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
    Optional<Insurance> findByPatient(User patient);
    Optional<Insurance> findByPolicyNumber(String policyNumber);
}
