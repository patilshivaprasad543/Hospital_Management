package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.ConsultationFeedback;
import com.hospital.model.User;
import com.hospital.repository.ConsultationFeedbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    @Autowired
    private ConsultationFeedbackRepository feedbackRepository;

    @Autowired
    private NotificationService notificationService;

    public ConsultationFeedback submitFeedback(Appointment appointment, User patient, int rating, String comment) {
        if (appointment.getStatus() != com.hospital.model.AppointmentStatus.COMPLETED) {
            throw new RuntimeException("Feedback is only allowed for completed appointments.");
        }
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized feedback submission.");
        }
        if (feedbackRepository.existsByAppointmentId(appointment.getId())) {
            throw new RuntimeException("You have already rated this consultation.");
        }
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5.");
        }

        ConsultationFeedback feedback = new ConsultationFeedback();
        feedback.setAppointment(appointment);
        feedback.setPatient(patient);
        feedback.setDoctor(appointment.getDoctor());
        feedback.setRating(rating);
        feedback.setComment(comment);
        ConsultationFeedback saved = feedbackRepository.save(feedback);

        notificationService.sendPortalNotification(
                appointment.getDoctor(),
                "New Patient Rating",
                patient.getFullName() + " rated your consultation " + rating + "/5 stars.",
                com.hospital.model.NotificationCategory.SYSTEM,
                "/doctor/profile"
        );
        return saved;
    }

    public double getDoctorAverageRating(User doctor) {
        return feedbackRepository.averageRatingByDoctor(doctor);
    }

    public long getDoctorRatingCount(User doctor) {
        return feedbackRepository.countByDoctor(doctor);
    }

    public boolean hasFeedback(Long appointmentId) {
        return feedbackRepository.existsByAppointmentId(appointmentId);
    }
}
