package com.hospital.service;

import com.hospital.model.Invoice;
import com.hospital.model.Payment;
import com.hospital.model.PaymentStatus;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.InvoiceRepository;
import com.hospital.repository.PaymentRepository;
import com.hospital.repository.PharmacyOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private PharmacyOrderRepository pharmacyOrderRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    void recordPaymentAsAdminMarksInvoicePaid() {
        User patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "x", Role.PATIENT);
        patient.setId(1L);
        User admin = new User("Admin", "admin@smartcare360.com", "9999999999", "x", Role.ADMIN);
        admin.setId(9L);
        Invoice invoice = new Invoice(patient, "INV-1", "CONSULTATION", "Visit", 500.0, 12L);
        invoice.setId(3L);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = billingService.recordPaymentAsAdmin(3L, admin);

        assertEquals(PaymentStatus.PAID, invoice.getPaymentStatus());
        assertEquals(500.0, payment.getAmount());
        verify(auditLogService).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordPaymentAsAdminRejectsAlreadyPaidInvoice() {
        User patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "x", Role.PATIENT);
        Invoice invoice = new Invoice(patient, "INV-1", "CONSULTATION", "Visit", 500.0, 12L);
        invoice.setId(3L);
        invoice.setPaymentStatus(PaymentStatus.PAID);
        when(invoiceRepository.findById(3L)).thenReturn(Optional.of(invoice));

        assertThrows(RuntimeException.class, () -> billingService.recordPaymentAsAdmin(3L,
                new User("Admin", "admin@smartcare360.com", "9999999999", "x", Role.ADMIN)));
    }
}
