#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@smartcare360.com}"
ADMIN_PASS="${ADMIN_PASS:-Admin@360}"
COOKIE="$(mktemp)"
trap 'rm -f "$COOKIE"' EXIT

pass() { echo "✅ $1"; }
fail() { echo "❌ $1"; exit 1; }

code="$(curl -s -c "$COOKIE" -b "$COOKIE" -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/login" \
  --data-urlencode "email=$ADMIN_EMAIL" \
  --data-urlencode "password=$ADMIN_PASS" \
  --data-urlencode "portalRole=ADMIN")"
[[ "$code" == "302" || "$code" == "200" ]] || fail "Admin login failed (HTTP $code)"
pass "Admin logged in"

expect_page() {
  local path="$1" needle="$2"
  local html
  html="$(curl -s -b "$COOKIE" -w "\nHTTP:%{http_code}" "$BASE_URL$path")"
  [[ "$html" == *$'\n'HTTP:200 ]] || fail "$path did not return 200"
  [[ "$html" == *"$needle"* ]] || fail "$path missing expected content: $needle"
  pass "$path"
}

expect_page "/admin/dashboard" "Hospital modules"
expect_page "/admin/users" "Manage Patients"
expect_page "/admin/doctors" "Doctor Management"
expect_page "/admin/pharmacy" "Pharmacy Management"
expect_page "/admin/laboratories" "Laboratory Management"
expect_page "/admin/appointments" "Hospital-Wide Appointments"
expect_page "/admin/prescriptions" "Monitor Prescriptions"
expect_page "/admin/pharmacy-orders" "Monitor Pharmacy Orders"
expect_page "/admin/lab-reports" "Monitor Lab Reports"
expect_page "/admin/medicines" "Medicine Inventory Monitor"
expect_page "/admin/billing" "Billing"
expect_page "/admin/records" "Medical Records"
expect_page "/admin/notifications" "Portal Notifications"
expect_page "/admin/reports" "Reports"
expect_page "/admin/settings" "System Settings"
expect_page "/admin/departments" "Hospital Departments"
logout_code="$(curl -s -b "$COOKIE" -o /dev/null -w "%{http_code}" "$BASE_URL/logout")"
[[ "$logout_code" == "302" || "$logout_code" == "200" ]] || fail "Logout failed (HTTP $logout_code)"
pass "Secure logout"

echo ""
echo "ADMIN WORKFLOW TEST: ALL PAGES PASSED"
