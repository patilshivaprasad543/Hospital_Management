package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.InsuranceClaimRepository;
import com.hospital.repository.InsuranceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class InsuranceService {

    @Autowired
    private InsuranceRepository insuranceRepository;

    @Autowired
    private InsuranceClaimRepository insuranceClaimRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public Optional<Insurance> getPatientInsurance(User patient) {
        return insuranceRepository.findByPatient(patient);
    }

    public List<Insurance> getAllInsurances() {
        return insuranceRepository.findAll();
    }

    @Transactional
    public Insurance registerPolicy(User patient, String provider, String policyNumber, String policyType,
                                    LocalDate startDate, LocalDate expiryDate, Double coverageAmount) {
        Optional<Insurance> existing = insuranceRepository.findByPatient(patient);
        Insurance insurance = existing.orElseGet(() -> new Insurance());
        insurance.setPatient(patient);
        insurance.setProvider(provider);
        insurance.setPolicyNumber(policyNumber);
        insurance.setPolicyType(policyType);
        insurance.setStartDate(startDate);
        insurance.setExpiryDate(expiryDate);
        insurance.setCoverageAmount(coverageAmount);

        Insurance saved = insuranceRepository.save(insurance);
        auditLogService.log(patient, "INSURANCE_POLICY_REGISTERED", "INSURANCE", "Registered insurance policy #" + policyNumber);
        notificationService.sendNotification(patient, "Insurance Policy Registered",
                "Your insurance policy #" + policyNumber + " from " + provider + " has been recorded.",
                NotificationCategory.BILLING, "/patient/insurance");
        return saved;
    }

    public List<InsuranceClaim> getPatientClaims(User patient) {
        Optional<Insurance> insurance = insuranceRepository.findByPatient(patient);
        return insurance.map(insuranceClaimRepository::findByInsurance).orElse(List.of());
    }

    public List<InsuranceClaim> getAllClaims() {
        return insuranceClaimRepository.findAllByOrderByClaimDateDesc();
    }

    public List<InsuranceClaim> getClaimsFiltered(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return insuranceClaimRepository.findAllByOrderByClaimDateDesc();
        }
        return insuranceClaimRepository.findByStatus(status.toUpperCase());
    }

    public long countPendingClaims() {
        return insuranceClaimRepository.countByStatus("SUBMITTED") + insuranceClaimRepository.countByStatus("UNDER_REVIEW");
    }

    public long countApprovedClaims() {
        return insuranceClaimRepository.countByStatus("APPROVED");
    }

    public double getTotalClaimValue() {
        return insuranceClaimRepository.findAll().stream()
                .mapToDouble(c -> c.getClaimAmount() != null ? c.getClaimAmount() : 0.0)
                .sum();
    }

    @Transactional
    public InsuranceClaim submitClaim(Insurance insurance, Invoice invoice, Double claimAmount, String remarks, User user) {
        InsuranceClaim claim = new InsuranceClaim(insurance, invoice, claimAmount, remarks);
        claim.setStatus("SUBMITTED");
        InsuranceClaim saved = insuranceClaimRepository.save(claim);

        auditLogService.log(user, "INSURANCE_CLAIM_SUBMITTED", "INSURANCE",
                "Submitted claim #" + saved.getId() + " for invoice #" + invoice.getId());
        notificationService.sendNotification(insurance.getPatient(), "Insurance Claim Submitted",
                "Insurance claim #" + saved.getId() + " for ₹" + claimAmount + " is now under review.",
                NotificationCategory.BILLING, "/patient/insurance");
        return saved;
    }

    @Transactional
    public InsuranceClaim updateClaimStatus(Long claimId, String status, String remarks, User adminUser) {
        InsuranceClaim claim = insuranceClaimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Insurance claim not found: " + claimId));

        claim.setStatus(status);
        if (remarks != null && !remarks.isBlank()) {
            claim.setRemarks(remarks);
        }
        InsuranceClaim updated = insuranceClaimRepository.save(claim);

        auditLogService.log(adminUser, "INSURANCE_CLAIM_UPDATED", "INSURANCE",
                "Claim #" + claimId + " updated to status " + status);
        notificationService.sendNotification(claim.getInsurance().getPatient(), "Claim Status Update",
                "Your insurance claim #" + claimId + " status is now: " + status,
                NotificationCategory.BILLING, "/patient/insurance");
        return updated;
    }
}
