package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.InvoiceRepository;
import com.hospital.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BillingService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public Invoice createInvoice(User patient, String chargeType, String description, Double amount, Long referenceId) {
        Invoice invoice = new Invoice(patient, generateInvoiceNumber(), chargeType, description, amount, referenceId);
        return invoiceRepository.save(invoice);
    }

    public Payment payInvoice(Long invoiceId, User patient) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (!invoice.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized payment attempt.");
        }
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Invoice is already paid.");
        }

        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoiceRepository.save(invoice);

        Payment payment = new Payment(invoice, patient, invoice.getAmount(), "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Payment saved = paymentRepository.save(payment);

        notificationService.sendNotification(patient, "💰 Payment Successful",
                "Payment of ₹" + invoice.getAmount() + " received for " + invoice.getDescription()
                        + ". Invoice: " + invoice.getInvoiceNumber(),
                NotificationCategory.BILLING, "/patient/bills");

        auditLogService.log(patient, "PAYMENT_COMPLETED", "BILLING", "Invoice", invoiceId,
                "Paid ₹" + invoice.getAmount());
        return saved;
    }

    public List<Invoice> getPatientInvoices(User patient) {
        return invoiceRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Payment> getPatientPayments(User patient) {
        return paymentRepository.findByPatientOrderByPaidAtDesc(patient);
    }

    public double getTotalRevenue() {
        return invoiceRepository.findAll().stream()
                .filter(i -> i.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(i -> i.getAmount() != null ? i.getAmount() : 0.0)
                .sum();
    }

    public long countPendingPayments() {
        return invoiceRepository.countByPaymentStatus(PaymentStatus.PENDING);
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis() % 1000000;
    }
}
