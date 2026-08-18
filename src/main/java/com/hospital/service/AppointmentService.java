package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDate date, LocalTime time, String reason) {
        return bookAppointmentWithDepartment(patientId, doctorId, date, time, reason, "General Consultation");
    }

    public Appointment bookAppointmentWithDepartment(Long patientId, Long doctorId, LocalDate date, LocalTime time, String reason, String departmentCategory) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Appointment appointment = new Appointment(patient, doctor, date, time, reason);
        appointment.setDepartmentCategory(departmentCategory != null ? departmentCategory : "General Consultation");
        appointment.setState(AppointmentState.PENDING_DOCTOR_APPROVAL);
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);

        // Send notifications
        notificationService.sendNotification(
            doctor,
            "📅 New Appointment Request",
            "Patient " + patient.getFullName() + " requested an appointment for " + date + " at " + time + " (" + departmentCategory + ").",
            NotificationCategory.APPOINTMENT,
            "/doctor/dashboard"
        );

        notificationService.sendNotification(
            patient,
            "📩 Appointment Request Created",
            "Your appointment request with Dr. " + doctor.getFullName() + " has been submitted and is pending approval.",
            NotificationCategory.APPOINTMENT,
            "/patient/appointments"
        );

        return saved;
    }

    public Appointment checkInPatient(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (appointment.getQueueTicket() == null) {
            long totalCheckedIn = appointmentRepository.count();
            String queueTicket = "#A" + (101 + (totalCheckedIn % 900));
            appointment.setQueueTicket(queueTicket);
        }
        appointment.setState(AppointmentState.CHECKED_IN);
        appointment.setCheckedInAt(LocalDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendNotification(
            appointment.getDoctor(),
            "🧑‍🦽 Patient Checked In (" + saved.getQueueTicket() + ")",
            "Patient " + saved.getPatient().getFullName() + " has arrived and checked in. Ticket: " + saved.getQueueTicket(),
            NotificationCategory.APPOINTMENT,
            "/doctor/dashboard"
        );

        notificationService.sendNotification(
            saved.getPatient(),
            "🎟️ Digital Queue Ticket Issued",
            "You have successfully checked in! Your Queue Ticket number is " + saved.getQueueTicket() + ". Please wait to be called.",
            NotificationCategory.APPOINTMENT,
            "/patient/appointments"
        );

        return saved;
    }

    public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus, String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        AppointmentStatus previousStatus = appointment.getStatus();
        appointment.setStatus(newStatus);
        if (notes != null && !notes.trim().isEmpty()) {
            appointment.setNotes(notes);
        }

        if (newStatus == AppointmentStatus.CONFIRMED) {
            appointment.setState(AppointmentState.CONFIRMED);
        } else if (newStatus == AppointmentStatus.REJECTED) {
            appointment.setState(AppointmentState.REJECTED);
        } else if (newStatus == AppointmentStatus.COMPLETED) {
            appointment.setState(AppointmentState.COMPLETED);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Trigger email & notification when confirmed
        if (newStatus == AppointmentStatus.CONFIRMED && previousStatus != AppointmentStatus.CONFIRMED) {
            emailService.sendAppointmentConfirmationEmail(savedAppointment);
            notificationService.sendNotification(
                savedAppointment.getPatient(),
                "✅ Appointment Confirmed!",
                "Dr. " + savedAppointment.getDoctor().getFullName() + " has confirmed your appointment for " + savedAppointment.getAppointmentDate() + " at " + savedAppointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
            );
        } else if (newStatus == AppointmentStatus.REJECTED) {
            notificationService.sendNotification(
                savedAppointment.getPatient(),
                "❌ Appointment Rejected",
                "Your appointment request with Dr. " + savedAppointment.getDoctor().getFullName() + " could not be confirmed.",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
            );
        }

        return savedAppointment;
    }

    public List<Appointment> getPatientAppointments(User patient) {
        return appointmentRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<Appointment> getDoctorAppointments(User doctor) {
        return appointmentRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }

    public List<Appointment> getDoctorAppointmentsByStatus(User doctor, AppointmentStatus status) {
        return appointmentRepository.findByDoctorAndStatusOrderByCreatedAtDesc(doctor, status);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> findById(Long id) {
        return appointmentRepository.findById(id);
    }

    public long countTotalAppointments() {
        return appointmentRepository.count();
    }

    public long countPendingAppointments() {
        return appointmentRepository.countByStatus(AppointmentStatus.PENDING);
    }
}
