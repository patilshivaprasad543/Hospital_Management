package com.hospital.controller;

import com.hospital.model.PatientVitals;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.repository.PatientVitalsRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/patient/vitals")
public class PatientVitalsController {

    @Autowired
    private PatientVitalsRepository patientVitalsRepository;

    private User getLoggedInPatient(HttpSession session) {
        return com.hospital.service.UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/"})
    public String viewVitals(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        List<PatientVitals> vitalsHistory = patientVitalsRepository.findByPatientOrderByRecordedAtDesc(patient);
        PatientVitals latestVitals = vitalsHistory.isEmpty() ? new PatientVitals() : vitalsHistory.get(0);

        model.addAttribute("patient", patient);
        model.addAttribute("vitalsHistory", vitalsHistory);
        model.addAttribute("latestVitals", latestVitals);
        model.addAttribute("loggedInUser", patient);

        return "patient/vitals";
    }

    @PostMapping("/add")
    public String recordVitals(@RequestParam(value = "systolicBp", required = false) Integer systolicBp,
                               @RequestParam(value = "diastolicBp", required = false) Integer diastolicBp,
                               @RequestParam(value = "heartRate", required = false) Integer heartRate,
                               @RequestParam(value = "bloodGlucose", required = false) Double bloodGlucose,
                               @RequestParam(value = "bodyTemperature", required = false) Double bodyTemperature,
                               @RequestParam(value = "spo2", required = false) Integer spo2,
                               @RequestParam(value = "weightKg", required = false) Double weightKg,
                               @RequestParam(value = "heightCm", required = false) Double heightCm,
                               @RequestParam(value = "notes", required = false) String notes,
                               HttpSession session) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login";
        }

        PatientVitals vitals = new PatientVitals(patient, systolicBp, diastolicBp, heartRate, bloodGlucose, bodyTemperature, spo2, weightKg, heightCm);
        vitals.setNotes(notes);
        patientVitalsRepository.save(vitals);

        return "redirect:/patient/vitals?success=true";
    }
}
