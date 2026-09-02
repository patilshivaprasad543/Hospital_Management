package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.BloodOrderRepository;
import com.hospital.repository.BloodUnitRepository;
import com.hospital.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BloodOrderService {

    public static final double DEFAULT_UNIT_PRICE = 1500.0;

    @Autowired
    private BloodOrderRepository bloodOrderRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private BloodUnitRepository bloodUnitRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditLogService auditLogService;

    @Transactional
    public BloodOrder createBloodOrder(User patient, Long prescriptionId, BloodGroup bloodGroup,
                                       BloodComponentType componentType, Integer units,
                                       String deliveryType, String deliveryLocation,
                                       String patientContact, String clinicalNotes,
                                       String urgencyLevel, String paymentMethod) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required.");
        }
        if (prescriptionId == null) {
            throw new IllegalArgumentException("Valid doctor prescription is required to purchase blood.");
        }

        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found with ID: " + prescriptionId));

        if (!prescription.getPatient().getId().equals(patient.getId())) {
            throw new SecurityException("Prescription does not belong to the logged-in patient.");
        }

        User doctor = prescription.getDoctor();
        int reqUnits = (units != null && units > 0) ? units : 1;
        BloodGroup reqGroup = (bloodGroup != null) ? bloodGroup : (prescription.getBloodGroup() != null ? prescription.getBloodGroup() : BloodGroup.O_POSITIVE);
        BloodComponentType reqComponent = (componentType != null) ? componentType : (prescription.getBloodComponentType() != null ? prescription.getBloodComponentType() : BloodComponentType.PACKED_RED_CELLS);

        String orderNum = "BLD-ORD-" + (System.currentTimeMillis() % 1000000);

        BloodOrder order = new BloodOrder(
                orderNum,
                patient,
                prescription,
                doctor,
                reqGroup,
                reqComponent,
                reqUnits,
                DEFAULT_UNIT_PRICE,
                deliveryType,
                deliveryLocation,
                patientContact,
                clinicalNotes,
                urgencyLevel,
                paymentMethod
        );

        BloodOrder saved = bloodOrderRepository.save(order);

        // Multi-channel notifications
        notificationService.sendPortalNotification(
                patient,
                "🩸 Blood Purchase Order Placed",
                "Your request for " + reqUnits + " unit(s) of " + reqGroup.getLabel() + " (" + reqComponent.getLabel() + ") under Dr. " + doctor.getFullName() + "'s prescription has been submitted. Order #" + orderNum + ".",
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        notificationService.sendPortalNotification(
                doctor,
                "🩸 Blood Order Placed by Patient",
                "Patient " + patient.getFullName() + " has requested " + reqUnits + " unit(s) of " + reqGroup.getLabel() + " through your prescription #" + prescription.getId() + ".",
                NotificationCategory.SYSTEM,
                "/blood-bank/dashboard"
        );

        auditLogService.log(patient, "BLOOD_ORDER_CREATED", "BLOOD_BANK",
                "Created blood order " + orderNum + " for " + reqUnits + " units of " + reqGroup.getLabel() + " with Rx #" + prescriptionId);

        return saved;
    }

    public List<BloodOrder> getPatientOrders(User patient) {
        return bloodOrderRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<BloodOrder> getDoctorOrders(User doctor) {
        return bloodOrderRepository.findByDoctorOrderByCreatedAtDesc(doctor);
    }

    public List<BloodOrder> getAllOrders() {
        return bloodOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<BloodOrder> getPendingOrders() {
        return bloodOrderRepository.findByStatusOrderByCreatedAtDesc(BloodOrderStatus.REQUESTED);
    }

    public Optional<BloodOrder> findById(Long id) {
        return bloodOrderRepository.findById(id);
    }

    public Optional<BloodOrder> findByOrderNumber(String orderNumber) {
        return bloodOrderRepository.findByOrderNumber(orderNumber);
    }

    @Transactional
    public BloodOrder verifyPrescription(Long orderId, User staff) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        order.setStatus(BloodOrderStatus.VERIFIED);
        order.setPrescriptionVerified(true);
        BloodOrder updated = bloodOrderRepository.save(order);

        notificationService.sendPortalNotification(
                order.getPatient(),
                "✅ Blood Prescription Verified",
                "Blood Bank staff has verified Dr. " + order.getDoctor().getFullName() + "'s prescription for Order #" + order.getOrderNumber() + ". Proceeding to cross-matching.",
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        if (staff != null) {
            auditLogService.log(staff, "BLOOD_PRESCRIPTION_VERIFIED", "BLOOD_BANK",
                    "Verified prescription for blood order " + order.getOrderNumber());
        }

        return updated;
    }

    @Transactional
    public BloodOrder allocateAndCrossMatch(Long orderId, List<Long> unitIds, String notes, User staff) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        List<String> codes = new ArrayList<>();
        if (unitIds != null && !unitIds.isEmpty()) {
            for (Long uid : unitIds) {
                Optional<BloodUnit> opt = bloodUnitRepository.findById(uid);
                if (opt.isPresent()) {
                    BloodUnit bu = opt.get();
                    bu.setStatus(BloodUnitStatus.ISSUED);
                    bloodUnitRepository.save(bu);
                    codes.add(bu.getUnitCode());
                }
            }
        }

        if (!codes.isEmpty()) {
            order.setAllocatedUnitCodes(String.join(", ", codes));
        }

        order.setStatus(BloodOrderStatus.READY_FOR_COLLECTION);
        order.setClinicalNotes(notes != null ? notes : order.getClinicalNotes());
        BloodOrder updated = bloodOrderRepository.save(order);

        String unitCodesStr = !codes.isEmpty() ? " (Barcodes: " + String.join(", ", codes) + ")" : "";
        notificationService.sendPortalNotification(
                order.getPatient(),
                "🩸 Blood Units Ready for Transfusion",
                "Compatible blood units for Order #" + order.getOrderNumber() + unitCodesStr + " are cross-matched and ready. Please collect or ward dispatch initiated.",
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        if (staff != null) {
            auditLogService.log(staff, "BLOOD_UNITS_ALLOCATED", "BLOOD_BANK",
                    "Allocated units " + unitCodesStr + " for order " + order.getOrderNumber());
        }

        return updated;
    }

    @Transactional
    public BloodOrder dispatchOrder(Long orderId, String trackingNotes, User staff) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        order.setStatus(BloodOrderStatus.DISPATCHED);
        order.setClinicalNotes(trackingNotes != null ? trackingNotes : order.getClinicalNotes());
        BloodOrder updated = bloodOrderRepository.save(order);

        notificationService.sendPortalNotification(
                order.getPatient(),
                "🚚 Blood Cold-Chain Dispatched",
                "Your blood units for Order #" + order.getOrderNumber() + " have been dispatched to " + (order.getDeliveryLocation() != null ? order.getDeliveryLocation() : "your ward") + ".",
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        if (staff != null) {
            auditLogService.log(staff, "BLOOD_ORDER_DISPATCHED", "BLOOD_BANK",
                    "Dispatched blood order " + order.getOrderNumber());
        }

        return updated;
    }

    @Transactional
    public BloodOrder completeOrder(Long orderId, User staff) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        order.setStatus(BloodOrderStatus.COMPLETED);
        BloodOrder updated = bloodOrderRepository.save(order);

        notificationService.sendPortalNotification(
                order.getPatient(),
                "✅ Blood Requisition Fulfilled",
                "Blood Order #" + order.getOrderNumber() + " has been successfully fulfilled and handed over.",
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        if (staff != null) {
            auditLogService.log(staff, "BLOOD_ORDER_COMPLETED", "BLOOD_BANK",
                    "Completed blood order " + order.getOrderNumber());
        }

        return updated;
    }

    @Transactional
    public BloodOrder rejectOrder(Long orderId, String reason, User staff) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        order.setStatus(BloodOrderStatus.REJECTED);
        order.setRejectionReason(reason);
        BloodOrder updated = bloodOrderRepository.save(order);

        notificationService.sendPortalNotification(
                order.getPatient(),
                "❌ Blood Order Rejected",
                "Your Blood Order #" + order.getOrderNumber() + " could not be processed. Reason: " + reason,
                NotificationCategory.SYSTEM,
                "/patient/blood-bank/orders"
        );

        if (staff != null) {
            auditLogService.log(staff, "BLOOD_ORDER_REJECTED", "BLOOD_BANK",
                    "Rejected blood order " + order.getOrderNumber() + ". Reason: " + reason);
        }

        return updated;
    }

    @Transactional
    public BloodOrder payOrder(Long orderId, String paymentMethod) {
        BloodOrder order = bloodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Blood order not found"));

        order.setPaymentStatus(PaymentStatus.PAID);
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            order.setPaymentMethod(paymentMethod);
        }
        return bloodOrderRepository.save(order);
    }
}
