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
        notificationService.sendPortalNotification(
            doctor,
            "📅 New Appointment Request",
            "Patient " + patient.getFullName() + " requested an appointment for " + date + " at " + time
                    + " (" + departmentCategory + "). Reason: " + (reason != null ? reason : "General"),
            NotificationCategory.APPOINTMENT,
            "/doctor/dashboard"
        );

        notificationService.sendPortalNotification(
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

        notificationService.sendPortalNotification(
            appointment.getDoctor(),
            "🧑‍🦽 Patient Checked In (" + saved.getQueueTicket() + ")",
            "Patient " + saved.getPatient().getFullName() + " has arrived and checked in. Ticket: " + saved.getQueueTicket(),
            NotificationCategory.APPOINTMENT,
            "/doctor/dashboard"
        );

        notificationService.sendPortalNotification(
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
            notificationService.sendPortalNotification(
                savedAppointment.getPatient(),
                "✅ Appointment Confirmed!",
                "Dr. " + savedAppointment.getDoctor().getFullName() + " confirmed your appointment on "
                        + savedAppointment.getAppointmentDate() + " at " + savedAppointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
            );
            notificationService.sendPortalNotification(
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
            notificationService.sendPortalNotification(
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
                || appointment.getStatus() == AppointmentStatus.REJECTED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("This appointment cannot be cancelled.");
        }
        if (appointment.getState() == AppointmentState.CHECKED_IN
                || appointment.getState() == AppointmentState.IN_CONSULTATION) {
            throw new RuntimeException("Cannot cancel after check-in. Contact the hospital desk.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setState(AppointmentState.CANCELLED);
        appointment.setNotes("Cancelled by patient");
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendPortalNotification(
                appointment.getDoctor(),
                "Appointment Cancelled",
                "Patient " + patient.getFullName() + " cancelled the appointment on "
                        + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".",
                NotificationCategory.APPOINTMENT,
                "/doctor/dashboard"
        );
        notificationService.sendPortalNotification(
                patient,
                "Appointment Cancelled",
                "Your appointment with Dr. " + appointment.getDoctor().getFullName() + " has been cancelled.",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
        );
        return saved;
    }

    public Appointment rescheduleAppointment(Long appointmentId, User patient, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!appointment.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized reschedule.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.REJECTED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("This appointment cannot be rescheduled.");
        }
        if (appointment.getState() == AppointmentState.CHECKED_IN
                || appointment.getState() == AppointmentState.IN_CONSULTATION) {
            throw new RuntimeException("Cannot reschedule after check-in.");
        }

        doctorScheduleService.validateBooking(appointment.getDoctor(), newDate, newTime);
        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setState(AppointmentState.PENDING_DOCTOR_APPROVAL);
        appointment.setReminderSent(false);
        Appointment saved = appointmentRepository.save(appointment);

        notificationService.sendPortalNotification(
                appointment.getDoctor(),
                "Appointment Rescheduled",
                "Patient " + patient.getFullName() + " requested a new time: " + newDate + " at " + newTime + ".",
                NotificationCategory.APPOINTMENT,
                "/doctor/dashboard"
        );
        notificationService.sendPortalNotification(
                patient,
                "Appointment Rescheduled",
                "Your appointment with Dr. " + appointment.getDoctor().getFullName()
                        + " was moved to " + newDate + " at " + newTime + " and is pending doctor confirmation.",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
        );
        return saved;
    }

    public Appointment rescheduleByDoctor(Long appointmentId, User doctor, LocalDate newDate, LocalTime newTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!appointment.getDoctor().getId().equals(doctor.getId())) {
            throw new RuntimeException("Unauthorized reschedule.");
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED
                || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new RuntimeException("This appointment cannot be rescheduled.");
        }
        doctorScheduleService.validateBooking(doctor, newDate, newTime);
        appointment.setAppointmentDate(newDate);
        appointment.setAppointmentTime(newTime);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setState(AppointmentState.CONFIRMED);
        appointment.setReminderSent(false);
        Appointment saved = appointmentRepository.save(appointment);
        notificationService.sendPortalNotification(
                appointment.getPatient(),
                "Appointment Rescheduled by Doctor",
                "Dr. " + doctor.getFullName() + " moved your appointment to " + newDate + " at " + newTime + ".",
                NotificationCategory.APPOINTMENT,
                "/patient/appointments"
        );
        return saved;
    }

    public void sendDueReminders(User patient) {
        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        for (Appointment appointment : getPatientAppointments(patient)) {
            if (appointment.isReminderSent()) {
                continue;
            }
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                continue;
            }
            if (appointment.getAppointmentDate() == null) {
                continue;
            }
            if (appointment.getAppointmentDate().isAfter(tomorrow)) {
                continue;
            }
            if (appointment.getAppointmentDate().isBefore(java.time.LocalDate.now())) {
                continue;
            }
            notificationService.sendPortalNotification(
                    patient,
                    "Appointment Reminder",
                    "Reminder: consultation with Dr. " + appointment.getDoctor().getFullName()
                            + " on " + appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + ".",
                    NotificationCategory.APPOINTMENT,
                    "/patient/appointments"
            );
            appointment.setReminderSent(true);
            appointmentRepository.save(appointment);
        }
    }
}
