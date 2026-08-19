package com.hospital.repository;

import com.hospital.model.DoctorLeave;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, Long> {
    boolean existsByDoctorAndLeaveDate(User doctor, LocalDate leaveDate);
    List<DoctorLeave> findByDoctorAndLeaveDateBetween(User doctor, LocalDate start, LocalDate end);
    List<DoctorLeave> findByDoctorOrderByLeaveDateDesc(User doctor);
}
