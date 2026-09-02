package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.InsuranceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/patient/insurance")
public class InsuranceController {

    @Autowired
    private InsuranceService insuranceService;

    private User getLoggedInPatient(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/"})
    public String insuranceHome(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        Optional<Insurance> insurance = insuranceService.getPatientInsurance(patient);
        List<InsuranceClaim> claims = insuranceService.getPatientClaims(patient);

        model.addAttribute("insurance", insurance.orElse(null));
        model.addAttribute("claims", claims);
        model.addAttribute("loggedInUser", patient);
        return "patient/insurance";
    }

    @PostMapping("/apply")
    public String registerInsurance(@RequestParam("provider") String provider,
                                    @RequestParam("policyNumber") String policyNumber,
                                    @RequestParam("policyType") String policyType,
                                    @RequestParam("startDate") String startDateStr,
                                    @RequestParam("expiryDate") String expiryDateStr,
                                    @RequestParam("coverageAmount") Double coverageAmount,
                                    HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate expiryDate = LocalDate.parse(expiryDateStr);

        insuranceService.registerPolicy(patient, provider, policyNumber, policyType, startDate, expiryDate, coverageAmount);
        return "redirect:/patient/insurance?saved=true";
    }
}
