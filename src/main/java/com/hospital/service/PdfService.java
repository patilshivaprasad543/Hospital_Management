package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.Consultation;
import com.hospital.model.Invoice;
import com.hospital.model.LabRequest;
import com.hospital.model.PharmacyOrder;
import com.hospital.model.Prescription;
import com.hospital.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Autowired(required = false)
    private HospitalSettingService hospitalSettingService;

    public ResponseEntity<byte[]> download(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    public byte[] generatePrescriptionPdf(Prescription prescription, User doctor, User patient) {
        return buildPdf("SmartCare 360 — Prescription", document -> {
            document.add(new Paragraph("Prescription #" + prescription.getId(), TITLE));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Patient: " + patient.getFullName(), BODY));
            document.add(new Paragraph("Doctor: Dr. " + doctor.getFullName(), BODY));
            if (prescription.getCreatedAt() != null) {
                document.add(new Paragraph("Date: " + prescription.getCreatedAt().format(DATE), BODY));
            }
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
            if (invoice.getCreatedAt() != null) {
                document.add(new Paragraph("Date: " + invoice.getCreatedAt().format(DATE_TIME), BODY));
            }
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

    public byte[] generatePharmacyOrderPdf(PharmacyOrder order) {
        return buildPdf("SmartCare 360 — Pharmacy Order", document -> {
            document.add(new Paragraph("Pharmacy Order #ORD-" + order.getId(), TITLE));
            document.add(new Paragraph(" "));
            if (order.getPatient() != null) {
                document.add(new Paragraph("Patient: " + order.getPatient().getFullName(), BODY));
            }
            if (order.getPharmacyVendor() != null) {
                document.add(new Paragraph("Pharmacy: " + order.getPharmacyVendor().getFullName(), BODY));
            }
            document.add(new Paragraph("Status: " + (order.getStatus() != null ? order.getStatus().getDisplayName() : "-"), BODY));
            document.add(new Paragraph("Payment: " + String.valueOf(order.getPaymentStatus()), BODY));
            document.add(new Paragraph("Items: " + nullSafe(order.getOrderSummary()), BODY));
            document.add(new Paragraph("Delivery: " + nullSafe(order.getDeliveryAddress()), BODY));
            document.add(new Paragraph("Total: ₹" + (order.getTotalPrice() != null ? order.getTotalPrice() : 0), HEADING));
            if (order.getTrackingNotes() != null && !order.getTrackingNotes().isBlank()) {
                document.add(new Paragraph("Tracking: " + order.getTrackingNotes(), BODY));
            }
            if (order.getCreatedAt() != null) {
                document.add(new Paragraph("Ordered: " + order.getCreatedAt().format(DATE_TIME), BODY));
            }
        });
    }

    public byte[] generateConsultationPdf(Consultation consultation) {
        return buildPdf("SmartCare 360 — Consultation Summary", document -> {
            document.add(new Paragraph("Consultation Summary", TITLE));
            document.add(new Paragraph(" "));
            if (consultation.getPatient() != null) {
                document.add(new Paragraph("Patient: " + consultation.getPatient().getFullName(), BODY));
            }
            if (consultation.getDoctor() != null) {
                document.add(new Paragraph("Doctor: Dr. " + consultation.getDoctor().getFullName(), BODY));
            }
            if (consultation.getCompletedAt() != null) {
                document.add(new Paragraph("Completed: " + consultation.getCompletedAt().format(DATE_TIME), BODY));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Symptoms: " + nullSafe(consultation.getSymptoms()), BODY));
            document.add(new Paragraph("Diagnosis: " + nullSafe(consultation.getDiagnosis()), HEADING));
            document.add(new Paragraph("Treatment: " + nullSafe(consultation.getTreatment()), BODY));
            document.add(new Paragraph("Observations: " + nullSafe(consultation.getObservations()), BODY));
            document.add(new Paragraph("Notes: " + nullSafe(consultation.getNotes()), BODY));
            if (consultation.getFollowUpDate() != null) {
                document.add(new Paragraph("Follow-up: " + consultation.getFollowUpDate().format(DATE), BODY));
            }
        });
    }

    public byte[] generateAppointmentPdf(Appointment appointment) {
        return buildPdf("SmartCare 360 — Appointment", document -> {
            document.add(new Paragraph("Appointment Slip #APP-" + appointment.getId(), TITLE));
            document.add(new Paragraph(" "));
            if (appointment.getPatient() != null) {
                document.add(new Paragraph("Patient: " + appointment.getPatient().getFullName(), BODY));
            }
            if (appointment.getDoctor() != null) {
                document.add(new Paragraph("Doctor: Dr. " + appointment.getDoctor().getFullName(), BODY));
            }
            document.add(new Paragraph("Date: " + appointment.getAppointmentDate() + " " + appointment.getAppointmentTime(), HEADING));
            document.add(new Paragraph("Status: " + (appointment.getStatus() != null ? appointment.getStatus().getLabel() : "-"), BODY));
            document.add(new Paragraph("Department: " + nullSafe(appointment.getDepartmentCategory()), BODY));
            document.add(new Paragraph("Reason: " + nullSafe(appointment.getReason()), BODY));
            if (appointment.getQueueTicket() != null) {
                document.add(new Paragraph("Queue ticket: " + appointment.getQueueTicket(), HEADING));
            }
        });
    }

    private byte[] buildPdf(String title, PdfConsumer consumer) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();
            document.addTitle(title);
            document.add(new Paragraph(hospitalName(), TITLE));
            String address = setting(HospitalSettingService.HOSPITAL_ADDRESS);
            String phone = setting(HospitalSettingService.HOSPITAL_PHONE);
            if (!address.isBlank() || !phone.isBlank()) {
                document.add(new Paragraph((address.isBlank() ? "" : address) + (phone.isBlank() ? "" : " · " + phone), BODY));
            }
            document.add(new Paragraph(" "));
            consumer.accept(document);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }

    private String hospitalName() {
        String name = setting(HospitalSettingService.HOSPITAL_NAME);
        return name.isBlank() ? "SmartCare 360 Hospital" : name;
    }

    private String setting(String key) {
        if (hospitalSettingService == null) {
            return "";
        }
        String value = hospitalSettingService.get(key);
        return value != null ? value.trim() : "";
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }

    @FunctionalInterface
    private interface PdfConsumer {
        void accept(Document document) throws DocumentException;
    }
}
