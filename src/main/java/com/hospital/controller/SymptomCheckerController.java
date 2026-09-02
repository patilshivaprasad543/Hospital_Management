package com.hospital.controller;

import com.hospital.model.User;
import com.hospital.service.SymptomTriageService;
import com.hospital.service.SymptomTriageService.TriageResult;
import com.hospital.service.UserService;
import com.hospital.service.UserSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/patient/symptom-checker")
public class SymptomCheckerController {

    @Autowired
    private SymptomTriageService triageService;

    @Autowired
    private UserService userService;

    @GetMapping({"", "/"})
    public String showSymptomChecker(HttpSession session, Model model) {
        User patient = UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        model.addAttribute("patient", patient);
        model.addAttribute("loggedInUser", patient);
        return "patient/symptom-checker";
    }

    @PostMapping("/evaluate")
    public String evaluateSymptoms(@RequestParam(value = "symptoms", required = false) List<String> symptoms,
                                   @RequestParam(value = "severity", required = false, defaultValue = "3") Integer severity,
                                   @RequestParam(value = "isEmergency", required = false, defaultValue = "false") Boolean isEmergency,
                                   HttpSession session, Model model) {
        User patient = UserSessionHelper.getLoggedInPatient(session);
        if (patient == null) return "redirect:/login/patient";

        TriageResult result = triageService.evaluateSymptoms(symptoms, severity, null, isEmergency);

        model.addAttribute("patient", patient);
        model.addAttribute("loggedInUser", patient);
        model.addAttribute("result", result);
        model.addAttribute("selectedSymptoms", symptoms);
        model.addAttribute("severity", severity);
        model.addAttribute("doctors", userService.findApprovedDoctors());

        return "patient/symptom-checker";
    }
}
