package com.hospital.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SymptomTriageService {

    public enum TriageLevel {
        EMERGENCY_ER("🔴 Emergency / Call 108/911", "danger", "Go to the nearest Emergency Room or call emergency services immediately."),
        URGENT_CARE("🟠 Urgent Care Visit", "warning", "Schedule an urgent consultation or visit an Urgent Care Clinic within 24 hours."),
        ROUTINE_DOCTOR("🟡 Routine Doctor Consultation", "info", "Schedule a routine appointment with a specialist or general physician."),
        SELF_CARE("🟢 Home Care & Self Monitoring", "success", "Your symptoms suggest mild discomfort. Follow home care guidelines and monitor for changes.");

        private final String title;
        private final String badgeStyle;
        private final String recommendation;

        TriageLevel(String title, String badgeStyle, String recommendation) {
            this.title = title;
            this.badgeStyle = badgeStyle;
            this.recommendation = recommendation;
        }

        public String getTitle() { return title; }
        public String getBadgeStyle() { return badgeStyle; }
        public String getRecommendation() { return recommendation; }
    }

    public static class TriageResult {
        private TriageLevel level;
        private String recommendedSpecialty;
        private List<String> redFlagAlerts = new ArrayList<>();
        private List<String> careInstructions = new ArrayList<>();

        public TriageResult(TriageLevel level, String recommendedSpecialty) {
            this.level = level;
            this.recommendedSpecialty = recommendedSpecialty;
        }

        public TriageLevel getLevel() { return level; }
        public String getRecommendedSpecialty() { return recommendedSpecialty; }
        public List<String> getRedFlagAlerts() { return redFlagAlerts; }
        public List<String> getCareInstructions() { return careInstructions; }
    }

    public TriageResult evaluateSymptoms(List<String> symptoms, Integer severity, Integer age, Boolean isEmergencyCall) {
        if (Boolean.TRUE.equals(isEmergencyCall)) {
            TriageResult result = new TriageResult(TriageLevel.EMERGENCY_ER, "Emergency Medicine");
            result.getRedFlagAlerts().add("Patient reported immediate life-threatening emergency.");
            result.getCareInstructions().add("Call emergency services (108/911) or visit the ER immediately.");
            return result;
        }

        Set<String> lowerSymptoms = new HashSet<>();
        if (symptoms != null) {
            for (String s : symptoms) {
                if (s != null) lowerSymptoms.add(s.trim().toLowerCase());
            }
        }

        int sev = (severity != null) ? severity : 3;

        TriageResult result;

        // Check for Emergency Red Flags
        if (lowerSymptoms.contains("chest_pain") || lowerSymptoms.contains("shortness_of_breath") ||
            lowerSymptoms.contains("sudden_numbness") || lowerSymptoms.contains("severe_bleeding") ||
            lowerSymptoms.contains("unconsciousness") || sev >= 9) {

            result = new TriageResult(TriageLevel.EMERGENCY_ER, "Emergency Medicine & Cardiology");
            if (lowerSymptoms.contains("chest_pain")) result.getRedFlagAlerts().add("Chest pain or pressure requires immediate cardiac evaluation.");
            if (lowerSymptoms.contains("shortness_of_breath")) result.getRedFlagAlerts().add("Severe respiratory distress detected.");
            if (lowerSymptoms.contains("sudden_numbness")) result.getRedFlagAlerts().add("Sudden numbness/weakness may indicate stroke.");
            result.getCareInstructions().add("Do not drive yourself. Have someone drive you to the ER or call emergency services.");
            return result;
        }

        // Urgent Care symptoms
        if (lowerSymptoms.contains("high_fever") || lowerSymptoms.contains("severe_headache") ||
            lowerSymptoms.contains("persistent_vomiting") || lowerSymptoms.contains("deep_cut") ||
            sev >= 7) {

            result = new TriageResult(TriageLevel.URGENT_CARE, "Internal Medicine & General Care");
            result.getCareInstructions().add("Stay hydrated and rest.");
            result.getCareInstructions().add("Book an urgent consultation with a physician within 24 hours.");
            return result;
        }

        // Specialty routing for routine symptoms
        String specialty = "General Physician";
        if (lowerSymptoms.contains("skin_rash") || lowerSymptoms.contains("itching")) {
            specialty = "Dermatology";
        } else if (lowerSymptoms.contains("joint_pain") || lowerSymptoms.contains("back_pain")) {
            specialty = "Orthopedics";
        } else if (lowerSymptoms.contains("eye_pain") || lowerSymptoms.contains("blurred_vision")) {
            specialty = "Ophthalmology";
        } else if (lowerSymptoms.contains("ear_pain") || lowerSymptoms.contains("sore_throat")) {
            specialty = "ENT (Ear, Nose, Throat)";
        } else if (lowerSymptoms.contains("anxiety") || lowerSymptoms.contains("depression")) {
            specialty = "Psychiatry";
        } else if (age != null && age < 14) {
            specialty = "Pediatrics";
        }

        if (sev >= 4) {
            result = new TriageResult(TriageLevel.ROUTINE_DOCTOR, specialty);
            result.getCareInstructions().add("Schedule an appointment with a " + specialty + " specialist.");
            result.getCareInstructions().add("Keep a log of when symptoms occur and any triggers.");
        } else {
            result = new TriageResult(TriageLevel.SELF_CARE, specialty);
            result.getCareInstructions().add("Get plenty of rest and drink adequate fluids.");
            result.getCareInstructions().add("Over-the-counter medication may help relieve mild discomfort.");
            result.getCareInstructions().add("If symptoms worsen or persist beyond 3 days, book a doctor appointment.");
        }

        return result;
    }
}
