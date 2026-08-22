package com.hospital.repository;

import com.hospital.model.Appointment;
import com.hospital.model.AppointmentState;
import com.hospital.model.AppointmentStatus;
import com.hospital.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientOrderByCreatedAtDesc(User patient);

    List<Appointment> findByDoctorOrderByCreatedAtDesc(User doctor);

    List<Appointment> findByDoctorAndStatusOrderByCreatedAtDesc(User doctor, AppointmentStatus status);

    List<Appointment> findByDoctorAndAppointmentDateAndStateInOrderByCheckedInAtAsc(
            User doctor, LocalDate appointmentDate, Collection<AppointmentState> states);

    long countByStatus(AppointmentStatus status);

    long countByAppointmentDate(LocalDate appointmentDate);

    boolean existsByDoctorAndAppointmentDateAndAppointmentTimeAndStatusIn(
            User doctor, LocalDate appointmentDate, LocalTime appointmentTime, Collection<AppointmentStatus> statuses);

    List<Appointment> findByDoctorAndAppointmentDateAndStatusIn(
            User doctor, LocalDate appointmentDate, Collection<AppointmentStatus> statuses);

    long countByDoctorAndAppointmentDateAndStatusIn(
            User doctor, LocalDate appointmentDate, Collection<AppointmentStatus> statuses);
}
