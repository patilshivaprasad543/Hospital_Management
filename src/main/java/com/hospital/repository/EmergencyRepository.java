package com.hospital.repository;

import com.hospital.model.Emergency;
import com.hospital.model.EmergencyPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyRepository extends JpaRepository<Emergency, Long> {
    List<Emergency> findByStatusNotOrderByPriorityAscArrivalTimeDesc(String status);
    List<Emergency> findByPriority(EmergencyPriority priority);
}
