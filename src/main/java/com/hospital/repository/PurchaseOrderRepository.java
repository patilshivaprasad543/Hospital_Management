package com.hospital.repository;

import com.hospital.model.PurchaseOrder;
import com.hospital.model.PurchaseOrderStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByVendorOrderByCreatedAtDesc(User vendor);
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);
}
