#!/usr/bin/env python3
"""Capture SmartCare 360 portal screenshots."""
import os
from playwright.sync_api import sync_playwright

BASE = "http://localhost:8080"
OUT = "/opt/cursor/artifacts/screenshots"
os.makedirs(OUT, exist_ok=True)

WEBSITE_PAGES = [
    ("00-home-page", "/"),
    ("00-about-page", "/about"),
    ("00-contact-page", "/contact"),
]

PUBLIC_PAGES = [
    ("01-role-selection-portal", "/login"),
    ("02-admin-login", "/login/admin"),
    ("03-patient-login", "/login/patient"),
    ("04-doctor-login", "/login/doctor"),
    ("05-vendor-login", "/login/vendor"),
    ("06-pharmacy-login", "/login/pharmacy"),
    ("07-patient-register", "/register/patient"),
    ("08-doctor-register", "/register/doctor"),
    ("09-vendor-register", "/register/vendor"),
    ("10-pharmacy-register", "/register/pharmacy"),
]

DASHBOARDS = [
    ("11-admin-dashboard", "/admin/dashboard", "admin@smartcare360.com", "Admin@360", "ADMIN"),
    ("12-patient-dashboard", "/patient/dashboard", "patient@smartcare360.com", "patient123", "PATIENT"),
    ("13-doctor-dashboard", "/doctor/dashboard", "sarah.jenkins@smartcare360.com", "doc123", "DOCTOR"),
    ("14-vendor-dashboard", "/vendor/dashboard", "lab@smartcare360.com", "vendor123", "VENDOR"),
    ("15-pharmacy-dashboard", "/vendor/dashboard", "pharmacy@smartcare360.com", "vendor123", "PHARMACY"),
]


def login(page, email, password, portal_role):
    page.goto(f"{BASE}/login/{portal_role.lower()}")
    page.fill('input[name="email"]', email)
    page.fill('input[name="password"]', password)
    page.click('button[type="submit"]')
    page.wait_for_load_state("networkidle")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1400, "height": 900})
        page = context.new_page()

        for name, path in WEBSITE_PAGES:
            page.goto(f"{BASE}{path}")
            page.wait_for_load_state("networkidle")
            page.screenshot(path=f"{OUT}/{name}.png", full_page=True)
            print(f"Saved {name}.png")

        for name, path in PUBLIC_PAGES:
            page.goto(f"{BASE}{path}")
            page.wait_for_load_state("networkidle")
            page.screenshot(path=f"{OUT}/{name}.png", full_page=True)
            print(f"Saved {name}.png")

        for name, path, email, password, role in DASHBOARDS:
            context.clear_cookies()
            login(page, email, password, role)
            page.goto(f"{BASE}{path}")
            page.wait_for_load_state("networkidle")
            page.screenshot(path=f"{OUT}/{name}.png", full_page=True)
            print(f"Saved {name}.png")

        browser.close()


if __name__ == "__main__":
    main()
