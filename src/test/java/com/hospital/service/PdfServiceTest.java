package com.hospital.service;

import com.hospital.model.Invoice;
import com.hospital.model.PaymentStatus;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void generateInvoicePdfContainsPdfHeader() {
        User patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "x", Role.PATIENT);
        Invoice invoice = new Invoice(patient, "INV-1001", "PHARMACY", "Order #40", 175.0, 40L);
        invoice.setPaymentStatus(PaymentStatus.PENDING);
        invoice.setCreatedAt(LocalDateTime.of(2026, 8, 20, 10, 0));

        byte[] pdf = pdfService.generateInvoicePdf(invoice, patient);

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
