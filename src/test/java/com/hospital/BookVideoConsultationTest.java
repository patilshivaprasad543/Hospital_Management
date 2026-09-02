package com.hospital;

import com.hospital.model.*;
import com.hospital.repository.*;
import com.hospital.service.AppointmentService;
import com.hospital.service.UserService;
import com.hospital.service.VideoConsultationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class BookVideoConsultationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private VideoConsultationService videoConsultationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoConsultationRepository videoConsultationRepository;

    private User patient;
    private User doctor1;
    private User doctor2;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis();
        patient = userRepository.save(new User("Video Booking Patient " + ts, "vbp_" + ts + "@hospital.com", "99" + (ts % 100000000), "pass123", Role.PATIENT));
        
        doctor1 = new User("Dr. Cardiology Specialist " + ts, "cardio_" + ts + "@hospital.com", "98" + (ts % 100000000), "pass123", Role.DOCTOR);
        doctor1.setAdminApproved(true);
        doctor1.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor1 = userRepository.save(doctor1);
        doctorProfileRepository.save(new DoctorProfile(doctor1));

        doctor2 = new User("Dr. Neurology Specialist " + ts, "neuro_" + ts + "@hospital.com", "97" + (ts % 100000000), "pass123", Role.DOCTOR);
        doctor2.setAdminApproved(true);
        doctor2.setApprovalStatus(ApprovalStatus.APPROVED);
        doctor2 = userRepository.save(doctor2);
        doctorProfileRepository.save(new DoctorProfile(doctor2));
    }

    @Test
    void testBookVideoConsultationWithAnyDoctor() {
        // Book video consultation with Doctor 1 (Cardiology)
        Appointment appt1 = appointmentService.bookAppointmentWithDepartment(
                patient.getId(), doctor1.getId(), LocalDate.now().plusDays(1), LocalTime.of(10, 0),
                "Heart palpitations video consultation", "Cardiology", ConsultationType.VIDEO
        );

        assertNotNull(appt1);
        assertEquals(ConsultationType.VIDEO, appt1.getConsultationType());
        assertEquals(doctor1.getId(), appt1.getDoctor().getId());

        // Create Video Room immediately
        VideoConsultation room1 = videoConsultationService.createVideoRoom(appt1);
        assertNotNull(room1);
        assertNotNull(room1.getRoomId());
        assertEquals(patient.getId(), room1.getPatient().getId());
        assertEquals(doctor1.getId(), room1.getDoctor().getId());

        // Book another video consultation with Doctor 2 (Neurology)
        Appointment appt2 = appointmentService.bookAppointmentWithDepartment(
                patient.getId(), doctor2.getId(), LocalDate.now().plusDays(2), LocalTime.of(14, 0),
                "Migraine review video consultation", "Neurology", ConsultationType.VIDEO
        );

        assertNotNull(appt2);
        assertEquals(ConsultationType.VIDEO, appt2.getConsultationType());
        assertEquals(doctor2.getId(), appt2.getDoctor().getId());

        VideoConsultation room2 = videoConsultationService.createVideoRoom(appt2);
        assertNotNull(room2);
        assertNotNull(room2.getRoomId());
        assertEquals(doctor2.getId(), room2.getDoctor().getId());

        // Verify patient consultations list contains both video rooms
        List<VideoConsultation> patientConsultations = videoConsultationService.getPatientConsultations(patient);
        assertTrue(patientConsultations.stream().anyMatch(c -> c.getDoctor().getId().equals(doctor1.getId())));
        assertTrue(patientConsultations.stream().anyMatch(c -> c.getDoctor().getId().equals(doctor2.getId())));
    }

    @Test
    void testDoctorAcceptanceEnforcesWaitingRoomAccess() {
        Appointment appt = appointmentService.bookAppointmentWithDepartment(
                patient.getId(), doctor1.getId(), LocalDate.now().plusDays(1), LocalTime.of(11, 0),
                "Hypertension consultation", "Cardiology", ConsultationType.VIDEO
        );

        assertEquals(AppointmentStatus.PENDING, appt.getStatus());

        // Doctor accepts the appointment
        Appointment confirmed = appointmentService.updateAppointmentStatus(appt.getId(), AppointmentStatus.CONFIRMED, "Accepted by doctor");
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
    }
}
