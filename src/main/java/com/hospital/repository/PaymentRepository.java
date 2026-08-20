package com.hospital.repository;

import com.hospital.model.Payment;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByPatientOrderByPaidAtDesc(User patient);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p LEFT JOIN FETCH p.patient LEFT JOIN FETCH p.invoice ORDER BY p.paidAt DESC")
    java.util.List<Payment> findAllDetailed();
}
