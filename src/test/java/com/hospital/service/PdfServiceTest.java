package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.AppointmentStatus;
import com.hospital.model.Consultation;
import com.hospital.model.Invoice;
import com.hospital.model.LabRequest;
import com.hospital.model.PaymentStatus;
import com.hospital.model.PharmacyOrder;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void generatesPdfDocumentsForClinicalAndBillingRecords() {
        User patient = new User("Pat Patient", "pat@example.com", "9000000001", "x", Role.PATIENT);
        patient.setId(1L);
        User doctor = new User("Sam Doctor", "sam@example.com", "9000000002", "x", Role.DOCTOR);
        doctor.setId(2L);
        User pharmacy = new User("City Pharmacy", "pharm@example.com", "9000000003", "x", Role.VENDOR);
        pharmacy.setId(3L);

        Appointment appointment = new Appointment(patient, doctor, LocalDate.of(2026, 8, 20),
                LocalTime.of(10, 30), "Fever");
        appointment.setId(11L);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setDepartmentCategory("General Medicine");
        appointment.setQueueTicket("A12");

        Prescription prescription = new Prescription(appointment, doctor, patient, "Viral fever", "Rest and fluids", null);
        prescription.setId(21L);
        prescription.addItem(new PrescriptionItem("Paracetamol", "650mg", "1-0-1", "5 days", null));

        Invoice invoice = new Invoice(patient, "INV-100", "CONSULTATION", "OPD consult", 500.0, 11L);
        invoice.setPaymentStatus(PaymentStatus.PENDING);

        LabRequest lab = new LabRequest(doctor, patient, "CBC", "Fasting");
        lab.setId(31L);
        lab.setLabVendor(pharmacy);
        lab.setStatus("REPORT_READY");
        lab.setReportResult("Hemoglobin 14.2 g/dL");

        PharmacyOrder order = new PharmacyOrder(patient, prescription, pharmacy, 150.0, "Paracetamol x10", "12 Main St");
        order.setId(41L);

        Consultation consultation = new Consultation();
        consultation.setId(51L);
        consultation.setPatient(patient);
        consultation.setDoctor(doctor);
        consultation.setDiagnosis("Viral fever");
        consultation.setSymptoms("High temperature");
        consultation.setTreatment("Paracetamol and rest");

        assertStartsWithPdf(pdfService.generatePrescriptionPdf(prescription, doctor, patient));
        assertStartsWithPdf(pdfService.generateInvoicePdf(invoice, patient));
        assertStartsWithPdf(pdfService.generateLabReportPdf(lab, patient));
        assertStartsWithPdf(pdfService.generatePharmacyOrderPdf(order));
        assertStartsWithPdf(pdfService.generateConsultationPdf(consultation));
        assertStartsWithPdf(pdfService.generateAppointmentPdf(appointment));
    }

    private void assertStartsWithPdf(byte[] pdf) {
        assertTrue(pdf.length > 4);
        assertTrue(new String(pdf, 0, 4).equals("%PDF"));
    }
}
