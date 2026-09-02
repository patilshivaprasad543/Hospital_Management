package com.hospital.repository;

import com.hospital.model.BloodOrder;
import com.hospital.model.BloodOrderStatus;
import com.hospital.model.Prescription;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodOrderRepository extends JpaRepository<BloodOrder, Long> {

    List<BloodOrder> findByPatientOrderByCreatedAtDesc(User patient);

    List<BloodOrder> findByDoctorOrderByCreatedAtDesc(User doctor);

    List<BloodOrder> findByStatusOrderByCreatedAtDesc(BloodOrderStatus status);

    List<BloodOrder> findAllByOrderByCreatedAtDesc();

    List<BloodOrder> findByPrescription(Prescription prescription);

    Optional<BloodOrder> findByOrderNumber(String orderNumber);

    long countByStatus(BloodOrderStatus status);

    long countByPatient(User patient);
}
