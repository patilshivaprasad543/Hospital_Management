package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.PharmacyOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PharmacyWorkflowService {

    @Autowired
    private PharmacyOrderRepository pharmacyOrderRepository;

    @Autowired
    private NotificationService notificationService;

    public PharmacyOrder placeOrder(User patient, Prescription prescription, User pharmacyVendor, Double totalPrice, String orderSummary) {
        PharmacyOrder order = new PharmacyOrder(patient, prescription, pharmacyVendor, totalPrice, orderSummary);
        PharmacyOrder saved = pharmacyOrderRepository.save(order);

        if (pharmacyVendor != null) {
            notificationService.sendNotification(
                pharmacyVendor,
                "💊 New Prescription Medicine Order",
                "New order placed by " + patient.getFullName() + ". Order Summary: " + orderSummary,
                NotificationCategory.PHARMACY,
                "/vendor/dashboard"
            );
        }

        notificationService.sendNotification(
            patient,
            "📦 Pharmacy Order Placed",
            "Your medicine order has been dispatched to pharmacy vendor.",
            NotificationCategory.PHARMACY,
            "/patient/dashboard"
        );

        return saved;
    }

    public PharmacyOrder updateOrderStatus(Long orderId, String newStatus) {
        PharmacyOrder order = pharmacyOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pharmacy order not found"));
        order.setStatus(newStatus);
        PharmacyOrder saved = pharmacyOrderRepository.save(order);

        if ("READY_FOR_PICKUP".equals(newStatus) || "COMPLETED".equals(newStatus)) {
            notificationService.sendNotification(
                order.getPatient(),
                "💊 Medicine Ready",
                "Your pharmacy order is " + newStatus.replace("_", " ").toLowerCase()
                        + ". Please collect your medicines.",
                NotificationCategory.PHARMACY,
                "/patient/dashboard"
            );
        } else {
            notificationService.sendNotification(
                order.getPatient(),
                "📦 Pharmacy Order Status Update: " + newStatus,
                "Your medicine order status has changed to " + newStatus,
                NotificationCategory.PHARMACY,
                "/patient/dashboard"
            );
        }

        return saved;
    }

    public List<PharmacyOrder> getPatientOrders(User patient) {
        return pharmacyOrderRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public List<PharmacyOrder> getVendorOrders(User vendor) {
        return pharmacyOrderRepository.findByPharmacyVendorOrderByCreatedAtDesc(vendor);
    }
}
