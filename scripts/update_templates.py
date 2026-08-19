#!/usr/bin/env python3
"""Batch-update templates for professional SmartCare 360 branding."""
import re
from pathlib import Path

TEMPLATES = Path("/workspace/src/main/resources/templates")

NAV_MAP = {
    "admin/": "admin-nav",
    "patient/": "patient-nav",
    "doctor/": "doctor-nav",
    "vendor/": "vendor-nav",
}

TITLE_MAP = {
    "admin/dashboard.html": "Admin Dashboard",
    "admin/users.html": "Patients",
    "admin/doctors.html": "Doctors",
    "admin/vendors.html": "Vendors",
    "admin/appointments.html": "Appointments",
    "admin/departments.html": "Departments",
    "admin/audit-logs.html": "Audit Logs",
    "patient/appointments.html": "My Appointments",
    "patient/doctors.html": "Find Doctors",
    "patient/book-appointment.html": "Book Appointment",
    "patient/profile.html": "My Profile",
    "patient/bills.html": "Billing",
    "patient/prescriptions.html": "Prescriptions",
    "patient/lab-reports.html": "Lab Reports",
    "patient/timeline.html": "Health Timeline",
    "patient/symptom-wizard.html": "Symptom Checker",
    "doctor/appointments.html": "Appointments",
    "doctor/profile.html": "Profile & Schedule",
    "doctor/consultation.html": "Consultation",
    "doctor/dashboard.html": "Doctor Dashboard",
    "vendor/dashboard.html": "Vendor Dashboard",
    "vendor/lab-dashboard.html": "Laboratory",
    "vendor/pharmacy-dashboard.html": "Pharmacy",
    "vendor/profile.html": "Vendor Profile",
    "auth/register-role.html": "Register",
    "auth/register.html": "Register",
    "auth/verify-otp.html": "Verify Email",
    "auth/submit-documents.html": "Document Verification",
    "auth/forgot-password.html": "Forgot Password",
    "auth/reset-password.html": "Reset Password",
    "notifications/list.html": "Notifications",
    "index.html": "Home",
}

SKIP = {
    "auth/portal.html",
    "auth/login.html",
    "patient/dashboard.html",
    "fragments/layout.html",
}

NAV_PATTERN = re.compile(
    r"<nav class=\"navbar\">.*?</nav>\s*",
    re.DOTALL,
)

HEAD_PATTERN = re.compile(
    r"<head>.*?</head>",
    re.DOTALL,
)


def get_nav_fragment(rel_path: str) -> str:
    for prefix, frag in NAV_MAP.items():
        if rel_path.startswith(prefix):
            return frag
    if rel_path.startswith("auth/"):
        return "auth-nav"
    return "auth-nav"


def process_file(path: Path):
    rel = str(path.relative_to(TEMPLATES)).replace("\\", "/")
    if rel in SKIP:
        return False

    content = path.read_text()
    original = content

    title = TITLE_MAP.get(rel, rel.replace("/", " - ").replace(".html", "").title())

    # Replace head
    if "<head>" in content and "th:replace" not in content:
        content = HEAD_PATTERN.sub(
            f'<head th:replace="~{{fragments/layout :: head(\'{title}\')}}"></head>',
            content,
            count=1,
        )

    # Replace navbar
    if '<nav class="navbar">' in content:
        frag = get_nav_fragment(rel)
        content = NAV_PATTERN.sub(
            f'<nav th:replace="~{{fragments/layout :: {frag}}}"></nav>\n\n',
            content,
            count=1,
        )

    # Add auth-body class for auth pages
    if rel.startswith("auth/") and rel not in ("auth/portal.html", "auth/login.html"):
        content = content.replace("<body>", '<body class="auth-body">', 1)

    # Replace simple footer
    if "<footer>" in content and "th:replace" not in content:
        content = re.sub(
            r"<footer>.*?</footer>",
            '<footer th:replace="~{fragments/layout :: footer}"></footer>',
            content,
            flags=re.DOTALL,
            count=1,
        )

    # Standardize logout button text in remaining inline navs
    content = content.replace(">Logout<", ">Sign Out<")
    content = content.replace("btn btn-secondary btn-sm\">Logout", 'btn btn-outline btn-sm">Sign Out')

    if content != original:
        path.write_text(content)
        print(f"Updated: {rel}")
        return True
    return False


def main():
    count = 0
    for html in sorted(TEMPLATES.rglob("*.html")):
        if process_file(html):
            count += 1
    print(f"Done. Updated {count} files.")


if __name__ == "__main__":
    main()
