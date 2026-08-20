package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.InvoiceRepository;
import com.hospital.repository.PaymentRepository;
import com.hospital.repository.PharmacyOrderRepository;
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

    @Autowired
    private PharmacyOrderRepository pharmacyOrderRepository;

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

        if ("PHARMACY".equalsIgnoreCase(invoice.getChargeType()) && invoice.getReferenceId() != null) {
            pharmacyOrderRepository.findById(invoice.getReferenceId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                pharmacyOrderRepository.save(order);
                if (order.getPharmacyVendor() != null) {
                    notificationService.sendPortalNotification(order.getPharmacyVendor(),
                            "💰 Payment confirmation",
                            "Payment received for pharmacy order #" + order.getId()
                                    + " (" + patient.getFullName() + "). Invoice: " + invoice.getInvoiceNumber(),
                            NotificationCategory.BILLING, "/vendor/orders/" + order.getId());
                }
            });
        }

        notificationService.sendPortalNotification(patient, "💰 Payment Successful",
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

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllWithPatient();
    }

    public Invoice requireInvoice(Long invoiceId) {
        return invoiceRepository.findDetailedById(invoiceId)
                .or(() -> invoiceRepository.findById(invoiceId))
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAllDetailed();
    }

    public Payment recordPaymentAsAdmin(Long invoiceId, User admin) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Invoice is already paid.");
        }
        invoice.setPaymentStatus(PaymentStatus.PAID);
        invoiceRepository.save(invoice);
        Payment payment = new Payment(invoice, invoice.getPatient(), invoice.getAmount(),
                "ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        Payment saved = paymentRepository.save(payment);

        if ("PHARMACY".equalsIgnoreCase(invoice.getChargeType()) && invoice.getReferenceId() != null) {
            pharmacyOrderRepository.findById(invoice.getReferenceId()).ifPresent(order -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                pharmacyOrderRepository.save(order);
            });
        }

        notificationService.sendPortalNotification(invoice.getPatient(), "💰 Payment recorded",
                "Payment of ₹" + invoice.getAmount() + " was recorded for " + invoice.getDescription()
                        + ". Invoice: " + invoice.getInvoiceNumber(),
                NotificationCategory.BILLING, "/patient/bills");
        auditLogService.log(admin, "PAYMENT_RECORDED", "BILLING", "Invoice", invoiceId,
                "Admin recorded ₹" + invoice.getAmount());
        return saved;
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

    public DoctorEarnings getDoctorEarnings(User doctor, List<Appointment> appointments) {
        List<Appointment> completed = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate monthStart = today.withDayOfMonth(1);
        long todayCount = completed.stream()
                .filter(a -> today.equals(a.getAppointmentDate()))
                .count();
        long monthCount = completed.stream()
                .filter(a -> a.getAppointmentDate() != null && !a.getAppointmentDate().isBefore(monthStart))
                .count();
        List<Invoice> paid = invoiceRepository.findByChargeTypeAndPaymentStatus("CONSULTATION", PaymentStatus.PAID);
        java.util.Set<Long> appointmentIds = completed.stream().map(Appointment::getId).collect(java.util.stream.Collectors.toSet());
        double earned = paid.stream()
                .filter(i -> i.getReferenceId() != null && appointmentIds.contains(i.getReferenceId()))
                .mapToDouble(i -> i.getAmount() != null ? i.getAmount() : 0)
                .sum();
        double monthEarned = paid.stream()
                .filter(i -> i.getReferenceId() != null && appointmentIds.contains(i.getReferenceId()))
                .filter(i -> i.getCreatedAt() != null && !i.getCreatedAt().toLocalDate().isBefore(monthStart))
                .mapToDouble(i -> i.getAmount() != null ? i.getAmount() : 0)
                .sum();
        return new DoctorEarnings(completed.size(), todayCount, monthCount, earned, monthEarned);
    }

    public record DoctorEarnings(long completedAppointments, long todayCompleted, long monthCompleted,
                                 double totalEarned, double monthEarned) {
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis() % 1000000;
    }
}
