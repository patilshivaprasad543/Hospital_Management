package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.BloodBankService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/blood-bank")
public class BloodBankViewController {

    @Autowired
    private BloodBankService bloodBankService;

    @Autowired
    private com.hospital.service.BloodOrderService bloodOrderService;

    @GetMapping({"", "/", "/dashboard"})
    public String bloodBankDashboard(HttpSession session, Model model) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("units", bloodBankService.getAllUnits());
        model.addAttribute("availableUnits", bloodBankService.getAvailableUnits());
        model.addAttribute("groupCounts", bloodBankService.getAvailableCountsGrouped());
        model.addAttribute("bloodOrders", bloodOrderService.getAllOrders());
        model.addAttribute("pendingOrders", bloodOrderService.getPendingOrders());
        model.addAttribute("loggedInUser", loggedInUser);

        return "blood-bank/dashboard";
    }

    @PostMapping("/add")
    public String addBloodUnit(@RequestParam("unitCode") String unitCode,
                               @RequestParam("bloodGroup") BloodGroup bloodGroup,
                               @RequestParam("componentType") BloodComponentType componentType,
                               @RequestParam("donorName") String donorName,
                               @RequestParam("donorContact") String donorContact,
                               @RequestParam(value = "volumeMl", required = false, defaultValue = "450") Integer volumeMl,
                               @RequestParam(value = "expiryDays", required = false, defaultValue = "42") Integer expiryDays,
                               HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        LocalDate expiryDate = LocalDate.now().plusDays(expiryDays);
        bloodBankService.registerBloodUnit(unitCode, bloodGroup, componentType, donorName, donorContact, volumeMl, expiryDate, loggedInUser);

        return "redirect:/blood-bank/dashboard?added=true";
    }

    @PostMapping("/issue")
    public String issueBloodUnit(@RequestParam("unitId") Long unitId,
                                 HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        bloodBankService.issueBloodUnit(unitId, null, loggedInUser);

        return "redirect:/blood-bank/dashboard?issued=true";
    }

    @PostMapping("/orders/{id}/verify")
    public String verifyOrder(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        bloodOrderService.verifyPrescription(id, loggedInUser);
        return "redirect:/blood-bank/dashboard?verified=true";
    }

    @PostMapping("/orders/{id}/allocate")
    public String allocateUnits(@PathVariable("id") Long id,
                                @RequestParam(value = "unitIds", required = false) List<Long> unitIds,
                                @RequestParam(value = "notes", required = false) String notes,
                                HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        bloodOrderService.allocateAndCrossMatch(id, unitIds, notes, loggedInUser);
        return "redirect:/blood-bank/dashboard?allocated=true";
    }

    @PostMapping("/orders/{id}/dispatch")
    public String dispatchOrder(@PathVariable("id") Long id,
                                @RequestParam(value = "trackingNotes", required = false) String trackingNotes,
                                HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        bloodOrderService.dispatchOrder(id, trackingNotes, loggedInUser);
        return "redirect:/blood-bank/dashboard?dispatched=true";
    }

    @PostMapping("/orders/{id}/complete")
    public String completeOrder(@PathVariable("id") Long id, HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        bloodOrderService.completeOrder(id, loggedInUser);
        return "redirect:/blood-bank/dashboard?completed=true";
    }

    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable("id") Long id,
                              @RequestParam("reason") String reason,
                              HttpSession session) {
        User loggedInUser = com.hospital.service.UserSessionHelper.getAnyLoggedInUser(session);
        if (loggedInUser == null) return "redirect:/login";

        bloodOrderService.rejectOrder(id, reason, loggedInUser);
        return "redirect:/blood-bank/dashboard?rejected=true";
    }
}
