package com.hospital.service;

import com.hospital.model.Appointment;
import com.hospital.model.Consultation;
import com.hospital.model.User;
import com.hospital.repository.ConsultationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ConsultationService {

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private AuditLogService auditLogService;

    public Consultation startConsultation(Appointment appointment, User doctor) {
        Optional<Consultation> existing = consultationRepository.findByAppointmentId(appointment.getId());
        if (existing.isPresent()) {
            return existing.get();
        }
        Consultation consultation = new Consultation();
        consultation.setAppointment(appointment);
        consultation.setDoctor(doctor);
        consultation.setPatient(appointment.getPatient());
        consultation.setStartedAt(LocalDateTime.now());
        Consultation saved = consultationRepository.save(consultation);
        auditLogService.log(doctor, "CONSULTATION_STARTED", "CONSULTATION",
                "Appointment", appointment.getId(), "Consultation started for " + appointment.getPatient().getFullName());
        return saved;
    }

    public Consultation completeConsultation(Long consultationId, String symptoms, String diagnosis,
                                             String treatment, String notes, LocalDate followUpDate, User doctor) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new RuntimeException("Consultation not found"));
        consultation.setSymptoms(symptoms);
        consultation.setDiagnosis(diagnosis);
        consultation.setTreatment(treatment);
        consultation.setNotes(notes);
        consultation.setFollowUpDate(followUpDate);
        consultation.setCompletedAt(LocalDateTime.now());
        Consultation saved = consultationRepository.save(consultation);
        auditLogService.log(doctor, "CONSULTATION_COMPLETED", "CONSULTATION",
                "Consultation", consultationId, "Diagnosis: " + diagnosis);
        return saved;
    }

    public List<Consultation> getPatientConsultations(User patient) {
        return consultationRepository.findByPatientOrderByStartedAtDesc(patient);
    }

    public List<Consultation> getDoctorConsultations(User doctor) {
        return consultationRepository.findByDoctorOrderByStartedAtDesc(doctor);
    }

    public Optional<Consultation> findByAppointment(Long appointmentId) {
        return consultationRepository.findByAppointmentId(appointmentId);
    }
}
