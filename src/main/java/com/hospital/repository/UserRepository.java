package com.hospital.repository;

import com.hospital.model.ApprovalStatus;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobileNumber(String mobileNumber);

    List<User> findByRole(Role role);

    List<User> findByRoleAndVendorType(Role role, VendorType vendorType);

    long countByRole(Role role);

    List<User> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<User> findByRoleAndApprovalStatus(Role role, ApprovalStatus approvalStatus);

    List<User> findByRoleAndAdminApprovedTrueAndApprovalStatus(Role role, ApprovalStatus approvalStatus);
}
