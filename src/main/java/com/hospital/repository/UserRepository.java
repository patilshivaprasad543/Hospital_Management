package com.hospital.repository;

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

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleAndVendorType(Role role, VendorType vendorType);

    long countByRole(Role role);
}
