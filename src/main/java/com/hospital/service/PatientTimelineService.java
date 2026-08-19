package com.hospital.service;

import com.hospital.dto.TimelineEvent;
import com.hospital.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PatientTimelineService {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ConsultationService consultationService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private LabWorkflowService labWorkflowService;

    public List<TimelineEvent> buildTimeline(User patient) {
        List<TimelineEvent> events = new ArrayList<>();

        for (Appointment app : appointmentService.getPatientAppointments(patient)) {
            String desc = "Department: " + (app.getDepartmentCategory() != null ? app.getDepartmentCategory() : "General")
                    + " | Status: " + app.getStatus()
                    + (app.getReason() != null ? " | Reason: " + app.getReason() : "");
            events.add(new TimelineEvent(app.getCreatedAt(), "APPOINTMENT",
                    "Appointment with Dr. " + app.getDoctor().getFullName(), desc, "badge-confirmed"));
        }

        for (Consultation c : consultationService.getPatientConsultations(patient)) {
            if (c.getCompletedAt() != null) {
                String desc = "Diagnosis: " + (c.getDiagnosis() != null ? c.getDiagnosis() : "-")
                        + " | Treatment: " + (c.getTreatment() != null ? c.getTreatment() : "-");
                if (c.getFollowUpDate() != null) {
                    desc += " | Follow-up: " + c.getFollowUpDate();
                }
                events.add(new TimelineEvent(c.getCompletedAt(), "CONSULTATION",
                        "Consultation completed — Dr. " + c.getDoctor().getFullName(), desc, "badge-completed"));
            }
        }

        for (Prescription rx : prescriptionService.getPatientPrescriptions(patient)) {
            String meds = rx.getItems() != null ? rx.getItems().size() + " medicine(s)" : "0 medicines";
            events.add(new TimelineEvent(rx.getCreatedAt(), "PRESCRIPTION",
                    "Prescription from Dr. " + rx.getDoctor().getFullName(),
                    "Diagnosis: " + (rx.getDiagnosis() != null ? rx.getDiagnosis() : "-") + " | " + meds,
                    "badge-role"));
        }

        for (LabRequest lab : labWorkflowService.getPatientLabRequests(patient)) {
            String desc = "Status: " + lab.getStatus();
            if (lab.getReportResult() != null) {
                desc += " | Result available";
            }
            events.add(new TimelineEvent(lab.getCreatedAt(), "LAB",
                    "Lab test: " + lab.getTestName(), desc, "badge-pending"));
        }

        events.sort(Comparator.comparing(TimelineEvent::getOccurredAt).reversed());
        return events;
    }
}
