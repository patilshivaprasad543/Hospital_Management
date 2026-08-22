package com.hospital.dto;

public class QueueStatusDto {

    private boolean inQueue;
    private int position;
    private int totalWaiting;
    private int estimatedWaitMinutes;
    private int averageConsultMinutes;
    private String queueTicket;
    private String doctorName;

    public QueueStatusDto() {
    }

    public boolean isInQueue() { return inQueue; }
    public void setInQueue(boolean inQueue) { this.inQueue = inQueue; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public int getTotalWaiting() { return totalWaiting; }
    public void setTotalWaiting(int totalWaiting) { this.totalWaiting = totalWaiting; }
    public int getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }
    public int getAverageConsultMinutes() { return averageConsultMinutes; }
    public void setAverageConsultMinutes(int averageConsultMinutes) { this.averageConsultMinutes = averageConsultMinutes; }
    public String getQueueTicket() { return queueTicket; }
    public void setQueueTicket(String queueTicket) { this.queueTicket = queueTicket; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
}
