package com.hospital.service;

import com.hospital.dto.CareCaseWorkflow;
import com.hospital.dto.WorkflowStep;
import com.hospital.dto.WorkflowStepStatus;
import com.hospital.model.*;
import com.hospital.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class CareWorkflowService {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private PharmacyWorkflowService pharmacyWorkflowService;

    public List<CareCaseWorkflow> getPatientWorkflows(User patient) {
        List<Appointment> appointments = appointmentService.getPatientAppointments(patient);
        List<Prescription> prescriptions = prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
        List<LabRequest> labRequests = labWorkflowService.getPatientLabRequests(patient);
        List<PharmacyOrder> pharmacyOrders = pharmacyWorkflowService.getPatientOrders(patient);

        List<CareCaseWorkflow> workflows = new ArrayList<>();
        for (Appointment appointment : appointments) {
            workflows.add(buildWorkflowForAppointment(appointment, prescriptions, labRequests, pharmacyOrders, true));
        }
        return workflows;
    }

    public List<CareCaseWorkflow> getDoctorWorkflows(User doctor) {
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor);
        List<CareCaseWorkflow> workflows = new ArrayList<>();

        for (Appointment appointment : appointments) {
            if (appointment.getState() == AppointmentState.COMPLETED
                    || appointment.getState() == AppointmentState.REJECTED
                    || appointment.getState() == AppointmentState.CANCELLED) {
                continue;
            }
            User patient = appointment.getPatient();
            List<Prescription> prescriptions = prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
            List<LabRequest> labRequests = labWorkflowService.getPatientLabRequests(patient);
            List<PharmacyOrder> pharmacyOrders = pharmacyWorkflowService.getPatientOrders(patient);
            workflows.add(buildWorkflowForAppointment(appointment, prescriptions, labRequests, pharmacyOrders, false));
        }

        workflows.sort(Comparator.comparingInt(w -> priorityForState(w.getCurrentState())));
        return workflows;
    }

    public CareCaseWorkflow getWorkflowForAppointment(Long appointmentId, boolean patientView) {
        Appointment appointment = appointmentService.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        User patient = appointment.getPatient();
        List<Prescription> prescriptions = prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
        List<LabRequest> labRequests = labWorkflowService.getPatientLabRequests(patient);
        List<PharmacyOrder> pharmacyOrders = pharmacyWorkflowService.getPatientOrders(patient);
        return buildWorkflowForAppointment(appointment, prescriptions, labRequests, pharmacyOrders, patientView);
    }

    private int priorityForState(AppointmentState state) {
        return switch (state) {
            case IN_CONSULTATION -> 0;
            case CHECKED_IN -> 1;
            case CONFIRMED, ACCEPTED -> 2;
            case PENDING_DOCTOR_APPROVAL, REQUESTED -> 3;
            default -> 4;
        };
    }

    private CareCaseWorkflow buildWorkflowForAppointment(Appointment appointment,
                                                           List<Prescription> prescriptions,
                                                           List<LabRequest> labRequests,
                                                           List<PharmacyOrder> pharmacyOrders,
                                                           boolean patientView) {
        CareCaseWorkflow workflow = new CareCaseWorkflow(appointment);
        AppointmentState state = appointment.getState() != null
                ? appointment.getState()
                : AppointmentState.PENDING_DOCTOR_APPROVAL;

        Optional<Prescription> linkedRx = prescriptions.stream()
                .filter(rx -> rx.getAppointment() != null && rx.getAppointment().getId().equals(appointment.getId()))
                .findFirst();

        List<LabRequest> linkedLabs = labRequests.stream()
                .filter(lab -> !lab.getCreatedAt().isBefore(appointment.getCreatedAt()))
                .toList();

        List<PharmacyOrder> linkedOrders = linkedRx.map(rx -> pharmacyOrders.stream()
                .filter(order -> order.getPrescription() != null
                        && order.getPrescription().getId().equals(rx.getId()))
                .toList()).orElse(List.of());

        boolean rejected = state == AppointmentState.REJECTED || state == AppointmentState.CANCELLED;
        boolean completed = state == AppointmentState.COMPLETED;

        workflow.addStep(step("book", "Book Appointment", "Appointment request submitted",
                "📅", WorkflowStepStatus.COMPLETED, null, null,
                "Dr. " + appointment.getDoctor().getFullName() + " · " + workflow.getAppointmentDate()));

        WorkflowStepStatus approvalStatus = rejected ? WorkflowStepStatus.FAILED
                : (state.ordinal() >= AppointmentState.CONFIRMED.ordinal()
                || state == AppointmentState.ACCEPTED ? WorkflowStepStatus.COMPLETED
                : (state == AppointmentState.PENDING_DOCTOR_APPROVAL ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING));
        workflow.addStep(step("approval", "Doctor Approval", "Waiting for doctor to confirm",
                "✅", approvalStatus,
                patientView ? "/patient/appointments" : "/doctor/dashboard",
                patientView ? "View Status" : "Review Request",
                rejected ? "Appointment was not approved" : (approvalStatus == WorkflowStepStatus.COMPLETED
                ? "Confirmed by Dr. " + appointment.getDoctor().getFullName() : "Pending doctor review")));

        if (!rejected) {
            WorkflowStepStatus checkInStatus = completed || state.ordinal() >= AppointmentState.CHECKED_IN.ordinal()
                    ? WorkflowStepStatus.COMPLETED
                    : (state == AppointmentState.CONFIRMED ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING);
            workflow.addStep(step("checkin", "Digital Check-In", "Arrive and get your queue ticket",
                    "🎟️", checkInStatus,
                    patientView && state == AppointmentState.CONFIRMED ? "/patient/appointments" : null,
                    patientView && state == AppointmentState.CONFIRMED ? "Check In Now" : null,
                    appointment.getQueueTicket() != null ? "Queue ticket: " + appointment.getQueueTicket() : null));

            WorkflowStepStatus consultStatus = completed || state == AppointmentState.IN_CONSULTATION
                    ? WorkflowStepStatus.COMPLETED
                    : (state == AppointmentState.CHECKED_IN ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING);
            workflow.addStep(step("consultation", "Doctor Consultation", "Meet with your doctor",
                    "🩺", consultStatus,
                    !patientView && state == AppointmentState.CHECKED_IN ? "/doctor/dashboard" : null,
                    !patientView && state == AppointmentState.CHECKED_IN ? "Start Consultation" : null,
                    state == AppointmentState.IN_CONSULTATION ? "Consultation in progress" : null));

            WorkflowStepStatus rxStatus = linkedRx.isPresent() ? WorkflowStepStatus.COMPLETED
                    : (state == AppointmentState.IN_CONSULTATION ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING);
            workflow.addStep(step("prescription", "Prescription", "Receive diagnosis and medicines",
                    "💊", rxStatus,
                    !patientView && state == AppointmentState.IN_CONSULTATION ? "/doctor/dashboard" : "/patient/prescriptions",
                    !patientView && state == AppointmentState.IN_CONSULTATION ? "Create Prescription" : "View Prescriptions",
                    linkedRx.map(rx -> "Diagnosis: " + rx.getDiagnosis()).orElse(null)));

            boolean hasLab = !linkedLabs.isEmpty();
            WorkflowStepStatus labStatus = !hasLab ? WorkflowStepStatus.SKIPPED
                    : linkedLabs.stream().allMatch(l -> "REPORT_READY".equals(l.getStatus()))
                    ? WorkflowStepStatus.COMPLETED
                    : (linkedLabs.stream().anyMatch(l -> "PROCESSING".equals(l.getStatus()))
                    ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING);
            workflow.addStep(step("lab", "Lab Tests", "Diagnostic tests if recommended",
                    "🔬", labStatus,
                    patientView ? "/patient/lab-reports" : "/doctor/dashboard",
                    hasLab ? "View Lab" : null,
                    hasLab ? summarizeLab(linkedLabs) : "No lab tests for this visit"));

            boolean hasPharmacy = !linkedOrders.isEmpty();
            WorkflowStepStatus pharmacyStatus = !hasPharmacy ? WorkflowStepStatus.SKIPPED
                    : linkedOrders.stream().allMatch(o -> "COMPLETED".equals(o.getStatus()))
                    ? WorkflowStepStatus.COMPLETED
                    : WorkflowStepStatus.ACTIVE;
            workflow.addStep(step("pharmacy", "Pharmacy", "Order medicines from prescription",
                    "🏪", pharmacyStatus,
                    patientView ? "/patient/prescriptions" : "/vendor/pharmacy-dashboard",
                    hasPharmacy ? "View Order" : (linkedRx.isPresent() && patientView ? "Order Medicines" : null),
                    hasPharmacy ? "Order status: " + linkedOrders.get(0).getStatus() : null));

            WorkflowStepStatus completeStatus = completed ? WorkflowStepStatus.COMPLETED
                    : (linkedRx.isPresent() ? WorkflowStepStatus.ACTIVE : WorkflowStepStatus.PENDING);
            workflow.addStep(step("complete", "Care Complete", "Visit wrapped up",
                    "🏁", completeStatus, null, null,
                    completed ? "This care episode is complete" : "Pending final completion"));
        }

        computeProgress(workflow);
        return workflow;
    }

    private String summarizeLab(List<LabRequest> labs) {
        if (labs.size() == 1) {
            return labs.get(0).getTestName() + " — " + labs.get(0).getStatus();
        }
        return labs.size() + " tests — latest: " + labs.get(0).getStatus();
    }

    private WorkflowStep step(String id, String label, String description, String icon,
                              WorkflowStepStatus status, String actionUrl, String actionLabel, String detail) {
        return new WorkflowStep(id, label, description, icon, status, actionUrl, actionLabel, detail);
    }

    private void computeProgress(CareCaseWorkflow workflow) {
        List<WorkflowStep> steps = workflow.getSteps();
        long countable = steps.stream().filter(s -> s.getStatus() != WorkflowStepStatus.SKIPPED).count();
        long done = steps.stream().filter(s -> s.getStatus() == WorkflowStepStatus.COMPLETED).count();
        workflow.setProgressPercent(countable == 0 ? 0 : (int) Math.round((done * 100.0) / countable));

        workflow.setCurrentStepLabel(steps.stream()
                .filter(s -> s.getStatus() == WorkflowStepStatus.ACTIVE)
                .map(WorkflowStep::getLabel)
                .findFirst()
                .orElse(steps.stream()
                        .filter(s -> s.getStatus() == WorkflowStepStatus.COMPLETED)
                        .reduce((first, second) -> second)
                        .map(WorkflowStep::getLabel)
                        .orElse("Not started")));
    }
}
