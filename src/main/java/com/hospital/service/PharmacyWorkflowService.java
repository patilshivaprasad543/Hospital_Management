package com.hospital.service;

import com.hospital.model.*;
import com.hospital.repository.InvoiceRepository;
import com.hospital.repository.PharmacyOrderRepository;
import com.hospital.repository.PharmacyStockMovementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PharmacyWorkflowService {

    private static final double DEFAULT_MEDICINE_PRICE = 75.0;
    private static final List<PharmacyOrderStatus> INACTIVE_ORDER_STATUSES =
            List.of(PharmacyOrderStatus.DELIVERED, PharmacyOrderStatus.COMPLETED, PharmacyOrderStatus.CANCELLED);

    @Autowired
    private PharmacyOrderRepository pharmacyOrderRepository;

    @Autowired
    private PharmacyStockMovementRepository stockMovementRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VendorService vendorService;

    @Autowired
    private BillingService billingService;

    public PharmacyOrder placeOrder(User patient, Prescription prescription, User pharmacyVendor,
                                    String deliveryAddress) {
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            throw new RuntimeException("This prescription has no medicines to order.");
        }
        if (pharmacyOrderRepository.existsByPrescriptionAndStatusNotIn(prescription, INACTIVE_ORDER_STATUSES)) {
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
                        + ". Total: ₹" + String.format(Locale.ENGLISH, "%.2f", totalPrice)
                        + ". Verify the prescription and check stock.",
                NotificationCategory.PHARMACY,
                "/vendor/orders/" + saved.getId()
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

    @Transactional
    public PharmacyOrder updateOrderStatus(Long orderId, PharmacyOrderStatus newStatus,
                                           User vendor, String trackingNotes) {
        PharmacyOrder order = pharmacyOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Pharmacy order not found"));

        if (vendor != null && order.getPharmacyVendor() != null
                && !order.getPharmacyVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("You are not authorized to update this order");
        }

        PharmacyOrderStatus previous = order.getStatus();
        validateStatusTransition(previous, newStatus);

        if (newStatus == PharmacyOrderStatus.ACCEPTED) {
            List<MedicineAvailability> availability = checkAvailability(order);
            order.setStockChecked(true);
            boolean unavailable = availability.stream().anyMatch(a -> !a.available());
            if (unavailable) {
                String missing = availability.stream()
                        .filter(a -> !a.available())
                        .map(MedicineAvailability::medicineName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("prescribed medicines");
                notificationService.sendPortalNotification(
                        order.getPatient(),
                        "⚠️ Medicine availability",
                        "Some medicines on order #" + order.getId() + " are currently unavailable: " + missing
                                + ". The pharmacy may still prepare available items or cancel the order.",
                        NotificationCategory.PHARMACY,
                        "/patient/pharmacy-orders"
                );
            }
            ensureInvoice(order);
        }

        if (newStatus == PharmacyOrderStatus.CANCELLED) {
            notifyVendorOfCancellation(order);
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        if (trackingNotes != null && !trackingNotes.isBlank()) {
            order.setTrackingNotes(trackingNotes.trim());
        }

        if (newStatus == PharmacyOrderStatus.DELIVERED || newStatus == PharmacyOrderStatus.COMPLETED) {
            deductStockForSale(order);
        }

        PharmacyOrder saved = pharmacyOrderRepository.saveAndFlush(order);
        notifyPatientOfStatusChange(saved);
        return saved;
    }

    @Transactional
    public PharmacyOrder verifyPrescription(Long orderId, User vendor, boolean verified, String notes) {
        PharmacyOrder order = requireVendorOrder(orderId, vendor);
        if (order.getPrescription() == null) {
            throw new RuntimeException("This order has no linked doctor prescription.");
        }
        order.setPrescriptionVerified(verified);
        if (notes != null && !notes.isBlank()) {
            order.setPharmacyNotes(notes.trim());
        }
        order.setUpdatedAt(LocalDateTime.now());
        PharmacyOrder saved = pharmacyOrderRepository.save(order);

        notificationService.sendPortalNotification(
                order.getPatient(),
                verified ? "✅ Prescription verified" : "❌ Prescription could not be verified",
                verified
                        ? "The pharmacy verified your doctor's prescription for order #" + order.getId() + "."
                        : "The pharmacy could not verify the prescription for order #" + order.getId()
                        + (notes != null && !notes.isBlank() ? ": " + notes : "."),
                NotificationCategory.PHARMACY,
                "/patient/pharmacy-orders"
        );
        return saved;
    }

    public List<MedicineAvailability> checkAvailability(PharmacyOrder order) {
        List<MedicineAvailability> result = new ArrayList<>();
        Prescription prescription = order.getPrescription();
        if (prescription == null || prescription.getItems() == null) {
            return result;
        }
        List<PharmacyItem> catalog = vendorService.getPharmacyItemsByVendor(order.getPharmacyVendor());
        for (PrescriptionItem item : prescription.getItems()) {
            Optional<PharmacyItem> match = findMatchingItem(catalog, item.getMedicineName());
            if (match.isEmpty()) {
                result.add(new MedicineAvailability(item.getMedicineName(), false, 0, DEFAULT_MEDICINE_PRICE,
                        "Not in pharmacy catalog"));
                continue;
            }
            PharmacyItem stock = match.get();
            boolean available = stock.getStockQuantity() != null && stock.getStockQuantity() > 0 && !stock.isExpired();
            String message = stock.isExpired() ? "Expired batch" : (available ? "In stock" : "Out of stock");
            result.add(new MedicineAvailability(item.getMedicineName(), available,
                    stock.getStockQuantity(), stock.getPrice() != null ? stock.getPrice() : DEFAULT_MEDICINE_PRICE,
                    message));
        }
        return result;
    }

    public PharmacyOrder getVendorOrder(Long orderId, User vendor) {
        return requireVendorOrder(orderId, vendor);
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

    public Map<PharmacyOrderStatus, Long> countOrdersByStatus(User vendor) {
        Map<PharmacyOrderStatus, Long> counts = new EnumMap<>(PharmacyOrderStatus.class);
        for (PharmacyOrderStatus status : PharmacyOrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (PharmacyOrder order : getVendorOrders(vendor)) {
            counts.merge(order.getStatus(), 1L, Long::sum);
        }
        return counts;
    }

    public PharmacyReport buildReport(User vendor) {
        List<PharmacyOrder> orders = getVendorOrders(vendor);
        List<PharmacyItem> items = vendorService.getPharmacyItemsByVendor(vendor);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        double dailySales = orders.stream()
                .filter(o -> isSaleStatus(o.getStatus()) && o.getUpdatedAt() != null
                        && today.equals(o.getUpdatedAt().toLocalDate()))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
        double monthlySales = orders.stream()
                .filter(o -> isSaleStatus(o.getStatus()) && o.getUpdatedAt() != null
                        && !o.getUpdatedAt().toLocalDate().isBefore(monthStart))
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
        double revenue = orders.stream()
                .filter(o -> o.getPaymentStatus() == PaymentStatus.PAID)
                .mapToDouble(o -> o.getTotalPrice() != null ? o.getTotalPrice() : 0)
                .sum();
        if (revenue <= 0) {
            revenue = monthlySales;
        }

        List<MedicineSaleLine> medicineSales = new ArrayList<>();
        for (PharmacyItem item : items) {
            long sold = stockMovementRepository.findByVendorOrderByCreatedAtDesc(vendor).stream()
                    .filter(m -> m.getMovementType() == StockMovementType.SALE
                            && m.getItem() != null && m.getItem().getId().equals(item.getId()))
                    .mapToLong(m -> m.getQuantityChange() != null ? Math.abs(m.getQuantityChange()) : 0)
                    .sum();
            medicineSales.add(new MedicineSaleLine(item.getItemName(), sold,
                    item.getPrice() != null ? item.getPrice() * sold : 0));
        }

        return new PharmacyReport(dailySales, monthlySales, revenue, orders.size(),
                items.stream().filter(PharmacyItem::isLowStock).toList(),
                items.stream().filter(i -> i.isExpired() || i.isNearExpiry()).toList(),
                medicineSales, countOrdersByStatus(vendor),
                stockMovementRepository.findByVendorOrderByCreatedAtDesc(vendor));
    }

    public Optional<Invoice> getOrderInvoice(PharmacyOrder order) {
        if (order.getInvoiceId() != null) {
            return invoiceRepository.findById(order.getInvoiceId());
        }
        return invoiceRepository.findFirstByChargeTypeAndReferenceId("PHARMACY", order.getId());
    }

    public void markOrderPaid(PharmacyOrder order) {
        order.setPaymentStatus(PaymentStatus.PAID);
        pharmacyOrderRepository.save(order);
        if (order.getPharmacyVendor() != null) {
            notificationService.sendPortalNotification(
                    order.getPharmacyVendor(),
                    "💰 Payment confirmation",
                    "Payment received for pharmacy order #" + order.getId()
                            + " (" + order.getPatient().getFullName() + ").",
                    NotificationCategory.BILLING,
                    "/vendor/orders/" + order.getId()
            );
        }
    }

    private void ensureInvoice(PharmacyOrder order) {
        if (order.getInvoiceId() != null) {
            return;
        }
        Optional<Invoice> existing = invoiceRepository.findFirstByChargeTypeAndReferenceId("PHARMACY", order.getId());
        if (existing.isPresent()) {
            order.setInvoiceId(existing.get().getId());
            return;
        }
        Invoice invoice = billingService.createInvoice(
                order.getPatient(),
                "PHARMACY",
                "Pharmacy order #" + order.getId() + " — " + order.getOrderSummary(),
                order.getTotalPrice(),
                order.getId()
        );
        order.setInvoiceId(invoice.getId());
        order.setPaymentStatus(PaymentStatus.PENDING);
        notificationService.sendPortalNotification(
                order.getPatient(),
                "🧾 Pharmacy invoice ready",
                "Invoice " + invoice.getInvoiceNumber() + " for ₹"
                        + String.format(Locale.ENGLISH, "%.2f", invoice.getAmount())
                        + " is ready. Pay from Bills or download the receipt.",
                NotificationCategory.BILLING,
                "/patient/bills"
        );
    }

    private void deductStockForSale(PharmacyOrder order) {
        if (order.isStockDeducted() || order.getPrescription() == null || order.getPharmacyVendor() == null) {
            return;
        }
        List<PharmacyItem> catalog = vendorService.getPharmacyItemsByVendor(order.getPharmacyVendor());
        for (PrescriptionItem prescribed : order.getPrescription().getItems()) {
            findMatchingItem(catalog, prescribed.getMedicineName()).ifPresent(item -> {
                int qty = Math.max(1, parseQuantity(prescribed));
                int current = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
                int remaining = Math.max(0, current - qty);
                item.setStockQuantity(remaining);
                vendorService.savePharmacyItem(item);
                stockMovementRepository.save(new PharmacyStockMovement(
                        order.getPharmacyVendor(), item, order, StockMovementType.SALE,
                        -qty, remaining, "Sale for order #" + order.getId()));
                if (item.isLowStock()) {
                    notificationService.sendPortalNotification(
                            order.getPharmacyVendor(),
                            "📉 Low stock",
                            item.getItemName() + " is low on stock (" + remaining + " remaining).",
                            NotificationCategory.PHARMACY,
                            "/vendor/inventory"
                    );
                }
            });
        }
        order.setStockDeducted(true);
    }

    public PharmacyItem receiveStock(User vendor, Long itemId, int quantity, String notes) {
        PharmacyItem item = vendorService.getPharmacyItemForVendor(itemId, vendor);
        int current = item.getStockQuantity() != null ? item.getStockQuantity() : 0;
        int updated = current + quantity;
        item.setStockQuantity(updated);
        vendorService.savePharmacyItem(item);
        stockMovementRepository.save(new PharmacyStockMovement(
                vendor, item, null, StockMovementType.RECEIPT, quantity, updated,
                notes != null && !notes.isBlank() ? notes : "Stock received"));
        return item;
    }

    public void notifyExpiryAndLowStock(User vendor) {
        List<PharmacyItem> items = vendorService.getPharmacyItemsByVendor(vendor);
        long low = items.stream().filter(PharmacyItem::isLowStock).count();
        long expiring = items.stream().filter(i -> i.isExpired() || i.isNearExpiry()).count();
        if (low > 0) {
            notificationService.sendPortalNotification(
                    vendor,
                    "📉 Low stock alert",
                    low + " medicine(s) are at or below the low-stock threshold. Review inventory.",
                    NotificationCategory.PHARMACY,
                    "/vendor/inventory"
            );
        }
        if (expiring > 0) {
            notificationService.sendPortalNotification(
                    vendor,
                    "⏰ Expiry alert",
                    expiring + " medicine(s) are expired or expiring within 30 days.",
                    NotificationCategory.PHARMACY,
                    "/vendor/inventory"
            );
        }
    }

    private int parseQuantity(PrescriptionItem item) {
        if (item.getDuration() == null) {
            return 1;
        }
        String digits = item.getDuration().replaceAll("[^0-9]", " ").trim();
        if (digits.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(digits.split("\\s+")[0]));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private PharmacyOrder requireVendorOrder(Long orderId, User vendor) {
        PharmacyOrder order = pharmacyOrderRepository.findDetailedById(orderId)
                .orElseGet(() -> pharmacyOrderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Pharmacy order not found")));
        if (vendor != null && order.getPharmacyVendor() != null
                && !order.getPharmacyVendor().getId().equals(vendor.getId())) {
            throw new RuntimeException("You are not authorized to view this order");
        }
        return order;
    }

    private void notifyVendorOfCancellation(PharmacyOrder order) {
        if (order.getPharmacyVendor() == null) {
            return;
        }
        notificationService.sendPortalNotification(
                order.getPharmacyVendor(),
                "🚫 Order cancelled",
                "Pharmacy order #" + order.getId() + " for " + order.getPatient().getFullName() + " was cancelled.",
                NotificationCategory.PHARMACY,
                "/vendor/orders/" + order.getId()
        );
    }

    private boolean isSaleStatus(PharmacyOrderStatus status) {
        return status == PharmacyOrderStatus.DELIVERED || status == PharmacyOrderStatus.COMPLETED;
    }

    private double calculateOrderTotal(Prescription prescription, User pharmacyVendor) {
        if (prescription.getItems() == null || prescription.getItems().isEmpty()) {
            return DEFAULT_MEDICINE_PRICE;
        }

        List<PharmacyItem> vendorItems = vendorService.getPharmacyItemsByVendor(pharmacyVendor);
        double total = 0.0;

        for (PrescriptionItem item : prescription.getItems()) {
            double itemPrice = findMatchingItem(vendorItems, item.getMedicineName())
                    .map(PharmacyItem::getPrice)
                    .orElse(DEFAULT_MEDICINE_PRICE);
            total += itemPrice * Math.max(1, parseQuantity(item));
        }

        return total > 0 ? total : DEFAULT_MEDICINE_PRICE;
    }

    private Optional<PharmacyItem> findMatchingItem(List<PharmacyItem> catalog, String prescribedName) {
        if (catalog == null || prescribedName == null) {
            return Optional.empty();
        }
        return catalog.stream()
                .filter(pi -> medicineMatches(pi.getItemName(), prescribedName))
                .findFirst();
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
        if (current == PharmacyOrderStatus.DELIVERED && next != PharmacyOrderStatus.COMPLETED) {
            throw new RuntimeException("Delivered orders can only be marked completed.");
        }
        boolean valid = current.nextStatuses().contains(next);
        if (!valid) {
            throw new RuntimeException("Cannot change status from " + current.getDisplayName()
                    + " to " + next.getDisplayName());
        }
    }

    private void notifyPatientOfStatusChange(PharmacyOrder order) {
        PharmacyOrderStatus status = order.getStatus();
        String message = switch (status) {
            case ACCEPTED -> "Your pharmacy order #" + order.getId() + " has been accepted and is being prepared.";
            case PROCESSING -> "Your medicines for order #" + order.getId() + " are being prepared.";
            case READY_FOR_PICKUP -> "Your order #" + order.getId() + " is ready for pickup.";
            case DISPATCHED -> "Your order #" + order.getId() + " has been dispatched and is on the way.";
            case DELIVERED -> "Your order #" + order.getId() + " has been delivered. Thank you!";
            case COMPLETED -> "Your pharmacy order #" + order.getId() + " is complete.";
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

    public record MedicineAvailability(String medicineName, boolean available, Integer stock,
                                       Double price, String message) {
    }

    public record MedicineSaleLine(String medicineName, long quantitySold, double revenue) {
    }

    public record PharmacyReport(double dailySales, double monthlySales, double revenue, long orderCount,
                                 List<PharmacyItem> lowStockItems, List<PharmacyItem> expiringItems,
                                 List<MedicineSaleLine> medicineSales,
                                 Map<PharmacyOrderStatus, Long> ordersByStatus,
                                 List<PharmacyStockMovement> stockHistory) {
    }
}
