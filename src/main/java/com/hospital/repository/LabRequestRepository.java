package com.hospital.repository;

import com.hospital.model.LabRequest;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabRequestRepository extends JpaRepository<LabRequest, Long> {
    List<LabRequest> findByPatientOrderByCreatedAtDesc(User patient);
    List<LabRequest> findByDoctorOrderByCreatedAtDesc(User doctor);
    List<LabRequest> findByLabVendorOrderByCreatedAtDesc(User labVendor);
    List<LabRequest> findByStatusOrderByCreatedAtDesc(String status);
}
