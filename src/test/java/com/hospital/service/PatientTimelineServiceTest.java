package com.hospital.service;

import com.hospital.dto.TimelineEvent;
import com.hospital.model.Appointment;
import com.hospital.model.LabRequest;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientTimelineServiceTest {

    @Mock private AppointmentService appointmentService;
    @Mock private ConsultationService consultationService;
    @Mock private PrescriptionService prescriptionService;
    @Mock private LabWorkflowService labWorkflowService;

    @InjectMocks
    private PatientTimelineService patientTimelineService;

    @Test
    void buildTimelineMergesAppointmentsPrescriptionsAndLabs() {
        User patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "x", Role.PATIENT);
        User doctor = new User("Dr. Sarah Jenkins", "sarah.jenkins@smartcare360.com", "9876543211", "x", Role.DOCTOR);

        Appointment appointment = new Appointment(patient, doctor, LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), "Fever");
        Prescription prescription = new Prescription();
        prescription.setDoctor(doctor);
        prescription.setPatient(patient);
        prescription.setDiagnosis("Viral fever");
        prescription.addItem(new PrescriptionItem("Paracetamol 650mg", "650mg", "1-0-1", "3 Days", ""));
        LabRequest lab = new LabRequest(doctor, patient, "Complete Blood Count", "Routine");

        when(appointmentService.getPatientAppointments(patient)).thenReturn(List.of(appointment));
        when(consultationService.getPatientConsultations(patient)).thenReturn(List.of());
        when(prescriptionService.getPatientPrescriptions(patient)).thenReturn(List.of(prescription));
        when(labWorkflowService.getPatientLabRequests(patient)).thenReturn(List.of(lab));

        List<TimelineEvent> events = patientTimelineService.buildTimeline(patient);

        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(e -> "APPOINTMENT".equals(e.getType())));
        assertTrue(events.stream().anyMatch(e -> "PRESCRIPTION".equals(e.getType())));
        assertTrue(events.stream().anyMatch(e -> "LAB".equals(e.getType())));
    }
}
