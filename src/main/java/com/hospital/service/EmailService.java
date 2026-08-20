package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.service.mail.MailTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private MailTransport mailTransport;

    @Async
    public void sendNotificationEmail(String recipientEmail, String recipientName, String subject, String body) {
        String fullBody = String.format("Dear %s,\n\n%s\n\n— SmartCare 360 Hospital Team",
                recipientName != null ? recipientName : "User", body);
        sendEmail(recipientEmail, subject, fullBody, "NOTIFICATION");
    }

    @Async
    public void sendAppointmentConfirmationEmail(Appointment appointment) {
        String patientEmail = appointment.getPatient().getEmail();
        String patientName = appointment.getPatient().getFullName();
        String doctorName = appointment.getDoctor().getFullName();
        String date = appointment.getAppointmentDate().toString();
        String time = appointment.getAppointmentTime().toString();
        String status = appointment.getStatus().name();
        String details = appointment.getReason() != null ? appointment.getReason() : "Regular Consultation";

        String subject = "SmartCare 360 - Appointment Confirmed";

        String body = String.format(
            "Dear %s,\n\n" +
            "Your appointment has been successfully confirmed!\n\n" +
            "Appointment Details:\n" +
            "-----------------------------------\n" +
            "Doctor Name        : Dr. %s\n" +
            "Appointment Date   : %s\n" +
            "Appointment Time   : %s\n" +
            "Appointment Status : %s\n" +
            "Details            : %s\n" +
            "-----------------------------------\n\n" +
            "Please arrive 15 minutes prior to your scheduled time.\n\n" +
            "Best Regards,\n" +
            "SmartCare 360 Team",
            patientName, doctorName, date, time, status, details
        );

        sendEmail(patientEmail, subject, body, "APPOINTMENT CONFIRMATION");
    }

    @Async
    public void sendAppointmentBookedEmail(Appointment appointment) {
        String patientName = appointment.getPatient().getFullName();
        String doctorName = appointment.getDoctor().getFullName();
        String date = appointment.getAppointmentDate().toString();
        String time = appointment.getAppointmentTime().toString();

        String patientBody = String.format(
                "Dear %s,\n\nYour appointment request with Dr. %s on %s at %s has been submitted.\n" +
                "Status: PENDING doctor approval.\n\n— SmartCare 360",
                patientName, doctorName, date, time);
        sendEmail(appointment.getPatient().getEmail(),
                "SmartCare 360 - Appointment Request Submitted", patientBody, "APPOINTMENT BOOKED (PATIENT)");

        String doctorBody = String.format(
                "Dear Dr. %s,\n\nPatient %s has requested an appointment on %s at %s.\n" +
                "Reason: %s\nPlease review and accept/reject in your dashboard.\n\n— SmartCare 360",
                doctorName, patientName, date, time,
                appointment.getReason() != null ? appointment.getReason() : "General consultation");
        sendEmail(appointment.getDoctor().getEmail(),
                "SmartCare 360 - New Appointment Request", doctorBody, "APPOINTMENT BOOKED (DOCTOR)");
    }

    @Async
    public void sendWelcomeEmail(String recipientEmail, String fullName, String message) {
        String subject = "SmartCare 360 - Account Created";
        String body = String.format(
                "Dear %s,\n\n%s\n\nYou can sign in anytime at the SmartCare 360 portal using this email and your password.\n\n— SmartCare 360 Team",
                fullName != null ? fullName : "User",
                message != null ? message : "Your account has been created.");
        sendEmail(recipientEmail, subject, body, "WELCOME");
    }

    public boolean sendOtpEmail(String recipientEmail, String otpCode) {
        String subject = "SmartCare 360 - Email Verification OTP";
        String body = String.format(
                "Dear User,\n\n" +
                "Thank you for registering with SmartCare 360.\n\n" +
                "Your email verification OTP is: %s\n\n" +
                "This OTP is valid for 10 minutes.\n" +
                "Enter this code on the verification page to activate your account.\n" +
                "Do not share this OTP with anyone.\n\n" +
                "If you did not register, please ignore this email.\n\n" +
                "— SmartCare 360 Team",
                otpCode);

        return sendEmail(recipientEmail, subject, body, "OTP");
    }

    public boolean sendPasswordResetEmail(String recipientEmail, String resetOtp) {
        String subject = "SmartCare 360 - Password Reset OTP";
        String body = String.format(
                "Dear User,\n\nYour password reset OTP is: %s\n\n" +
                "This OTP is valid for 10 minutes. Do not share it with anyone.\n\n— SmartCare 360 Team",
                resetOtp);

        return sendEmail(recipientEmail, subject, body, "PASSWORD RESET");
    }

    @Async
    public void sendApprovalEmail(String recipientEmail, String fullName, boolean approved) {
        String subject = approved
                ? "SmartCare 360 - Account Approved"
                : "SmartCare 360 - Account Application Update";
        String body = approved
                ? String.format("Dear %s,\n\nYour SmartCare 360 account has been approved by the administrator. You can now log in.", fullName)
                : String.format("Dear %s,\n\nYour SmartCare 360 account application was not approved. Please contact the hospital administrator for details.", fullName);

        sendEmail(recipientEmail, subject, body, "APPROVAL");
    }

    public boolean isSmtpConfigured() {
        return mailTransport.isConfigured();
    }

    private boolean sendEmail(String to, String subject, String body, String type) {
        logger.info("\n=======================================================");
        logger.info("EMAIL [{} via {}] TO: {}", type, mailTransport.providerName(), to);
        logger.info("SUBJECT: {}", subject);
        logger.info("BODY:\n{}", body);
        logger.info("=======================================================\n");
        return mailTransport.send(to, subject, body, type);
    }
}
