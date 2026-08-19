package com.hospital.dto;

public class WorkflowStep {

    private String id;
    private String label;
    private String description;
    private String icon;
    private WorkflowStepStatus status;
    private String actionUrl;
    private String actionLabel;
    private String detail;

    public WorkflowStep() {
    }

    public WorkflowStep(String id, String label, String description, String icon,
                        WorkflowStepStatus status, String actionUrl, String actionLabel, String detail) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.status = status;
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
        this.detail = detail;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public WorkflowStepStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStepStatus status) {
        this.status = status;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
