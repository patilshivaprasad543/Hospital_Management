package com.hospital.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacy_stock_movements")
public class PharmacyStockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private PharmacyItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private PharmacyOrder pharmacyOrder;

    @Enumerated(EnumType.STRING)
    private StockMovementType movementType;

    private Integer quantityChange;
    private Integer quantityAfter;

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public PharmacyStockMovement() {
    }

    public PharmacyStockMovement(User vendor, PharmacyItem item, PharmacyOrder pharmacyOrder,
                                 StockMovementType movementType, Integer quantityChange,
                                 Integer quantityAfter, String notes) {
        this.vendor = vendor;
        this.item = item;
        this.pharmacyOrder = pharmacyOrder;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.quantityAfter = quantityAfter;
        this.notes = notes;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getVendor() {
        return vendor;
    }

    public void setVendor(User vendor) {
        this.vendor = vendor;
    }

    public PharmacyItem getItem() {
        return item;
    }

    public void setItem(PharmacyItem item) {
        this.item = item;
    }

    public PharmacyOrder getPharmacyOrder() {
        return pharmacyOrder;
    }

    public void setPharmacyOrder(PharmacyOrder pharmacyOrder) {
        this.pharmacyOrder = pharmacyOrder;
    }

    public StockMovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(StockMovementType movementType) {
        this.movementType = movementType;
    }

    public Integer getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Integer quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Integer quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
