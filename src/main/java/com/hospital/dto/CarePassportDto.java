package com.hospital.dto;

import java.util.ArrayList;
import java.util.List;

public class CarePassportDto {

    private String passportId;
    private String patientName;
    private String email;
    private String bloodGroup;
    private String allergies;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String generatedAt;
    private List<String> currentMedicines = new ArrayList<>();
    private List<String> recentDiagnoses = new ArrayList<>();
    private List<String> recentLabResults = new ArrayList<>();

    public String getPassportId() { return passportId; }
    public void setPassportId(String passportId) { this.passportId = passportId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
    public List<String> getCurrentMedicines() { return currentMedicines; }
    public void setCurrentMedicines(List<String> currentMedicines) { this.currentMedicines = currentMedicines; }
    public List<String> getRecentDiagnoses() { return recentDiagnoses; }
    public void setRecentDiagnoses(List<String> recentDiagnoses) { this.recentDiagnoses = recentDiagnoses; }
    public List<String> getRecentLabResults() { return recentLabResults; }
    public void setRecentLabResults(List<String> recentLabResults) { this.recentLabResults = recentLabResults; }
}
