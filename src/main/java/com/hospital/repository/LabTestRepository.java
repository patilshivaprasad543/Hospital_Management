package com.hospital.repository;

import com.hospital.model.LabTest;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabTestRepository extends JpaRepository<LabTest, Long> {

    List<LabTest> findByVendor(User vendor);

    @Query("SELECT t FROM LabTest t LEFT JOIN FETCH t.vendor")
    List<LabTest> findAllWithVendor();
}
