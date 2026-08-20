package com.hospital.service;

import com.hospital.model.PharmacyItem;
import com.hospital.model.PharmacyOrder;
import com.hospital.model.PharmacyOrderStatus;
import com.hospital.model.Prescription;
import com.hospital.model.PrescriptionItem;
import com.hospital.model.Role;
import com.hospital.model.User;
import com.hospital.model.VendorType;
import com.hospital.repository.InvoiceRepository;
import com.hospital.repository.PharmacyOrderRepository;
import com.hospital.repository.PharmacyStockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyWorkflowServiceTest {

    @Mock private PharmacyOrderRepository pharmacyOrderRepository;
    @Mock private PharmacyStockMovementRepository stockMovementRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private NotificationService notificationService;
    @Mock private VendorService vendorService;
    @Mock private BillingService billingService;

    @InjectMocks
    private PharmacyWorkflowService pharmacyWorkflowService;

    private User patient;
    private User vendor;
    private Prescription prescription;
    private PharmacyItem catalogItem;

    @BeforeEach
    void setUp() {
        patient = new User("John Doe", "patient@smartcare360.com", "9876543214", "x", Role.PATIENT);
        patient.setId(1L);
        vendor = new User("MediPlus Pharmacy", "pharmacy@smartcare360.com", "9876543216", "x", Role.VENDOR);
        vendor.setId(2L);
        vendor.setVendorType(VendorType.PHARMACY);
        prescription = new Prescription();
        prescription.setId(10L);
        prescription.setDiagnosis("Fever");
        prescription.addItem(new PrescriptionItem("Paracetamol 650mg", "650mg", "1-0-1", "5 Days", ""));
        catalogItem = new PharmacyItem("Paracetamol 650mg", "Analgesic", 35.0, 500, "strip", vendor);
    }

    @Test
    void placeOrderRejectsEmptyPrescription() {
        Prescription empty = new Prescription();
        empty.setId(11L);
        RuntimeException error = assertThrows(RuntimeException.class,
                () -> pharmacyWorkflowService.placeOrder(patient, empty, vendor, "123 Health Ave"));
        assertTrue(error.getMessage().toLowerCase().contains("no medicines"));
    }

    @Test
    void placeOrderSavesOrderAndUsesCatalogPrice() {
        when(pharmacyOrderRepository.existsByPrescriptionAndStatusNotIn(any(), anyList())).thenReturn(false);
        when(vendorService.getPharmacyItemsByVendor(vendor)).thenReturn(List.of(catalogItem));
        when(pharmacyOrderRepository.save(any(PharmacyOrder.class))).thenAnswer(invocation -> {
            PharmacyOrder order = invocation.getArgument(0);
            order.setId(40L);
            return order;
        });

        PharmacyOrder saved = pharmacyWorkflowService.placeOrder(patient, prescription, vendor, "123 Health Ave");

        assertEquals(40L, saved.getId());
        assertEquals(PharmacyOrderStatus.PLACED, saved.getStatus());
        assertEquals(175.0, saved.getTotalPrice());
        assertTrue(saved.getOrderSummary().contains("Paracetamol 650mg"));
    }

    @Test
    void updateOrderStatusRejectsWrongVendor() {
        PharmacyOrder order = new PharmacyOrder(patient, prescription, vendor, 175.0, "summary", "addr");
        order.setId(40L);
        User otherVendor = new User("Other", "other@smartcare360.com", "9000000001", "x", Role.VENDOR);
        otherVendor.setId(99L);
        when(pharmacyOrderRepository.findById(40L)).thenReturn(Optional.of(order));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> pharmacyWorkflowService.updateOrderStatus(40L, PharmacyOrderStatus.ACCEPTED, otherVendor, "note"));
        assertTrue(error.getMessage().toLowerCase().contains("not authorized"));
    }

    @Test
    void updateOrderStatusRejectsInvalidTransition() {
        PharmacyOrder order = new PharmacyOrder(patient, prescription, vendor, 175.0, "summary", "addr");
        order.setId(40L);
        order.setStatus(PharmacyOrderStatus.PLACED);
        when(pharmacyOrderRepository.findById(40L)).thenReturn(Optional.of(order));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> pharmacyWorkflowService.updateOrderStatus(40L, PharmacyOrderStatus.DISPATCHED, vendor, "skip"));
        assertTrue(error.getMessage().toLowerCase().contains("cannot change status"));
    }
}
