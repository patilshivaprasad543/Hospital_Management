package com.hospital.service;

import com.hospital.dto.AppointmentSlot;
import com.hospital.model.*;
import com.hospital.repository.AppointmentRepository;
import com.hospital.repository.DoctorLeaveRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorScheduleService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    @Autowired
    private UserService userService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorLeaveRepository doctorLeaveRepository;

    public void validateBooking(User doctor, LocalDate date, LocalTime time) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        if (date.isBefore(today)) {
            throw new RuntimeException("Cannot book appointments for past dates.");
        }
        if (date.equals(today) && time.isBefore(now)) {
            throw new RuntimeException("Cannot book past time slots for today.");
        }

        DoctorProfile profile = userService.getDoctorProfile(doctor)
                .orElseThrow(() -> new RuntimeException("Doctor profile not found."));

        if (!isWorkingDay(profile, date.getDayOfWeek())) {
            throw new RuntimeException("Doctor is not available on this day.");
        }

        if (doctorLeaveRepository.existsByDoctorAndLeaveDate(doctor, date)) {
            throw new RuntimeException("Doctor is on leave for the selected date.");
        }

        List<AppointmentSlot> slots = getAvailableSlots(doctor, date);
        boolean slotValid = slots.stream()
                .anyMatch(s -> s.getTime().equals(time) && s.isAvailable());
        if (!slotValid) {
            throw new RuntimeException("Selected time slot is not available.");
        }

        long dailyCount = appointmentRepository.countByDoctorAndAppointmentDateAndStatusIn(
                doctor, date, Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));
        if (profile.getMaxAppointmentsPerDay() != null && dailyCount >= profile.getMaxAppointmentsPerDay()) {
            throw new RuntimeException("Doctor has reached maximum appointments for this date.");
        }
    }

    public List<AppointmentSlot> getAvailableSlots(User doctor, LocalDate date) {
        List<AppointmentSlot> slots = new ArrayList<>();
        DoctorProfile profile = userService.getDoctorProfile(doctor).orElse(null);
        if (profile == null || date.isBefore(LocalDate.now())) {
            return slots;
        }

        if (!isWorkingDay(profile, date.getDayOfWeek())) {
            return slots;
        }
        if (doctorLeaveRepository.existsByDoctorAndLeaveDate(doctor, date)) {
            return slots;
        }

        int duration = profile.getSlotDurationMinutes() != null ? profile.getSlotDurationMinutes() : 30;
        LocalTime start = LocalTime.parse(profile.getWorkStartTime() != null ? profile.getWorkStartTime() : "09:00");
        LocalTime end = LocalTime.parse(profile.getWorkEndTime() != null ? profile.getWorkEndTime() : "17:00");
        LocalTime now = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());

        Set<LocalTime> booked = appointmentRepository
                .findByDoctorAndAppointmentDateAndStatusIn(doctor, date,
                        Arrays.asList(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED))
                .stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        LocalTime slot = start;
        while (slot.isBefore(end)) {
            boolean pastSlot = isToday && slot.isBefore(now);
            boolean taken = booked.contains(slot);
            slots.add(new AppointmentSlot(slot, slot.format(TIME_FMT), !pastSlot && !taken));
            slot = slot.plusMinutes(duration);
        }
        return slots;
    }

    public boolean isDateBookable(User doctor, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return false;
        }
        DoctorProfile profile = userService.getDoctorProfile(doctor).orElse(null);
        if (profile == null || !isWorkingDay(profile, date.getDayOfWeek())) {
            return false;
        }
        if (doctorLeaveRepository.existsByDoctorAndLeaveDate(doctor, date)) {
            return false;
        }
        return getAvailableSlots(doctor, date).stream().anyMatch(AppointmentSlot::isAvailable);
    }

    private boolean isWorkingDay(DoctorProfile profile, DayOfWeek day) {
        String days = profile.getWorkingDays() != null ? profile.getWorkingDays() : "MON,TUE,WED,THU,FRI,SAT";
        return days.toUpperCase().contains(day.name().substring(0, 3));
    }

    public DoctorLeave addLeave(User doctor, LocalDate date, String reason) {
        if (date.isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot mark leave for past dates.");
        }
        if (doctorLeaveRepository.existsByDoctorAndLeaveDate(doctor, date)) {
            throw new RuntimeException("Leave already marked for this date.");
        }
        return doctorLeaveRepository.save(new DoctorLeave(doctor, date, reason));
    }

    public List<DoctorLeave> getUpcomingLeaves(User doctor) {
        return doctorLeaveRepository.findByDoctorOrderByLeaveDateDesc(doctor).stream()
                .filter(l -> !l.getLeaveDate().isBefore(LocalDate.now()))
                .toList();
    }

    public void removeLeave(Long leaveId, User doctor) {
        doctorLeaveRepository.findById(leaveId).ifPresent(leave -> {
            if (!leave.getDoctor().getId().equals(doctor.getId())) {
                throw new RuntimeException("Unauthorized.");
            }
            doctorLeaveRepository.delete(leave);
        });
    }
}
