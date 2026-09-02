package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.AmbulanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/patient/ambulance")
public class AmbulanceController {

    @Autowired
    private AmbulanceService ambulanceService;

    private User getLoggedInPatient(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/"})
    public String ambulanceHome(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }
        List<AmbulanceTrip> trips = ambulanceService.getPatientTrips(patient);
        model.addAttribute("trips", trips);
        model.addAttribute("availableAmbulances", ambulanceService.getAvailableAmbulances());
        model.addAttribute("loggedInUser", patient);
        return "patient/ambulance";
    }

    @GetMapping("/current")
    public String currentTrip(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }
        List<AmbulanceTrip> trips = ambulanceService.getPatientTrips(patient);
        AmbulanceTrip activeTrip = trips.stream()
                .filter(t -> t.getStatus() != AmbulanceTripStatus.COMPLETED && t.getStatus() != AmbulanceTripStatus.CANCELLED)
                .findFirst().orElse(null);

        model.addAttribute("activeTrip", activeTrip);
        model.addAttribute("loggedInUser", patient);
        return "patient/ambulance-current";
    }

    @PostMapping("/book")
    public String bookAmbulance(@RequestParam("pickupAddress") String pickupAddress,
                                @RequestParam("destinationAddress") String destinationAddress,
                                @RequestParam(value = "priority", defaultValue = "MEDIUM") EmergencyPriority priority,
                                @RequestParam(value = "requestedType", defaultValue = "BASIC") AmbulanceType requestedType,
                                @RequestParam(value = "contactPerson", required = false) String contactPerson,
                                @RequestParam(value = "contactMobile", required = false) String contactMobile,
                                @RequestParam(value = "reason", required = false) String reason,
                                HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        ambulanceService.requestAmbulance(patient, pickupAddress, destinationAddress, priority,
                requestedType, contactPerson, contactMobile, reason);

        return "redirect:/patient/ambulance/current?success=true";
    }
}
