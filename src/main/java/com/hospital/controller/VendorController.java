package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.LabWorkflowService;
import com.hospital.service.NotificationService;
import com.hospital.service.PdfService;
import com.hospital.service.PharmacyWorkflowService;
import com.hospital.service.UserService;
import com.hospital.service.VendorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/vendor")
public class VendorController {

    @org.springframework.beans.factory.annotation.Autowired
    private UserService userService;

    @org.springframework.beans.factory.annotation.Autowired
    private VendorService vendorService;

    @org.springframework.beans.factory.annotation.Autowired
    private LabWorkflowService labWorkflowService;

    @org.springframework.beans.factory.annotation.Autowired
    private PharmacyWorkflowService pharmacyWorkflowService;

    @org.springframework.beans.factory.annotation.Autowired
    private NotificationService notificationService;

    @org.springframework.beans.factory.annotation.Autowired
    private PdfService pdfService;

    @ModelAttribute
    public void vendorNav(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) {
            return;
        }
        if (vendor.getVendorType() == VendorType.PHARMACY) {
            model.addAttribute("vendorTag", "Pharmacy");
        } else if (vendor.getVendorType() == VendorType.LABORATORY) {
            model.addAttribute("vendorTag", "Laboratory");
        }
        model.addAttribute("unreadCount", notificationService.getUnreadCount(vendor));
    }

    private User getLoggedInVendor(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null && user.getRole() == Role.VENDOR) {
            return user;
        }
        return null;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        VendorProfile profile = userService.getVendorProfile(vendor).orElse(new VendorProfile(vendor));

        model.addAttribute("vendor", vendor);
        model.addAttribute("profile", profile);

        if (vendor.getVendorType() == VendorType.LABORATORY) {
            model.addAttribute("labTests", vendorService.getLabTestsByVendor(vendor));
            model.addAttribute("labRequests", labWorkflowService.getVendorLabRequests(vendor));
            return "vendor/lab-dashboard";
        } else if (vendor.getVendorType() == VendorType.PHARMACY) {
            List<PharmacyItem> items = vendorService.getPharmacyItemsByVendor(vendor);
            List<PharmacyOrder> orders = pharmacyWorkflowService.getVendorOrders(vendor);
            model.addAttribute("pharmacyItems", items);
            model.addAttribute("pharmacyOrders", orders);
            model.addAttribute("orderCounts", pharmacyWorkflowService.countOrdersByStatus(vendor));
            model.addAttribute("lowStockItems", items.stream().filter(PharmacyItem::isLowStock).toList());
            model.addAttribute("expiringItems", items.stream().filter(i -> i.isExpired() || i.isNearExpiry()).toList());
            return "vendor/pharmacy-dashboard";
        }

        model.addAttribute("labTests", vendorService.getLabTestsByVendor(vendor));
        model.addAttribute("pharmacyItems", vendorService.getPharmacyItemsByVendor(vendor));
        return "vendor/dashboard";
    }

    @GetMapping("/inventory")
    public String inventory(HttpSession session, Model model) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        List<PharmacyItem> items = vendorService.getPharmacyItemsByVendor(vendor);
        model.addAttribute("vendor", vendor);
        model.addAttribute("pharmacyItems", items);
        model.addAttribute("lowStockItems", items.stream().filter(PharmacyItem::isLowStock).toList());
        model.addAttribute("expiringItems", items.stream().filter(i -> i.isExpired() || i.isNearExpiry()).toList());
        return "vendor/inventory";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(value = "status", required = false) PharmacyOrderStatus status,
                         HttpSession session, Model model) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        List<PharmacyOrder> orders = pharmacyWorkflowService.getVendorOrders(vendor);
        if (status != null) {
            orders = orders.stream().filter(o -> o.getStatus() == status).toList();
        }
        model.addAttribute("vendor", vendor);
        model.addAttribute("pharmacyOrders", orders);
        model.addAttribute("filterStatus", status);
        model.addAttribute("orderCounts", pharmacyWorkflowService.countOrdersByStatus(vendor));
        model.addAttribute("allStatuses", PharmacyOrderStatus.values());
        return "vendor/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable("id") Long id, HttpSession session, Model model,
                              RedirectAttributes redirectAttributes) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        try {
            PharmacyOrder order = pharmacyWorkflowService.getVendorOrder(id, vendor);
            model.addAttribute("vendor", vendor);
            model.addAttribute("order", order);
            model.addAttribute("availability", pharmacyWorkflowService.checkAvailability(order));
            model.addAttribute("invoice", pharmacyWorkflowService.getOrderInvoice(order).orElse(null));
            return "vendor/order-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/vendor/orders";
        }
    }

    @GetMapping("/reports")
    public String reports(HttpSession session, Model model) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        model.addAttribute("vendor", vendor);
        model.addAttribute("report", pharmacyWorkflowService.buildReport(vendor));
        return "vendor/reports";
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) {
            return "redirect:/login/vendor";
        }
        model.addAttribute("notifications", notificationService.getNotificationsForUser(vendor));
        model.addAttribute("unreadCount", notificationService.getUnreadCount(vendor));
        model.addAttribute("loggedInUser", vendor);
        return "vendor/notifications";
    }

    @PostMapping("/lab-request/{id}/upload-report")
    public String uploadReport(@PathVariable("id") Long id,
                               @RequestParam("reportResult") String reportResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        labWorkflowService.uploadReport(id, reportResult);
        redirectAttributes.addFlashAttribute("successMessage", "Diagnostic report uploaded and sent to Patient!");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/pharmacy-order/{id}/update-status")
    public String updateOrderStatus(@PathVariable("id") Long id,
                                    @RequestParam("status") PharmacyOrderStatus status,
                                    @RequestParam(value = "trackingNotes", required = false) String trackingNotes,
                                    @RequestParam(value = "returnTo", required = false) String returnTo,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        try {
            pharmacyWorkflowService.updateOrderStatus(id, status, vendor, trackingNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Order status updated to " + status.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        if (returnTo != null && returnTo.contains("/vendor/orders/")) {
            return "redirect:/vendor/orders/" + id;
        }
        return "redirect:/vendor/orders";
    }

    @PostMapping("/pharmacy-order/{id}/verify-prescription")
    public String verifyPrescription(@PathVariable("id") Long id,
                                     @RequestParam(value = "verified", defaultValue = "true") boolean verified,
                                     @RequestParam(value = "notes", required = false) String notes,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        try {
            pharmacyWorkflowService.verifyPrescription(id, vendor, verified, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    verified ? "Prescription verified." : "Prescription marked as not verified.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/vendor/orders/" + id;
    }

    @GetMapping("/orders/{id}/invoice.pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable("id") Long id, HttpSession session) {
        User vendor = requirePharmacy(session);
        if (vendor == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        PharmacyOrder order = pharmacyWorkflowService.getVendorOrder(id, vendor);
        Invoice invoice = pharmacyWorkflowService.getOrderInvoice(order)
                .orElseThrow(() -> new RuntimeException("Invoice is created after the order is accepted."));
        byte[] pdf = pdfService.generateInvoicePdf(invoice, order.getPatient());
        return pdfService.download(pdf, "pharmacy-invoice-" + invoice.getInvoiceNumber() + ".pdf");
    }

    @GetMapping("/orders/{id}/pdf")
    public ResponseEntity<byte[]> downloadOrderPdf(@PathVariable("id") Long id, HttpSession session) {
        User vendor = requirePharmacy(session);
        if (vendor == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        PharmacyOrder order = pharmacyWorkflowService.getVendorOrder(id, vendor);
        return pdfService.download(pdfService.generatePharmacyOrderPdf(order),
                "pharmacy-order-" + order.getId() + ".pdf");
    }

    @GetMapping("/lab-request/{id}/pdf")
    public ResponseEntity<byte[]> downloadLabReportPdf(@PathVariable("id") Long id, HttpSession session) {
        User vendor = requireLaboratory(session);
        if (vendor == null) {
            return ResponseEntity.status(302).header(HttpHeaders.LOCATION, "/login").build();
        }
        return labWorkflowService.findByIdForVendor(id, vendor)
                .filter(lab -> lab.getReportResult() != null)
                .map(lab -> pdfService.download(pdfService.generateLabReportPdf(lab, lab.getPatient()),
                        "lab-report-" + lab.getId() + ".pdf"))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lab-test/add")
    public String addLabTest(@ModelAttribute LabTest labTest,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        labTest.setVendor(vendor);
        vendorService.saveLabTest(labTest);
        redirectAttributes.addFlashAttribute("successMessage", "Lab Test added successfully!");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/lab-test/{id}/delete")
    public String deleteLabTest(@PathVariable("id") Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        vendorService.deleteLabTest(id);
        redirectAttributes.addFlashAttribute("successMessage", "Lab Test removed.");
        return "redirect:/vendor/dashboard";
    }

    @PostMapping("/pharmacy-item/add")
    public String addPharmacyItem(@RequestParam("itemName") String itemName,
                                  @RequestParam("category") String category,
                                  @RequestParam("price") Double price,
                                  @RequestParam("stockQuantity") Integer stockQuantity,
                                  @RequestParam(value = "manufacturer", required = false) String manufacturer,
                                  @RequestParam(value = "batchNumber", required = false) String batchNumber,
                                  @RequestParam(value = "expiryDate", required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                  @RequestParam(value = "description", required = false) String description,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        PharmacyItem item = new PharmacyItem(itemName, category, price, stockQuantity, description, vendor);
        item.setManufacturer(manufacturer);
        item.setBatchNumber(batchNumber);
        item.setExpiryDate(expiryDate);
        vendorService.savePharmacyItem(item);
        redirectAttributes.addFlashAttribute("successMessage", "Medicine added to inventory.");
        return "redirect:/vendor/inventory";
    }

    @PostMapping("/pharmacy-item/{id}/update")
    public String updatePharmacyItem(@PathVariable("id") Long id,
                                     @RequestParam("itemName") String itemName,
                                     @RequestParam("category") String category,
                                     @RequestParam("price") Double price,
                                     @RequestParam("stockQuantity") Integer stockQuantity,
                                     @RequestParam(value = "manufacturer", required = false) String manufacturer,
                                     @RequestParam(value = "batchNumber", required = false) String batchNumber,
                                     @RequestParam(value = "expiryDate", required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
                                     @RequestParam(value = "description", required = false) String description,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        try {
            PharmacyItem item = vendorService.getPharmacyItemForVendor(id, vendor);
            item.setItemName(itemName);
            item.setCategory(category);
            item.setPrice(price);
            item.setStockQuantity(stockQuantity);
            item.setManufacturer(manufacturer);
            item.setBatchNumber(batchNumber);
            item.setExpiryDate(expiryDate);
            item.setDescription(description);
            vendorService.savePharmacyItem(item);
            redirectAttributes.addFlashAttribute("successMessage", "Medicine details updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/vendor/inventory";
    }

    @PostMapping("/pharmacy-item/{id}/restock")
    public String restock(@PathVariable("id") Long id,
                          @RequestParam("quantity") Integer quantity,
                          @RequestParam(value = "notes", required = false) String notes,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        User vendor = requirePharmacy(session);
        if (vendor == null) return "redirect:/login";
        try {
            if (quantity == null || quantity <= 0) {
                throw new RuntimeException("Enter a positive quantity to receive.");
            }
            pharmacyWorkflowService.receiveStock(vendor, id, quantity, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Stock updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/vendor/inventory";
    }

    @PostMapping("/pharmacy-item/{id}/delete")
    public String deletePharmacyItem(@PathVariable("id") Long id,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        vendorService.deletePharmacyItem(id);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy item removed.");
        return "redirect:/vendor/inventory";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        VendorProfile profile = userService.getVendorProfile(vendor).orElse(new VendorProfile(vendor));
        model.addAttribute("vendor", vendor);
        model.addAttribute("profile", profile);
        model.addAttribute("vendorTypes", VendorType.values());
        return "vendor/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute VendorProfile profile,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null) return "redirect:/login";

        userService.updateVendorProfile(vendor.getId(), profile);
        redirectAttributes.addFlashAttribute("successMessage", "Pharmacy profile updated.");
        return "redirect:/vendor/profile";
    }

    private User requirePharmacy(HttpSession session) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null || vendor.getVendorType() != VendorType.PHARMACY) {
            return null;
        }
        return vendor;
    }

    private User requireLaboratory(HttpSession session) {
        User vendor = getLoggedInVendor(session);
        if (vendor == null || vendor.getVendorType() != VendorType.LABORATORY) {
            return null;
        }
        return vendor;
    }
}
