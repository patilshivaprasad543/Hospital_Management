# HospitalCare Pro — Hospital Management System

A Spring Boot web application for managing hospital workflows: appointments, consultations, prescriptions, lab tests, and pharmacy orders across patients, doctors, vendors, and admins.

## Features

- **Multi-role portals** — Patient, Doctor, Vendor (Lab/Pharmacy), Admin
- **Care workflow tracking** — Visual step-by-step journey from booking to completion
- **Appointment management** — Book, approve, check-in with queue tickets, consult, complete
- **Lab workflow** — Doctor requests → patient selects lab → vendor processes → report ready
- **Pharmacy workflow** — Order medicines from prescriptions with status tracking
- **Smart doctor matching** — Symptom-based doctor recommendations
- **Notifications & email** — OTP verification and appointment confirmations

## Quick Start

### Prerequisites

- Java 17+
- MySQL (production) or use the `dev` profile with H2 (no external DB)

### Run with H2 (recommended for local dev)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Open [http://localhost:803](http://localhost:803)

### Run with MySQL

1. Create database `hospitaldb` and update credentials in `application.properties`
2. Run:

```bash
./gradlew bootRun
```

## Demo Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@hospital.com | admin123 |
| Doctor | sarah.jenkins@hospital.com | doc123 |
| Patient | patient@hospital.com | patient123 |
| Lab Vendor | lab@hospital.com | vendor123 |
| Pharmacy Vendor | pharmacy@hospital.com | vendor123 |

## Care Workflow

The workflow hub tracks each appointment through these steps:

1. **Book** — Patient submits appointment request
2. **Approve** — Doctor confirms or rejects
3. **Check-In** — Patient arrives and gets a queue ticket
4. **Consult** — Doctor conducts consultation
5. **Prescription** — Diagnosis and medicines issued
6. **Lab** — Optional diagnostic tests
7. **Pharmacy** — Optional medicine fulfillment
8. **Complete** — Care episode finished

### Workflow URLs

| Page | URL |
|------|-----|
| Public overview | `/workflow` |
| Patient workflow hub | `/workflow/patient` |
| Doctor workflow queue | `/workflow/doctor` |
| Case detail | `/workflow/case/{appointmentId}` |

## Tech Stack

- Java 17, Spring Boot 3.4
- Thymeleaf, custom CSS
- Spring Data JPA, MySQL / H2
- Spring Mail

## Build & Test

```bash
./gradlew build
./gradlew test
```
