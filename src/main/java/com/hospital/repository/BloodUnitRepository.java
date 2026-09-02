package com.hospital.repository;

import com.hospital.model.BloodGroup;
import com.hospital.model.BloodUnit;
import com.hospital.model.BloodUnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {
    List<BloodUnit> findByStatus(BloodUnitStatus status);
    List<BloodUnit> findByBloodGroupAndStatus(BloodGroup bloodGroup, BloodUnitStatus status);
    long countByBloodGroupAndStatus(BloodGroup bloodGroup, BloodUnitStatus status);
    Optional<BloodUnit> findByUnitCode(String unitCode);
}
