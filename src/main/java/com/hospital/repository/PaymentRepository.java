package com.hospital.repository;

import com.hospital.model.Payment;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientOrderByPaidAtDesc(User patient);
}
