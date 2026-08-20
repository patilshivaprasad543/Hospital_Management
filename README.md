# SmartCare 360 — Hospital Management System

Integrated digital hospital platform: patient appointments, doctor consultations, laboratory workflows, pharmacy orders, billing, email/WhatsApp notifications, and role-based portals.

**Live demo:** [https://smartcare360.onrender.com](https://smartcare360.onrender.com)

## Public demo accounts

These accounts are safe to share. **Admin credentials are private** and configured only via server environment variables.

| Portal   | Login URL           | Email                          | Password   |
|----------|---------------------|--------------------------------|------------|
| Patient  | `/login/patient`    | `patient@smartcare360.com`     | `patient123` |
| Doctor   | `/login/doctor`     | `sarah.jenkins@smartcare360.com` | `doc123`   |
| Lab      | `/login/vendor`     | `lab@smartcare360.com`         | `vendor123` |
| Pharmacy | `/login/pharmacy`   | `pharmacy@smartcare360.com`    | `vendor123` |

Register new patients, doctors, and vendors from the portal login page.

## Features

- Role-based authentication with OTP verification (email + WhatsApp)
- Appointment booking, consultation, prescriptions, lab reports
- Pharmacy order workflow (placed → delivered)
- Billing, invoices, PDF downloads
- Admin dashboard (private access only)
- Persistent database (H2 file or MySQL)

## Quick start (local development)

```bash
./gradlew bootRun
```

Open http://localhost:8080 — uses `dev` profile with local defaults.

## Production deployment

See **[DEPLOYMENT.md](DEPLOYMENT.md)** for publishing to Render or Docker.

To keep the public URL live without “waking up” pauses, upgrade Render from Free to **Starter**, or add a 5-minute uptime ping to `/health` (details in DEPLOYMENT.md §7).

Required secrets (set in hosting dashboard, never in code):

- `SMARTCARE_ADMIN_EMAIL` / `SMARTCARE_ADMIN_PASSWORD` — **private admin login**
- **Email OTP:** On Render free tier use **Brevo API** (`SMARTCARE_MAIL_PROVIDER=brevo`, `SMARTCARE_BREVO_API_KEY`, `SMARTCARE_BREVO_SENDER_EMAIL`). Gmail SMTP works locally only — Render blocks SMTP ports on free plans.
- `SMARTCARE_APP_URL` — your public URL
- Twilio vars (optional) — WhatsApp notifications

Copy `.env.example` as a checklist.

## Tech stack

Java 21 · Spring Boot 3.4 · Gradle · Thymeleaf · JPA · H2/MySQL

## Repository

GitHub: [patilshivaprasad543/Hospital_Management](https://github.com/patilshivaprasad543/Hospital_Management)
