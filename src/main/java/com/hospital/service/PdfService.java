package com.hospital.service;

import com.hospital.model.Invoice;
import com.hospital.model.LabRequest;
import com.hospital.model.Prescription;
import com.hospital.model.User;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Font TITLE =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);

    private static final Font HEADING =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

    private static final Font BODY =
            FontFactory.getFont(FontFactory.HELVETICA, 11);

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");


    // ============================================================
    // PRESCRIPTION PDF
    // ============================================================

    public byte[] generatePrescriptionPdf(
            Prescription prescription,
            User doctor,
            User patient) {

        return buildPdf(
                "SmartCare 360 - Prescription",
                document -> {

                    document.add(
                            new Paragraph(
                                    "Prescription #" +
                                            prescription.getId(),
                                    TITLE
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Patient: " +
                                            nullSafe(patient.getFullName()),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Doctor: Dr. " +
                                            nullSafe(doctor.getFullName()),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Date: " +
                                            formatDate(
                                                    prescription.getCreatedAt()
                                            ),
                                    BODY
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Diagnosis",
                                    HEADING
                            )
                    );

                    document.add(
                            new Paragraph(
                                    nullSafe(
                                            prescription.getDiagnosis()
                                    ),
                                    BODY
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Instructions",
                                    HEADING
                            )
                    );

                    document.add(
                            new Paragraph(
                                    nullSafe(
                                            prescription.getInstructions()
                                    ),
                                    BODY
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Medicines",
                                    HEADING
                            )
                    );

                    if (prescription.getItems() != null
                            && !prescription.getItems().isEmpty()) {

                        for (var item : prescription.getItems()) {

                            String medicine =
                                    nullSafe(item.getMedicineName());

                            String dosage =
                                    nullSafe(item.getDosage());

                            String frequency =
                                    nullSafe(item.getFrequency());

                            String duration =
                                    nullSafe(item.getDuration());

                            String instructions =
                                    nullSafe(item.getInstructions());

                            document.add(
                                    new Paragraph(
                                            medicine +
                                                    " - Dosage: " +
                                                    dosage +
                                                    ", Frequency: " +
                                                    frequency +
                                                    ", Duration: " +
                                                    duration +
                                                    ", Instructions: " +
                                                    instructions,
                                            BODY
                                    )
                            );
                        }

                    } else {

                        document.add(
                                new Paragraph(
                                        "No medicines prescribed.",
                                        BODY
                                )
                        );
                    }
                }
        );
    }


    // ============================================================
    // INVOICE PDF
    // ============================================================

    public byte[] generateInvoicePdf(
            Invoice invoice,
            User patient) {

        return buildPdf(
                "SmartCare 360 - Invoice",
                document -> {

                    document.add(
                            new Paragraph(
                                    "Invoice " +
                                            nullSafe(
                                                    invoice.getInvoiceNumber()
                                            ),
                                    TITLE
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Patient: " +
                                            nullSafe(
                                                    patient.getFullName()
                                            ),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Charge Type: " +
                                            nullSafe(
                                                    invoice.getChargeType()
                                            ),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Description: " +
                                            nullSafe(
                                                    invoice.getDescription()
                                            ),
                                    BODY
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Amount: ₹" +
                                            String.valueOf(
                                                    invoice.getAmount()
                                            ),
                                    HEADING
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Payment Status: " +
                                            enumSafe(
                                                    invoice.getPaymentStatus()
                                            ),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Date: " +
                                            formatDateTime(
                                                    invoice.getCreatedAt()
                                            ),
                                    BODY
                            )
                    );
                }
        );
    }


    // ============================================================
    // LAB REPORT PDF
    // ============================================================

    public byte[] generateLabReportPdf(
            LabRequest labRequest,
            User patient) {

        return buildPdf(
                "SmartCare 360 - Lab Report",
                document -> {

                    document.add(
                            new Paragraph(
                                    "Lab Report #" +
                                            labRequest.getId(),
                                    TITLE
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Patient: " +
                                            nullSafe(
                                                    patient.getFullName()
                                            ),
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Test: " +
                                            nullSafe(
                                                    labRequest.getTestName()
                                            ),
                                    HEADING
                            )
                    );

                    String doctorName = "-";

                    if (labRequest.getDoctor() != null) {
                        doctorName =
                                nullSafe(
                                        labRequest
                                                .getDoctor()
                                                .getFullName()
                                );
                    }

                    document.add(
                            new Paragraph(
                                    "Requested by: Dr. " +
                                            doctorName,
                                    BODY
                            )
                    );

                    String laboratory = "-";

                    if (labRequest.getLabVendor() != null) {
                        laboratory =
                                nullSafe(
                                        labRequest
                                                .getLabVendor()
                                                .getFullName()
                                );
                    }

                    document.add(
                            new Paragraph(
                                    "Laboratory: " +
                                            laboratory,
                                    BODY
                            )
                    );

                    document.add(
                            new Paragraph(
                                    "Status: " +
                                            enumSafe(
                                                    labRequest.getStatus()
                                            ),
                                    BODY
                            )
                    );

                    document.add(new Paragraph(" "));

                    document.add(
                            new Paragraph(
                                    "Findings",
                                    HEADING
                            )
                    );

                    document.add(
                            new Paragraph(
                                    nullSafe(
                                            labRequest.getReportResult()
                                    ),
                                    BODY
                            )
                    );
                }
        );
    }


    // ============================================================
    // COMMON PDF BUILDER
    // ============================================================

    private byte[] buildPdf(
            String title,
            PdfConsumer consumer) {

        try (ByteArrayOutputStream out =
                     new ByteArrayOutputStream()) {

            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    out
            );

            document.open();

            document.addTitle(title);

            document.add(
                    new Paragraph(
                            "SmartCare 360 Hospital",
                            TITLE
                    )
            );

            document.add(
                    new Paragraph(
                            "Hospital Management System",
                            BODY
                    )
            );

            document.add(new Paragraph(" "));

            consumer.accept(document);

            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to generate PDF",
                    e
            );
        }
    }


    // ============================================================
    // NULL SAFE HELPERS
    // ============================================================

    private String nullSafe(String value) {

        return value != null && !value.trim().isEmpty()
                ? value
                : "-";
    }


    private String enumSafe(Object value) {

        return value != null
                ? value.toString()
                : "-";
    }


    private String formatDate(
            java.time.LocalDateTime date) {

        return date != null
                ? date.format(DATE_FORMAT)
                : "-";
    }


    private String formatDateTime(
            java.time.LocalDateTime date) {

        return date != null
                ? date.format(DATE_TIME_FORMAT)
                : "-";
    }


    @FunctionalInterface
    private interface PdfConsumer {

        void accept(Document document)
                throws DocumentException;
    }
}