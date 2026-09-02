package com.hospital.controller;

import com.hospital.model.*;
import com.hospital.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient/blood-bank")
public class PatientBloodOrderController {

    @Autowired
    private BloodOrderService bloodOrderService;

    @Autowired
    private BloodBankService bloodBankService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private UserService userService;

    private User getLoggedInPatient(HttpSession session) {
        return UserSessionHelper.getLoggedInPatient(session);
    }

    @GetMapping({"", "/", "/buy"})
    public String buyBloodPage(@RequestParam(value = "prescriptionId", required = false) Long prescriptionId,
                               HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }

        List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patient);
        Prescription selectedPrescription = null;
        if (prescriptionId != null) {
            selectedPrescription = prescriptionService.findByIdForPatient(prescriptionId, patient).orElse(null);
        }
        if (selectedPrescription == null && !prescriptions.isEmpty()) {
            selectedPrescription = prescriptions.get(0);
        }

        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        Map<BloodGroup, Long> groupCounts = bloodBankService.getAvailableCountsGrouped();

        model.addAttribute("patient", patient);
        model.addAttribute("profile", profile);
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("selectedPrescription", selectedPrescription);
        model.addAttribute("bloodGroups", BloodGroup.values());
        model.addAttribute("componentTypes", BloodComponentType.values());
        model.addAttribute("groupCounts", groupCounts);
        model.addAttribute("unitPrice", BloodOrderService.DEFAULT_UNIT_PRICE);
        model.addAttribute("activePage", "blood");

        return "patient/blood-orders/buy";
    }

    @PostMapping("/buy")
    public String submitBloodOrder(@RequestParam("prescriptionId") Long prescriptionId,
                                   @RequestParam(value = "bloodGroup", required = false) BloodGroup bloodGroup,
                                   @RequestParam(value = "componentType", required = false) BloodComponentType componentType,
                                   @RequestParam(value = "units", defaultValue = "1") Integer units,
                                   @RequestParam(value = "deliveryType", defaultValue = "HOSPITAL_WARD") String deliveryType,
                                   @RequestParam(value = "deliveryLocation", required = false) String deliveryLocation,
                                   @RequestParam(value = "patientContact", required = false) String patientContact,
                                   @RequestParam(value = "clinicalNotes", required = false) String clinicalNotes,
                                   @RequestParam(value = "urgencyLevel", defaultValue = "URGENT") String urgencyLevel,
                                   @RequestParam(value = "paymentMethod", defaultValue = "HOSPITAL_BILL") String paymentMethod,
                                   HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }

        try {
            BloodOrder order = bloodOrderService.createBloodOrder(
                    patient, prescriptionId, bloodGroup, componentType, units,
                    deliveryType, deliveryLocation, patientContact, clinicalNotes,
                    urgencyLevel, paymentMethod
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "✅ Blood Purchase Order #" + order.getOrderNumber() + " created successfully under Dr. " + order.getDoctor().getFullName() + "'s prescription! Blood bank staff will verify and cross-match units.");
            return "redirect:/patient/blood-bank/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to place blood order: " + e.getMessage());
            return "redirect:/patient/blood-bank/buy?prescriptionId=" + prescriptionId;
        }
    }

    @GetMapping("/orders")
    public String listBloodOrders(HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }

        List<BloodOrder> orders = bloodOrderService.getPatientOrders(patient);
        model.addAttribute("orders", orders);
        model.addAttribute("activePage", "blood");
        return "patient/blood-orders/list";
    }

    @PostMapping("/orders/{id}/pay")
    public String payBloodOrder(@PathVariable("id") Long id,
                                @RequestParam(value = "paymentMethod", defaultValue = "ONLINE") String paymentMethod,
                                HttpSession session, RedirectAttributes redirectAttributes) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }

        try {
            bloodOrderService.payOrder(id, paymentMethod);
            redirectAttributes.addFlashAttribute("successMessage", "✅ Payment processed successfully for Blood Order!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment failed: " + e.getMessage());
        }
        return "redirect:/patient/blood-bank/orders";
    }

    @GetMapping("/orders/{id}/slip")
    public String viewRequisitionSlip(@PathVariable("id") Long id, HttpSession session, Model model) {
        User patient = getLoggedInPatient(session);
        if (patient == null) {
            return "redirect:/login/patient";
        }

        BloodOrder order = bloodOrderService.findById(id).orElse(null);
        if (order == null || (!order.getPatient().getId().equals(patient.getId()) && patient.getRole() == Role.PATIENT)) {
            return "redirect:/patient/blood-bank/orders";
        }

        model.addAttribute("order", order);
        return "patient/blood-orders/slip";
    }
}
