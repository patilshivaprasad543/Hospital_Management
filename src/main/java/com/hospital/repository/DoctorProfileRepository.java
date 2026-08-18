package com.hospital.repository;

import com.hospital.model.DoctorProfile;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUser(User user);
    Optional<DoctorProfile> findByUserId(Long userId);
    List<DoctorProfile> findBySpecializationContainingIgnoreCase(String specialization);
}
