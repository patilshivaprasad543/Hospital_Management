package com.hospital.dto;

public class EpisodeStepDto {

    private String id;
    private String label;
    private String description;
    private String icon;
    private EpisodeStepStatus status;
    private String detail;
    private String actionUrl;
    private String actionLabel;

    public EpisodeStepDto() {
    }

    public EpisodeStepDto(String id, String label, String description, String icon,
                          EpisodeStepStatus status, String detail, String actionUrl, String actionLabel) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.icon = icon;
        this.status = status;
        this.detail = detail;
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public EpisodeStepStatus getStatus() { return status; }
    public void setStatus(EpisodeStepStatus status) { this.status = status; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
}
