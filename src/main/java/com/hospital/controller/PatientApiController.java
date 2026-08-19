package com.hospital.controller;

import com.hospital.dto.AppointmentSlot;
import com.hospital.service.DoctorScheduleService;
import com.hospital.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/patient/api")
public class PatientApiController {

    @Autowired
    private DoctorScheduleService doctorScheduleService;

    @Autowired
    private UserService userService;

    @GetMapping("/slots")
    public List<AppointmentSlot> getSlots(@RequestParam Long doctorId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return userService.findById(doctorId)
                .map(doctor -> doctorScheduleService.getAvailableSlots(doctor, date))
                .orElse(List.of());
    }
}
