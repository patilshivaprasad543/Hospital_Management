package com.hospital.service;

import com.hospital.model.Invoice;
import com.hospital.model.LabRequest;
import com.hospital.model.Prescription;
import com.hospital.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11);

    public byte[] generatePrescriptionPdf(Prescription prescription, User doctor, User patient) {
        return buildPdf("SmartCare 360 — Prescription", document -> {
            document.add(new Paragraph("Prescription #" + prescription.getId(), TITLE));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Patient: " + patient.getFullName(), BODY));
            document.add(new Paragraph("Doctor: Dr. " + doctor.getFullName(), BODY));
            document.add(new Paragraph("Date: " + prescription.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), BODY));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Diagnosis: " + nullSafe(prescription.getDiagnosis()), HEADING));
            document.add(new Paragraph("Instructions: " + nullSafe(prescription.getInstructions()), BODY));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Medicines:", HEADING));
            if (prescription.getItems() != null) {
                for (var item : prescription.getItems()) {
                    document.add(new Paragraph("• " + item.getMedicineName() + " — " + item.getDosage()
                            + ", " + item.getFrequency() + ", " + item.getDuration(), BODY));
                }
            }
        });
    }

    public byte[] generateInvoicePdf(Invoice invoice, User patient) {
        return buildPdf("SmartCare 360 — Invoice", document -> {
            document.add(new Paragraph("Invoice " + invoice.getInvoiceNumber(), TITLE));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Patient: " + patient.getFullName(), BODY));
            document.add(new Paragraph("Type: " + invoice.getChargeType(), BODY));
            document.add(new Paragraph("Description: " + invoice.getDescription(), BODY));
            document.add(new Paragraph("Amount: ₹" + invoice.getAmount(), HEADING));
            document.add(new Paragraph("Status: " + invoice.getPaymentStatus(), BODY));
            document.add(new Paragraph("Date: " + invoice.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), BODY));
        });
    }

    public byte[] generateLabReportPdf(LabRequest labRequest, User patient) {
        return buildPdf("SmartCare 360 — Lab Report", document -> {
            document.add(new Paragraph("Lab Report #" + labRequest.getId(), TITLE));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Patient: " + patient.getFullName(), BODY));
            document.add(new Paragraph("Test: " + nullSafe(labRequest.getTestName()), HEADING));
            document.add(new Paragraph("Requested by: Dr. " + (labRequest.getDoctor() != null ? labRequest.getDoctor().getFullName() : "-"), BODY));
            document.add(new Paragraph("Laboratory: " + (labRequest.getLabVendor() != null ? labRequest.getLabVendor().getFullName() : "-"), BODY));
            document.add(new Paragraph("Status: " + nullSafe(labRequest.getStatus()), BODY));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Findings:", HEADING));
            document.add(new Paragraph(nullSafe(labRequest.getReportResult()), BODY));
        });
    }

    private byte[] buildPdf(String title, PdfConsumer consumer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.addTitle(title);
            document.add(new Paragraph("SmartCare 360 Hospital", TITLE));
            document.add(new Paragraph(" "));
            consumer.accept(document);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "-";
    }

    @FunctionalInterface
    private interface PdfConsumer {
        void accept(Document document) throws DocumentException;
    }
}
