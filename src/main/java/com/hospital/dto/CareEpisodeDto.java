package com.hospital.dto;

import com.hospital.model.AppointmentState;
import com.hospital.model.AppointmentStatus;

import java.util.ArrayList;
import java.util.List;

public class CareEpisodeDto {

    private Long appointmentId;
    private String doctorName;
    private String department;
    private String appointmentDate;
    private String appointmentTime;
    private AppointmentState state;
    private AppointmentStatus status;
    private int progressPercent;
    private String currentStepLabel;
    private boolean active;
    private List<EpisodeStepDto> steps = new ArrayList<>();
    private List<ChecklistItemDto> checklist = new ArrayList<>();
    private QueueStatusDto queueStatus;

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }
    public AppointmentState getState() { return state; }
    public void setState(AppointmentState state) { this.state = state; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getCurrentStepLabel() { return currentStepLabel; }
    public void setCurrentStepLabel(String currentStepLabel) { this.currentStepLabel = currentStepLabel; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<EpisodeStepDto> getSteps() { return steps; }
    public void setSteps(List<EpisodeStepDto> steps) { this.steps = steps; }
    public List<ChecklistItemDto> getChecklist() { return checklist; }
    public void setChecklist(List<ChecklistItemDto> checklist) { this.checklist = checklist; }
    public QueueStatusDto getQueueStatus() { return queueStatus; }
    public void setQueueStatus(QueueStatusDto queueStatus) { this.queueStatus = queueStatus; }
}
