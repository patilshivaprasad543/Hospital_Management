package com.hospital.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PharmacyOrderStatusTest {

    @Test
    void pipelineAllowsExpectedNextSteps() {
        assertTrue(PharmacyOrderStatus.PLACED.nextStatuses().contains(PharmacyOrderStatus.ACCEPTED));
        assertTrue(PharmacyOrderStatus.ACCEPTED.nextStatuses().contains(PharmacyOrderStatus.PROCESSING));
        assertTrue(PharmacyOrderStatus.PROCESSING.nextStatuses().contains(PharmacyOrderStatus.READY_FOR_PICKUP));
        assertTrue(PharmacyOrderStatus.PROCESSING.nextStatuses().contains(PharmacyOrderStatus.DISPATCHED));
        assertTrue(PharmacyOrderStatus.READY_FOR_PICKUP.nextStatuses().contains(PharmacyOrderStatus.DISPATCHED));
        assertTrue(PharmacyOrderStatus.DISPATCHED.nextStatuses().contains(PharmacyOrderStatus.DELIVERED));
        assertTrue(PharmacyOrderStatus.DELIVERED.nextStatuses().contains(PharmacyOrderStatus.COMPLETED));
    }

    @Test
    void onlyCompletedAndCancelledAreTerminal() {
        assertFalse(PharmacyOrderStatus.DELIVERED.isTerminal());
        assertTrue(PharmacyOrderStatus.COMPLETED.isTerminal());
        assertTrue(PharmacyOrderStatus.CANCELLED.isTerminal());
        assertTrue(PharmacyOrderStatus.COMPLETED.nextStatuses().isEmpty());
    }

    @Test
    void displayNamesMatchPharmacyFlow() {
        assertEquals("Pending", PharmacyOrderStatus.PLACED.getDisplayName());
        assertEquals("Preparing", PharmacyOrderStatus.PROCESSING.getDisplayName());
        assertEquals("Ready for pickup", PharmacyOrderStatus.READY_FOR_PICKUP.getDisplayName());
        assertEquals("Completed", PharmacyOrderStatus.COMPLETED.getDisplayName());
    }
}
