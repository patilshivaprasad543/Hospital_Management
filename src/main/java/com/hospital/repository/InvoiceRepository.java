package com.hospital.repository;

import com.hospital.model.Invoice;
import com.hospital.model.PaymentStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatientOrderByCreatedAtDesc(User patient);
    long countByPaymentStatus(PaymentStatus status);
    List<Invoice> findByPaymentStatus(PaymentStatus status);
}
