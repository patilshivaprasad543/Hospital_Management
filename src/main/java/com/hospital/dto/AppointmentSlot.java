package com.hospital.dto;

import java.time.LocalTime;

public class AppointmentSlot {

    private LocalTime time;
    private String label;
    private boolean available;

    public AppointmentSlot() {
    }

    public AppointmentSlot(LocalTime time, String label, boolean available) {
        this.time = time;
        this.label = label;
        this.available = available;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
