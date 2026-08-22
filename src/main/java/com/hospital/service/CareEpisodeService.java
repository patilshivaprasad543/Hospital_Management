package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.model.*;
import com.hospital.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CareEpisodeService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final int DEFAULT_CONSULT_MINUTES = 12;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private LabWorkflowService labWorkflowService;

    @Autowired
    private PharmacyWorkflowService pharmacyWorkflowService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private UserService userService;

    @Autowired
    private VisitChecklistCompletionRepository checklistRepository;

    public List<CareEpisodeDto> getPatientEpisodes(User patient) {
        return appointmentRepository.findByPatientOrderByCreatedAtDesc(patient).stream()
                .map(app -> buildEpisode(app, patient))
                .collect(Collectors.toList());
    }

    public CareEpisodeDto getEpisode(Long appointmentId, User patient) {
        Appointment app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!app.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized access to care episode.");
        }
        return buildEpisode(app, patient);
    }

    public Optional<CareEpisodeDto> getActiveEpisode(User patient) {
        return getPatientEpisodes(patient).stream()
                .filter(CareEpisodeDto::isActive)
                .findFirst();
    }

    public QueueStatusDto getQueueStatus(Appointment appointment) {
        QueueStatusDto queue = new QueueStatusDto();
        queue.setDoctorName(appointment.getDoctor().getFullName());

        AppointmentState state = appointment.getState();
        if (state != AppointmentState.CHECKED_IN && state != AppointmentState.IN_CONSULTATION) {
            queue.setInQueue(false);
            return queue;
        }

        List<AppointmentState> queueStates = List.of(
                AppointmentState.CHECKED_IN, AppointmentState.IN_CONSULTATION);
        List<Appointment> waiting = appointmentRepository
                .findByDoctorAndAppointmentDateAndStateInOrderByCheckedInAtAsc(
                        appointment.getDoctor(), appointment.getAppointmentDate(), queueStates);

        int position = 1;
        for (int i = 0; i < waiting.size(); i++) {
            if (waiting.get(i).getId().equals(appointment.getId())) {
                position = i + 1;
                break;
            }
        }

        int avgMinutes = calculateAverageConsultMinutes(appointment.getDoctor());
        int ahead = Math.max(0, position - 1);
        int estimatedWait = state == AppointmentState.IN_CONSULTATION ? 0 : ahead * avgMinutes;

        queue.setInQueue(true);
        queue.setPosition(position);
        queue.setTotalWaiting(waiting.size());
        queue.setAverageConsultMinutes(avgMinutes);
        queue.setEstimatedWaitMinutes(estimatedWait);
        queue.setQueueTicket(appointment.getQueueTicket());
        return queue;
    }

    public CarePassportDto buildPassport(User patient) {
        PatientProfile profile = userService.getPatientProfile(patient).orElse(new PatientProfile(patient));
        CarePassportDto passport = new CarePassportDto();
        passport.setPassportId("SC360-" + String.format("%06d", patient.getId()));
        passport.setPatientName(patient.getFullName());
        passport.setEmail(patient.getEmail());
        passport.setBloodGroup(profile.getBloodGroup() != null ? profile.getBloodGroup() : "Not recorded");
        passport.setAllergies(profile.getAllergies() != null && !profile.getAllergies().isBlank()
                ? profile.getAllergies() : "None recorded");
        passport.setEmergencyContactName(profile.getEmergencyContactName());
        passport.setEmergencyContactPhone(profile.getEmergencyContactPhone());
        passport.setGeneratedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));

        List<Prescription> prescriptions = prescriptionRepository.findByPatientOrderByCreatedAtDesc(patient);
        Set<String> medicines = new LinkedHashSet<>();
        Set<String> diagnoses = new LinkedHashSet<>();
        for (Prescription rx : prescriptions) {
            if (rx.getDiagnosis() != null && !rx.getDiagnosis().isBlank()) {
                diagnoses.add(rx.getDiagnosis());
            }
            if (rx.getItems() != null) {
                for (PrescriptionItem item : rx.getItems()) {
                    medicines.add(item.getMedicineName() + " (" + item.getDosage() + ", " + item.getFrequency() + ")");
                }
            }
            if (medicines.size() >= 8) break;
        }
        passport.setCurrentMedicines(new ArrayList<>(medicines));
        passport.setRecentDiagnoses(diagnoses.stream().limit(5).collect(Collectors.toList()));

        List<String> labSummaries = new ArrayList<>();
        for (LabRequest lab : labWorkflowService.getPatientLabRequests(patient)) {
            if ("REPORT_READY".equals(lab.getStatus()) && lab.getReportResult() != null) {
                String summary = lab.getTestName() + ": " + truncate(lab.getReportResult(), 80);
                labSummaries.add(summary);
            }
            if (labSummaries.size() >= 5) break;
        }
        passport.setRecentLabResults(labSummaries);
        return passport;
    }

    @Transactional
    public void toggleChecklistItem(Long appointmentId, User patient, String itemKey) {
        Appointment app = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        if (!app.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        Optional<VisitChecklistCompletion> existing = checklistRepository
                .findByAppointmentIdAndPatientIdAndItemKey(appointmentId, patient.getId(), itemKey);

        if (existing.isPresent()) {
            VisitChecklistCompletion record = existing.get();
            record.setCompleted(!record.isCompleted());
            record.setCompletedAt(LocalDateTime.now());
            checklistRepository.save(record);
        } else {
            VisitChecklistCompletion record = new VisitChecklistCompletion();
            record.setAppointmentId(appointmentId);
            record.setPatientId(patient.getId());
            record.setItemKey(itemKey);
            record.setCompleted(true);
            checklistRepository.save(record);
        }
    }

    private CareEpisodeDto buildEpisode(Appointment appointment, User patient) {
        CareEpisodeDto episode = new CareEpisodeDto();
        episode.setAppointmentId(appointment.getId());
        episode.setDoctorName(appointment.getDoctor().getFullName());
        episode.setDepartment(appointment.getDepartmentCategory() != null
                ? appointment.getDepartmentCategory() : "General Consultation");
        episode.setAppointmentDate(appointment.getAppointmentDate().format(DATE_FMT));
        episode.setAppointmentTime(appointment.getAppointmentTime().toString());
        episode.setState(appointment.getState());
        episode.setStatus(appointment.getStatus());
        episode.setActive(isActiveEpisode(appointment));

        Optional<Consultation> consultation = consultationRepository.findByAppointmentId(appointment.getId());
        Optional<Prescription> prescription = prescriptionRepository.findByAppointmentId(appointment.getId());
        List<LabRequest> visitLabs = findVisitLabRequests(appointment, patient);
        List<PharmacyOrder> visitOrders = findVisitPharmacyOrders(prescription, patient);
        List<Invoice> visitInvoices = findVisitInvoices(appointment, patient);

        boolean rejected = appointment.getState() == AppointmentState.REJECTED
                || appointment.getState() == AppointmentState.CANCELLED;
        boolean completed = appointment.getState() == AppointmentState.COMPLETED
                || appointment.getStatus() == AppointmentStatus.COMPLETED;

        episode.getSteps().add(step("book", "Appointment Booked", "Your visit request was submitted",
                "📅", EpisodeStepStatus.COMPLETED, null, null, null));

        EpisodeStepStatus approvalStatus = rejected ? EpisodeStepStatus.FAILED
                : (isAtLeast(appointment.getState(), AppointmentState.CONFIRMED)
                ? EpisodeStepStatus.COMPLETED
                : (appointment.getState() == AppointmentState.PENDING_DOCTOR_APPROVAL
                ? EpisodeStepStatus.ACTIVE : EpisodeStepStatus.PENDING));
        episode.getSteps().add(step("approval", "Doctor Approval", "Waiting for doctor confirmation",
                "✅", approvalStatus,
                rejected ? "Visit was not approved" : null,
                "/patient/appointments", "View Appointment"));

        if (!rejected) {
            EpisodeStepStatus checkInStatus = completed || isAtLeast(appointment.getState(), AppointmentState.CHECKED_IN)
                    ? EpisodeStepStatus.COMPLETED
                    : (appointment.getState() == AppointmentState.CONFIRMED
                    ? EpisodeStepStatus.ACTIVE : EpisodeStepStatus.PENDING);
            episode.getSteps().add(step("checkin", "Digital Check-In", "Check in and receive queue ticket",
                    "🎟️", checkInStatus,
                    appointment.getQueueTicket() != null ? "Ticket: " + appointment.getQueueTicket() : null,
                    appointment.getState() == AppointmentState.CONFIRMED ? "/patient/appointments" : null,
                    appointment.getState() == AppointmentState.CONFIRMED ? "Check In" : null));

            EpisodeStepStatus consultStatus = consultation.map(c -> c.getCompletedAt() != null
                            ? EpisodeStepStatus.COMPLETED : EpisodeStepStatus.ACTIVE)
                    .orElse(appointment.getState() == AppointmentState.IN_CONSULTATION
                            ? EpisodeStepStatus.ACTIVE
                            : (isAtLeast(appointment.getState(), AppointmentState.CHECKED_IN)
                            && appointment.getState() != AppointmentState.CHECKED_IN
                            ? EpisodeStepStatus.COMPLETED : EpisodeStepStatus.PENDING));
            if (appointment.getState() == AppointmentState.CHECKED_IN) {
                consultStatus = EpisodeStepStatus.ACTIVE;
            }
            String consultDetail = consultation.map(c -> c.getDiagnosis() != null
                    ? "Diagnosis: " + c.getDiagnosis() : "Consultation in progress").orElse(null);
            episode.getSteps().add(step("consult", "Consultation", "Meet with your doctor",
                    "🩺", consultStatus, consultDetail, null, null));

            EpisodeStepStatus rxStatus = prescription.isPresent() ? EpisodeStepStatus.COMPLETED
                    : (consultation.isPresent() && consultation.get().getCompletedAt() == null
                    ? EpisodeStepStatus.PENDING
                    : (consultation.isPresent() ? EpisodeStepStatus.ACTIVE : EpisodeStepStatus.PENDING));
            episode.getSteps().add(step("prescription", "Prescription", "Receive medicines and instructions",
                    "💊", rxStatus,
                    prescription.map(rx -> "Diagnosis: " + nullSafe(rx.getDiagnosis())).orElse(null),
                    "/patient/prescriptions", prescription.isPresent() ? "View Prescription" : null));

            boolean hasLabs = !visitLabs.isEmpty();
            EpisodeStepStatus labStatus = !hasLabs ? EpisodeStepStatus.SKIPPED
                    : (visitLabs.stream().allMatch(l -> "REPORT_READY".equals(l.getStatus()))
                    ? EpisodeStepStatus.COMPLETED
                    : (visitLabs.stream().anyMatch(l -> "PROCESSING".equals(l.getStatus()))
                    ? EpisodeStepStatus.ACTIVE : EpisodeStepStatus.PENDING));
            episode.getSteps().add(step("lab", "Lab Tests", "Diagnostic tests if ordered",
                    "🔬", labStatus, hasLabs ? summarizeLabs(visitLabs) : "No lab tests this visit",
                    "/patient/lab-reports", hasLabs ? "Track Lab" : null));

            boolean hasPharmacy = !visitOrders.isEmpty();
            EpisodeStepStatus pharmacyStatus = !hasPharmacy ? EpisodeStepStatus.SKIPPED
                    : (visitOrders.stream().allMatch(o -> o.getStatus() == PharmacyOrderStatus.COMPLETED
                    || o.getStatus() == PharmacyOrderStatus.DELIVERED)
                    ? EpisodeStepStatus.COMPLETED : EpisodeStepStatus.ACTIVE);
            episode.getSteps().add(step("pharmacy", "Pharmacy", "Order and collect medicines",
                    "🏪", pharmacyStatus,
                    hasPharmacy ? "Status: " + visitOrders.get(0).getStatus().getDisplayName() : "No pharmacy order yet",
                    "/patient/pharmacy-orders", hasPharmacy ? "Track Order"
                            : (prescription.isPresent() ? "Order Medicines" : null)));

            boolean hasBills = !visitInvoices.isEmpty();
            boolean allPaid = hasBills && visitInvoices.stream()
                    .allMatch(i -> i.getPaymentStatus() == PaymentStatus.PAID);
            EpisodeStepStatus billStatus = !hasBills ? EpisodeStepStatus.SKIPPED
                    : (allPaid ? EpisodeStepStatus.COMPLETED : EpisodeStepStatus.ACTIVE);
            episode.getSteps().add(step("billing", "Billing", "Pay for consultation and services",
                    "💳", billStatus,
                    hasBills ? billSummary(visitInvoices) : "No bill generated yet",
                    "/patient/bills", hasBills && !allPaid ? "Pay Bill" : null));

            EpisodeStepStatus doneStatus = completed ? EpisodeStepStatus.COMPLETED : EpisodeStepStatus.PENDING;
            episode.getSteps().add(step("complete", "Care Complete", "Visit wrapped up",
                    "🏁", doneStatus, completed ? "This care episode is complete" : null, null, null));
        }

        episode.setQueueStatus(getQueueStatus(appointment));
        episode.setChecklist(buildChecklist(appointment, patient, consultation, prescription,
                visitLabs, visitOrders, visitInvoices, completed));
        computeProgress(episode);
        return episode;
    }

    private List<ChecklistItemDto> buildChecklist(Appointment appointment, User patient,
                                                   Optional<Consultation> consultation,
                                                   Optional<Prescription> prescription,
                                                   List<LabRequest> labs,
                                                   List<PharmacyOrder> orders,
                                                   List<Invoice> invoices,
                                                   boolean visitCompleted) {
        if (consultation.isEmpty() || consultation.get().getCompletedAt() == null) {
            return List.of();
        }

        Map<String, Boolean> manual = checklistRepository
                .findByAppointmentIdAndPatientId(appointment.getId(), patient.getId()).stream()
                .collect(Collectors.toMap(VisitChecklistCompletion::getItemKey,
                        VisitChecklistCompletion::isCompleted, (a, b) -> b));

        List<ChecklistItemDto> items = new ArrayList<>();

        for (LabRequest lab : labs) {
            boolean autoDone = "REPORT_READY".equals(lab.getStatus());
            boolean done = autoDone || Boolean.TRUE.equals(manual.get("lab-" + lab.getId()));
            items.add(new ChecklistItemDto(
                    "lab-" + lab.getId(),
                    "Get lab test: " + lab.getTestName(),
                    lab.getLabVendor() != null
                            ? "At " + lab.getLabVendor().getFullName() + " — " + lab.getStatus()
                            : "Select a laboratory vendor",
                    "/patient/lab-reports",
                    lab.getLabVendor() == null ? "Choose Lab" : "View Lab",
                    done, autoDone));
        }

        if (prescription.isPresent()) {
            if (orders.isEmpty()) {
                boolean done = Boolean.TRUE.equals(manual.get("pharmacy-order"));
                items.add(new ChecklistItemDto(
                        "pharmacy-order",
                        "Order prescribed medicines",
                        "Place pharmacy order from your digital prescription",
                        "/patient/prescriptions",
                        "Order Now",
                        done, false));
            } else {
                for (PharmacyOrder order : orders) {
                    boolean autoDone = order.getStatus() == PharmacyOrderStatus.COMPLETED
                            || order.getStatus() == PharmacyOrderStatus.DELIVERED;
                    boolean done = autoDone || Boolean.TRUE.equals(manual.get("pharmacy-" + order.getId()));
                    items.add(new ChecklistItemDto(
                            "pharmacy-" + order.getId(),
                            "Pharmacy order",
                            order.getOrderSummary() != null ? order.getOrderSummary() : "Track medicine delivery",
                            "/patient/pharmacy-orders",
                            "Track Order",
                            done, autoDone));
                }
            }
        }

        for (Invoice invoice : invoices) {
            boolean autoDone = invoice.getPaymentStatus() == PaymentStatus.PAID;
            boolean done = autoDone || Boolean.TRUE.equals(manual.get("bill-" + invoice.getId()));
            items.add(new ChecklistItemDto(
                    "bill-" + invoice.getId(),
                    "Pay bill " + invoice.getInvoiceNumber(),
                    invoice.getDescription() + " — ₹" + invoice.getAmount(),
                    "/patient/bills",
                    "Pay ₹" + invoice.getAmount(),
                    done, autoDone));
        }

        consultation.map(Consultation::getFollowUpDate).ifPresent(followUp -> {
            boolean done = Boolean.TRUE.equals(manual.get("followup"))
                    || followUp.isBefore(LocalDate.now());
            items.add(new ChecklistItemDto(
                    "followup",
                    "Schedule follow-up visit",
                    "Follow-up recommended on " + followUp.format(DATE_FMT),
                    "/patient/book-appointment?doctorId=" + appointment.getDoctor().getId(),
                    "Book Follow-Up",
                    done, followUp.isBefore(LocalDate.now())));
        });

        if (visitCompleted && !feedbackService.hasFeedback(appointment.getId())) {
            boolean done = Boolean.TRUE.equals(manual.get("feedback"));
            items.add(new ChecklistItemDto(
                    "feedback",
                    "Rate your consultation",
                    "Share feedback about Dr. " + appointment.getDoctor().getFullName(),
                    "/patient/appointments",
                    "Leave Rating",
                    done, false));
        }

        return items;
    }

    private List<LabRequest> findVisitLabRequests(Appointment appointment, User patient) {
        return labWorkflowService.getPatientLabRequests(patient).stream()
                .filter(lab -> lab.getDoctor().getId().equals(appointment.getDoctor().getId()))
                .filter(lab -> !lab.getCreatedAt().isBefore(appointment.getCreatedAt()))
                .filter(lab -> lab.getCreatedAt().toLocalDate()
                        .isBefore(appointment.getAppointmentDate().plusDays(14)))
                .collect(Collectors.toList());
    }

    private List<PharmacyOrder> findVisitPharmacyOrders(Optional<Prescription> prescription, User patient) {
        if (prescription.isEmpty()) return List.of();
        Long rxId = prescription.get().getId();
        return pharmacyWorkflowService.getPatientOrders(patient).stream()
                .filter(o -> o.getPrescription() != null && o.getPrescription().getId().equals(rxId))
                .collect(Collectors.toList());
    }

    private List<Invoice> findVisitInvoices(Appointment appointment, User patient) {
        return billingService.getPatientInvoices(patient).stream()
                .filter(i -> i.getReferenceId() != null && i.getReferenceId().equals(appointment.getId()))
                .collect(Collectors.toList());
    }

    private int calculateAverageConsultMinutes(User doctor) {
        List<Consultation> completed = consultationRepository.findByDoctorOrderByStartedAtDesc(doctor).stream()
                .filter(c -> c.getCompletedAt() != null && c.getStartedAt() != null)
                .limit(20)
                .toList();
        if (completed.isEmpty()) return DEFAULT_CONSULT_MINUTES;

        long total = 0;
        for (Consultation c : completed) {
            total += Duration.between(c.getStartedAt(), c.getCompletedAt()).toMinutes();
        }
        return Math.max(5, (int) (total / completed.size()));
    }

    private boolean isActiveEpisode(Appointment appointment) {
        AppointmentState state = appointment.getState();
        return state != AppointmentState.COMPLETED
                && state != AppointmentState.REJECTED
                && state != AppointmentState.CANCELLED
                && state != AppointmentState.EXPIRED;
    }

    private boolean isAtLeast(AppointmentState current, AppointmentState target) {
        if (current == null) return false;
        return current.ordinal() >= target.ordinal()
                && current != AppointmentState.REJECTED
                && current != AppointmentState.CANCELLED;
    }

    private void computeProgress(CareEpisodeDto episode) {
        long countable = episode.getSteps().stream()
                .filter(s -> s.getStatus() != EpisodeStepStatus.SKIPPED).count();
        long done = episode.getSteps().stream()
                .filter(s -> s.getStatus() == EpisodeStepStatus.COMPLETED).count();
        episode.setProgressPercent(countable == 0 ? 0 : (int) Math.round((done * 100.0) / countable));

        episode.setCurrentStepLabel(episode.getSteps().stream()
                .filter(s -> s.getStatus() == EpisodeStepStatus.ACTIVE)
                .map(EpisodeStepDto::getLabel)
                .findFirst()
                .orElse(episode.getSteps().stream()
                        .filter(s -> s.getStatus() == EpisodeStepStatus.COMPLETED)
                        .reduce((a, b) -> b)
                        .map(EpisodeStepDto::getLabel)
                        .orElse("Starting")));
    }

    private EpisodeStepDto step(String id, String label, String desc, String icon,
                                EpisodeStepStatus status, String detail,
                                String actionUrl, String actionLabel) {
        return new EpisodeStepDto(id, label, desc, icon, status, detail, actionUrl, actionLabel);
    }

    private String summarizeLabs(List<LabRequest> labs) {
        if (labs.size() == 1) {
            return labs.get(0).getTestName() + " — " + labs.get(0).getStatus();
        }
        return labs.size() + " tests — latest: " + labs.get(0).getStatus();
    }

    private String billSummary(List<Invoice> invoices) {
        double total = invoices.stream().mapToDouble(Invoice::getAmount).sum();
        long unpaid = invoices.stream().filter(i -> i.getPaymentStatus() != PaymentStatus.PAID).count();
        return "₹" + String.format("%.0f", total) + (unpaid > 0 ? " (" + unpaid + " unpaid)" : " (paid)");
    }

    private String nullSafe(String value) {
        return value != null ? value : "-";
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
