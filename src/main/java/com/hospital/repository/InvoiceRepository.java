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

    List<Invoice> findByChargeTypeAndPaymentStatus(String chargeType, PaymentStatus status);

    java.util.Optional<Invoice> findFirstByChargeTypeAndReferenceId(String chargeType, Long referenceId);

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.patient ORDER BY i.createdAt DESC")
    List<Invoice> findAllWithPatient();

    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.patient WHERE i.id = :id")
    java.util.Optional<Invoice> findDetailedById(@org.springframework.data.repository.query.Param("id") Long id);
}
