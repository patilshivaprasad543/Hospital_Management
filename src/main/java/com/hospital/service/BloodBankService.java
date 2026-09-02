package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.BloodUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class BloodBankService {

    @Autowired
    private BloodUnitRepository bloodUnitRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    public List<BloodUnit> getAllUnits() {
        return bloodUnitRepository.findAll();
    }

    public List<BloodUnit> getAvailableUnits() {
        return bloodUnitRepository.findByStatus(BloodUnitStatus.AVAILABLE);
    }

    public Map<BloodGroup, Long> getAvailableCountsGrouped() {
        Map<BloodGroup, Long> counts = new EnumMap<>(BloodGroup.class);
        for (BloodGroup bg : BloodGroup.values()) {
            counts.put(bg, bloodUnitRepository.countByBloodGroupAndStatus(bg, BloodUnitStatus.AVAILABLE));
        }
        return counts;
    }

    @Transactional
    public BloodUnit registerBloodUnit(String unitCode, BloodGroup bloodGroup, BloodComponentType componentType,
                                        String donorName, String donorContact, Integer volumeMl, LocalDate expiryDate, User user) {
        BloodUnit unit = new BloodUnit(unitCode, bloodGroup, componentType, donorName, donorContact, volumeMl, expiryDate);
        BloodUnit saved = bloodUnitRepository.save(unit);

        if (user != null) {
            auditLogService.log(user, "BLOOD_UNIT_REGISTERED", "BLOOD_BANK",
                    "Registered new " + bloodGroup.getLabel() + " blood unit: " + unitCode);
        }

        return saved;
    }

    @Transactional
    public BloodUnit issueBloodUnit(Long unitId, User recipient, User user) {
        BloodUnit unit = bloodUnitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Blood unit not found: " + unitId));

        if (unit.getStatus() != BloodUnitStatus.AVAILABLE) {
            throw new RuntimeException("Blood unit " + unit.getUnitCode() + " is not available for issue.");
        }

        unit.setStatus(BloodUnitStatus.ISSUED);
        BloodUnit updated = bloodUnitRepository.save(unit);

        if (user != null) {
            auditLogService.log(user, "BLOOD_UNIT_ISSUED", "BLOOD_BANK",
                    "Issued " + unit.getBloodGroup().getLabel() + " unit " + unit.getUnitCode() + " to patient " + (recipient != null ? recipient.getFullName() : "Emergency"));
        }

        if (recipient != null) {
            notificationService.sendNotification(recipient, "Blood Unit Allocated",
                    "Blood unit #" + unit.getUnitCode() + " (" + unit.getBloodGroup().getLabel() + ") has been allocated for your treatment.",
                    NotificationCategory.SYSTEM, "/blood-bank/dashboard");
        }

        return updated;
    }
}
