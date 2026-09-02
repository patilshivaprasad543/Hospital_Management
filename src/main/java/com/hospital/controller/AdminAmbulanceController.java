package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.AmbulanceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/ambulances")
public class AdminAmbulanceController {

    @Autowired
    private AmbulanceService ambulanceService;

    private User getLoggedInAdmin(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInAdmin(session);
    }

    @GetMapping({"", "/"})
    public String adminAmbulances(HttpSession session, Model model) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        model.addAttribute("ambulances", ambulanceService.getAllAmbulances());
        model.addAttribute("trips", ambulanceService.getAllTrips());
        model.addAttribute("availableAmbulances", ambulanceService.getAvailableAmbulances());
        model.addAttribute("loggedInUser", admin);
        return "admin/ambulances";
    }

    @PostMapping("/add")
    public String addAmbulance(@RequestParam("vehicleNumber") String vehicleNumber,
                               @RequestParam("type") AmbulanceType type,
                               @RequestParam("driverName") String driverName,
                               @RequestParam("driverContact") String driverContact,
                               @RequestParam(value = "basePrice", required = false, defaultValue = "1500.0") Double basePrice,
                               @RequestParam(value = "equipmentList", required = false) String equipmentList,
                               HttpSession session) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        Ambulance ambulance = new Ambulance(vehicleNumber, type, driverName, driverContact, basePrice);
        if (equipmentList != null && !equipmentList.isBlank()) {
            ambulance.setEquipmentList(equipmentList);
        }
        ambulanceService.saveAmbulance(ambulance);

        return "redirect:/admin/ambulances?added=true";
    }

    @PostMapping("/assign")
    public String assignAmbulance(@RequestParam("tripId") Long tripId,
                                  @RequestParam("ambulanceId") Long ambulanceId,
                                  HttpSession session) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        ambulanceService.assignAmbulance(tripId, ambulanceId, admin);
        return "redirect:/admin/ambulances?assigned=true";
    }

    @PostMapping("/status")
    public String updateTripStatus(@RequestParam("tripId") Long tripId,
                                   @RequestParam("status") AmbulanceTripStatus status,
                                   HttpSession session) {
        User admin = getLoggedInAdmin(session);
        if (admin == null) {
            return "redirect:/login";
        }

        ambulanceService.updateTripStatus(tripId, status, admin);
        return "redirect:/admin/ambulances?updated=true";
    }
}
