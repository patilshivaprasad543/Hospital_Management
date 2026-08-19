package com.hospital.dto;

import com.hospital.model.Appointment;
import com.hospital.model.AppointmentState;

import java.util.ArrayList;
import java.util.List;

public class CareCaseWorkflow {

    private Long appointmentId;
    private String patientName;
    private String doctorName;
    private String department;
    private String appointmentDate;
    private String appointmentTime;
    private AppointmentState currentState;
    private int progressPercent;
    private String currentStepLabel;
    private List<WorkflowStep> steps = new ArrayList<>();

    public CareCaseWorkflow() {
    }

    public CareCaseWorkflow(Appointment appointment) {
        this.appointmentId = appointment.getId();
        this.patientName = appointment.getPatient().getFullName();
        this.doctorName = appointment.getDoctor().getFullName();
        this.department = appointment.getDepartmentCategory() != null
                ? appointment.getDepartmentCategory()
                : "General Consultation";
        this.appointmentDate = appointment.getAppointmentDate().toString();
        this.appointmentTime = appointment.getAppointmentTime().toString();
        this.currentState = appointment.getState();
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public AppointmentState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(AppointmentState currentState) {
        this.currentState = currentState;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getCurrentStepLabel() {
        return currentStepLabel;
    }

    public void setCurrentStepLabel(String currentStepLabel) {
        this.currentStepLabel = currentStepLabel;
    }

    public List<WorkflowStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStep> steps) {
        this.steps = steps;
    }

    public void addStep(WorkflowStep step) {
        this.steps.add(step);
    }
}
