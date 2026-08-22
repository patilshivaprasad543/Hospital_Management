package com.hospital.dto;

public class ChecklistItemDto {

    private String key;
    private String label;
    private String description;
    private String actionUrl;
    private String actionLabel;
    private boolean completed;
    private boolean autoCompleted;

    public ChecklistItemDto() {
    }

    public ChecklistItemDto(String key, String label, String description,
                            String actionUrl, String actionLabel, boolean completed, boolean autoCompleted) {
        this.key = key;
        this.label = label;
        this.description = description;
        this.actionUrl = actionUrl;
        this.actionLabel = actionLabel;
        this.completed = completed;
        this.autoCompleted = autoCompleted;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getActionUrl() { return actionUrl; }
    public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public boolean isAutoCompleted() { return autoCompleted; }
    public void setAutoCompleted(boolean autoCompleted) { this.autoCompleted = autoCompleted; }
}
