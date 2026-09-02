package com.hospital.model;

public class VideoSignalingPayload {
    private String type; // offer, answer, candidate, join, leave, start-call, end-call
    private Object payload;
    private String senderId;
    private String senderName;
    private String senderRole;

    public VideoSignalingPayload() {}

    public VideoSignalingPayload(String type, Object payload, String senderId, String senderName, String senderRole) {
        this.type = type;
        this.payload = payload;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderRole = senderRole;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
}
