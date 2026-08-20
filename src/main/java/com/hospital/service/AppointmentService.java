package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    public Appointment bookAppointment(Long patientId, Long doctorId, LocalDate date, LocalTime time, String reason) {
        return bookAppointmentWithDepartment(patientId, doctorId, date, time, reason, "General Consultation");
    }

    public Appointment bookAppointmentWithDepartment(Long patientId, Long doctorId, LocalDate date, LocalTime time, String reason, String departmentCategory) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        if (!doctor.isAdminApproved() || doctor.getApprovalStatus() != com.hospital.model.ApprovalStatus.APPROVED) {
            throw new RuntimeException("This doctor is not yet approved for appointments.");
        }

        doctorScheduleService.validateBooking(doctor, date, time);

        boolean slotTaken = appointmentRepository.existsByDoctorAndAppointmentDateAndAppointmentTimeAndStatusIn(
                doctor, date, time,
                Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));
        if (slotTaken) {
            throw new RuntimeException("This time slot is already booked. Please select another slot.");
        }

        Appointment appointment = new Appointment(patient, doctor, date, time, reason);
        appointment.setDepartmentCategory(departmentCategory != null ? departmentCategory : "General Consultation");
        appointment.setState(AppointmentState.PENDING_DOCTOR_APPROVAL);
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);

        // Notify doctor + patient via in-app, email & WhatsApp
        notificationService.sendNotification(
            doctor,
            "📅 New Appointment Request",
            "Patient " + patient.getFullName() + " requested an appointment for " + date + " at " + time
                    + " (" + departmentCategory + "). Reason: " + (reason != null ? reason : "General"),
            NotificationCategory.APPOINTMENT,
            "/doctor/dashboard"
        );

        notificationService.sendNotification(
            patient,
            "📩 Appointment Request Created",
            "Your appointment with Dr. " + doctor.getFullName() + " on " + date + " at " + time
                    + " is pending doctor approval.",
            NotificationCategory.APPOINTMENT,
            "/patient/appointments"
        );

        emailService.sendAppointmentBookedEmail(saved);

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
            notificationService.sendNotification(
                savedAppointment.getPatient(),
                "✅ Appointment Confirmed!",
                "Dr. " + savedAppointment.getDoctor().getFullName() + " confirmed your appointment on "
                        + savedAppointment.getAppointmentDate() + " at " + savedAppointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
            );
            notificationService.sendNotification(
                savedAppointment.getDoctor(),
                "✅ Appointment Confirmed",
                "You confirmed the appointment with " + savedAppointment.getPatient().getFullName()
                        + " on " + savedAppointment.getAppointmentDate() + " at " + savedAppointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/doctor/dashboard"
            );
            emailService.sendAppointmentConfirmationEmail(savedAppointment);
            userService.getDoctorProfile(savedAppointment.getDoctor()).ifPresent(profile -> {
                if (profile.getConsultationFee() != null && profile.getConsultationFee() > 0) {
                    billingService.createInvoice(
                            savedAppointment.getPatient(),
                            "CONSULTATION",
                            "Consultation with Dr. " + savedAppointment.getDoctor().getFullName(),
                            profile.getConsultationFee(),
                            savedAppointment.getId());
                }
            });
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

    public long countTodayAppointments() {
        return appointmentRepository.countByAppointmentDate(LocalDate.now());
    }

    public long countCompletedConsultations() {
        return appointmentRepository.countByStatus(AppointmentStatus.COMPLETED);
    }

    public Appointment cancelAppointment(Long appointmentId, User patient) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized cancellation.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.REJECTED) {
            throw new RuntimeException("This appointment cannot be cancelled.");
        }
        if (appointment.getState() == AppointmentState.CHECKED_IN
                || appointment.getState() == AppointmentState.IN_CONSULTATION) {
            throw new RuntimeException("Cannot cancel after check-in. Contact the hospital desk.");
        }

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setState(AppointmentState.CANCELLED);
        appointment.setNotes("Cancelled by patient");
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendNotification(
                appointment.getDoctor(),
                "Appointment Cancelled",
                "Patient " + patient.getFullName() + " cancelled the appointment on "
                        + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/doctor/dashboard"
        );
        notificationService.sendNotification(
                patient,
                "Appointment Cancelled",
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " has been cancelled.",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
        );
        return saved;
    }
}
