package com.hospital.service;

import com.hospital.model.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Async
    public void sendAppointmentConfirmationEmail(Appointment appointment) {
        String patientEmail = appointment.getPatient().getEmail();
        String patientName = appointment.getPatient().getFullName();
        String doctorName = appointment.getDoctor().getFullName();
        String date = appointment.getAppointmentDate().toString();
        String time = appointment.getAppointmentTime().toString();
        String status = appointment.getStatus().name();
        String details = appointment.getReason() != null ? appointment.getReason() : "Regular Consultation";

        String subject = "Appointment Confirmation - Hospital Management";
        
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
            "Hospital Management Team",
            patientName, doctorName, date, time, status, details
        );

        logger.info("\n=======================================================");
        logger.info("SENDING APPOINTMENT CONFIRMATION EMAIL TO: {}", patientEmail);
        logger.info("SUBJECT: {}", subject);
        logger.info("BODY:\n{}", body);
        logger.info("=======================================================\n");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(patientEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                logger.info("Email successfully dispatched to {}", patientEmail);
            } catch (Exception e) {
                logger.warn("Could not send SMTP email to {}: {}. Logged email content to console.", patientEmail, e.getMessage());
            }
        }
    }

    @Async
    public void sendOtpEmail(String recipientEmail, String otpCode) {
        String subject = "Your Hospital Management Verification OTP";
        String body = "Dear User,\n\nYour OTP for verification is: " + otpCode + "\n\nDo not share this OTP with anyone.";
        
        logger.info("\n=======================================================");
        logger.info("SENDING OTP EMAIL TO: {}", recipientEmail);
        logger.info("OTP CODE: {}", otpCode);
        logger.info("=======================================================\n");

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipientEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
            } catch (Exception e) {
                logger.warn("Could not send SMTP OTP to {}: {}", recipientEmail, e.getMessage());
            }
        }
    }
}
