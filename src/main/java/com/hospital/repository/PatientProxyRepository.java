package com.hospital.repository;

import com.hospital.model.PatientProxy;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProxyRepository extends JpaRepository<PatientProxy, Long> {
    List<PatientProxy> findByPatientAndStatus(User patient, String status);
    List<PatientProxy> findByProxyUserAndStatus(User proxyUser, String status);
    boolean existsByPatientAndProxyUser(User patient, User proxyUser);
}
