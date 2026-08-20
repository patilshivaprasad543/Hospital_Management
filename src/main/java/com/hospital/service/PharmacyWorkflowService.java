package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.PharmacyOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PharmacyWorkflowService {

    private static final double DEFAULT_MEDICINE_PRICE = 75.0;

    @Autowired
    private PharmacyOrderRepository pharmacyOrderRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VendorService vendorService;

    public PharmacyOrder placeOrder(User patient, Prescription prescription, User pharmacyVendor,
                                    String deliveryAddress) {
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            throw new RuntimeException("This prescription has no medicines to order.");
        }
        if (pharmacyOrderRepository.existsByPrescriptionAndStatusNotIn(
                prescription, List.of(PharmacyOrderStatus.DELIVERED, PharmacyOrderStatus.CANCELLED))) {
            throw new RuntimeException("You already have an active pharmacy order for this prescription.");
        }

        double totalPrice = calculateOrderTotal(prescription, pharmacyVendor);
        String orderSummary = buildOrderSummary(prescription);
        PharmacyOrder order = new PharmacyOrder(patient, prescription, pharmacyVendor, totalPrice,
                orderSummary, deliveryAddress);
        PharmacyOrder saved = pharmacyOrderRepository.save(order);

        if (pharmacyVendor != null) {
            notificationService.sendPortalNotification(
                pharmacyVendor,
                "💊 New Prescription Medicine Order",
                "New order #" + saved.getId() + " from " + patient.getFullName()
                        + ". Total: ₹" + String.format(Locale.ENGLISH, "%.2f", totalPrice),
                NotificationCategory.PHARMACY,
                "/vendor/dashboard"
            );
        }

        notificationService.sendPortalNotification(
            patient,
            "📦 Pharmacy Order Placed",
            "Your medicine order #" + saved.getId() + " has been sent to the pharmacy.",
            NotificationCategory.PHARMACY,
            "/patient/pharmacy-orders"
        );

        return saved;
    }

    public PharmacyOrder updateOrderStatus(Long orderId, PharmacyOrderStatus newStatus,
                                           User vendor, String trackingNotes) {
        PharmacyOrder order = pharmacyOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pharmacy order not found"));

        if (vendor != null && order.getPharmacyVendor() != null
                && !order.getPharmacyVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("You are not authorized to update this order");
        }

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        if (trackingNotes != null && !trackingNotes.isBlank()) {
            order.setTrackingNotes(trackingNotes.trim());
        }

        PharmacyOrder saved = pharmacyOrderRepository.save(order);
        notifyPatientOfStatusChange(saved);
        return saved;
    }

    public List<PharmacyOrder> getPatientOrders(User patient) {
        return pharmacyOrderRepository.findByPatientOrderByCreatedAtDesc(patient);
    }

    public Optional<PharmacyOrder> getLatestOrderForPrescription(Prescription prescription) {
        return pharmacyOrderRepository.findFirstByPrescriptionOrderByCreatedAtDesc(prescription);
    }

    public double estimateOrderTotal(Prescription prescription, User pharmacyVendor) {
        return calculateOrderTotal(prescription, pharmacyVendor);
    }

    public List<PharmacyOrder> getVendorOrders(User vendor) {
        return pharmacyOrderRepository.findByPharmacyVendorOrderByCreatedAtDesc(vendor);
    }

    private double calculateOrderTotal(Prescription prescription, User pharmacyVendor) {
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            return DEFAULT_MEDICINE_PRICE;
        }

        List<PharmacyItem> vendorItems = vendorService.getPharmacyItemsByVendor(pharmacyVendor);
        double total = 0.0;

        for (PrescriptionItem item : prescription.getItems()) {
            double itemPrice = vendorItems.stream()
                    .filter(pi -> medicineMatches(pi.getItemName(), item.getMedicineName()))
                    .map(PharmacyItem::getPrice)
                    .findFirst()
                    .orElse(DEFAULT_MEDICINE_PRICE);
            total += itemPrice;
        }

        return total > 0 ? total : DEFAULT_MEDICINE_PRICE;
    }

    private boolean medicineMatches(String catalogName, String prescribedName) {
        if (catalogName == null || prescribedName == null) {
            return false;
        }
        String catalog = catalogName.toLowerCase(Locale.ENGLISH);
        String prescribed = prescribedName.toLowerCase(Locale.ENGLISH);
        return catalog.contains(prescribed) || prescribed.contains(catalog);
    }

    private String buildOrderSummary(Prescription prescription) {
        StringBuilder summary = new StringBuilder("Prescription #" + prescription.getId());
        if (prescription.getDiagnosis() != null && !prescription.getDiagnosis().isBlank()) {
            summary.append(" — ").append(prescription.getDiagnosis());
        }
        if (prescription.getItems() != null && !prescription.getItems().isEmpty()) {
            summary.append(" | Medicines: ");
            summary.append(prescription.getItems().stream()
                    .map(PrescriptionItem::getMedicineName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
        }
        return summary.toString();
    }

    private void validateStatusTransition(PharmacyOrderStatus current, PharmacyOrderStatus next) {
        if (current == next) {
            return;
        }
        if (current.isTerminal()) {
            throw new RuntimeException("Order is already " + current.getDisplayName().toLowerCase());
        }
        if (next == PharmacyOrderStatus.CANCELLED) {
            return;
        }

        boolean valid = switch (current) {
            case PLACED -> next == PharmacyOrderStatus.ACCEPTED;
            case ACCEPTED -> next == PharmacyOrderStatus.PROCESSING;
            case PROCESSING -> next == PharmacyOrderStatus.DISPATCHED;
            case DISPATCHED -> next == PharmacyOrderStatus.DELIVERED;
            default -> false;
        };

        if (!valid) {
            throw new RuntimeException("Cannot change status from " + current.getDisplayName()
                    + " to " + next.getDisplayName());
        }
    }

    private void notifyPatientOfStatusChange(PharmacyOrder order) {
        PharmacyOrderStatus status = order.getStatus();
        String message = switch (status) {
            case ACCEPTED -> "Your pharmacy order #" + order.getId() + " has been accepted and is being prepared.";
            case PROCESSING -> "Your medicines for order #" + order.getId() + " are being processed.";
            case DISPATCHED -> "Your order #" + order.getId() + " has been dispatched and is on the way.";
            case DELIVERED -> "Your order #" + order.getId() + " has been delivered. Thank you!";
            case CANCELLED -> "Your pharmacy order #" + order.getId() + " has been cancelled.";
            default -> "Your pharmacy order #" + order.getId() + " status is now "
                    + status.getDisplayName() + ".";
        };

        if (order.getTrackingNotes() != null && !order.getTrackingNotes().isBlank()) {
            message += " Note: " + order.getTrackingNotes();
        }

        notificationService.sendPortalNotification(
            order.getPatient(),
            "💊 Pharmacy Order: " + status.getDisplayName(),
            message,
            NotificationCategory.PHARMACY,
            "/patient/pharmacy-orders"
        );
    }
}
