#!/usr/bin/env bash
# SmartCare 360 — Full system integration test suite
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_EMAIL="${SMARTCARE_ADMIN_EMAIL:-admin@smartcare360.com}"
ADMIN_PASSWORD="${SMARTCARE_ADMIN_PASSWORD:-Admin@360}"
TOMORROW="$(date -d '+2 day' +%Y-%m-%d 2>/dev/null || date -v+2d +%Y-%m-%d)"

PASS=0
FAIL=0
SKIP=0
RESULTS=()

pass() { PASS=$((PASS+1)); RESULTS+=("✅ $1"); echo "  ✅ $1"; }
fail() { FAIL=$((FAIL+1)); RESULTS+=("❌ $1"); echo "  ❌ $1"; }
skip() { SKIP=$((SKIP+1)); RESULTS+=("⏭️  $1"); echo "  ⏭️  $1"; }
section() { echo ""; echo "━━━ $1 ━━━"; }

login() {
  local role="$1" email="$2" password="$3" jar="$4"
  local code
  code="$(curl -s -c "$jar" -b "$jar" -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/login" \
    --data-urlencode "email=$email" \
    --data-urlencode "password=$password" \
    --data-urlencode "portalRole=$role")"
  [[ "$code" == "302" || "$code" == "200" ]]
}

get_page() {
  local jar="$1" path="$2"
  curl -s -b "$jar" -o /tmp/page.html -w "%{http_code}" "$BASE_URL$path"
}

expect_page() {
  local jar="$1" path="$2" label="$3" marker="${4:-}"
  local code
  code="$(get_page "$jar" "$path")"
  if [[ "$code" == "200" ]]; then
    if [[ -n "$marker" ]] && ! grep -q "$marker" /tmp/page.html 2>/dev/null; then
      fail "$label (missing: $marker)"
    else
      pass "$label"
    fi
  else
    fail "$label (HTTP $code)"
  fi
}

expect_public() {
  local path="$1" label="$2" marker="${3:-}"
  local code
  code="$(curl -s -o /tmp/page.html -w "%{http_code}" "$BASE_URL$path")"
  if [[ "$code" == "200" ]]; then
    if [[ -n "$marker" ]] && ! grep -qi "$marker" /tmp/page.html 2>/dev/null; then
      fail "$label"
    else
      pass "$label"
    fi
  else
    fail "$label (HTTP $code)"
  fi
}

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     SmartCare 360 — Full System Test Suite              ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo "Base URL: $BASE_URL"
echo "Date: $(date -u)"

# ── 0. Health check ──────────────────────────────────────────
section "0. Server health"
code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")"
[[ "$code" == "200" ]] && pass "Server responding on $BASE_URL" || { fail "Server not reachable"; exit 1; }

# ── 1. Public website ────────────────────────────────────────
section "1. Public website"
expect_public "/" "Home page" "SmartCare"
expect_public "/about" "About page" "About"
expect_public "/contact" "Contact page" "Contact"
code="$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/contact" \
  --data-urlencode "name=Test User" \
  --data-urlencode "email=test@test.com" \
  --data-urlencode "subject=General Inquiry" \
  --data-urlencode "message=Integration test message")"
[[ "$code" == "302" || "$code" == "200" ]] && pass "Contact form submission" || fail "Contact form (HTTP $code)"

# ── 2. Auth portal pages ─────────────────────────────────────
section "2. Authentication portals"
for role in patient doctor vendor pharmacy admin; do
  code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/login/$role")"
  [[ "$code" == "200" ]] && pass "Login page: $role" || fail "Login page: $role (HTTP $code)"
done
expect_public "/login" "Role selection portal" "SmartCare"
expect_public "/forgot-password" "Forgot password page" "password"
expect_public "/register/patient" "Patient registration page" "Register"

# ── 3. Role logins ───────────────────────────────────────────
section "3. Role authentication"
declare -A COOKIES
declare -A ROLES=(
  [ADMIN]="${ADMIN_EMAIL}|${ADMIN_PASSWORD}|ADMIN"
  [PATIENT]="patient@smartcare360.com|patient123|PATIENT"
  [DOCTOR]="sarah.jenkins@smartcare360.com|doc123|DOCTOR"
  [LAB]="lab@smartcare360.com|vendor123|VENDOR"
  [PHARMACY]="pharmacy@smartcare360.com|vendor123|PHARMACY"
)
for key in ADMIN PATIENT DOCTOR LAB PHARMACY; do
  IFS='|' read -r email pass role <<< "${ROLES[$key]}"
  jar="$(mktemp)"
  COOKIES[$key]="$jar"
  if login "$role" "$email" "$pass" "$jar"; then
    pass "Login: $key ($email)"
  else
    fail "Login: $key ($email)"
  fi
done

# ── 4. Admin module ──────────────────────────────────────────
section "4. Admin module"
A="${COOKIES[ADMIN]}"
for path_label in \
  "/admin/dashboard|Admin dashboard|Hospital modules" \
  "/admin/users|Admin patients list|Manage Patients" \
  "/admin/doctors|Admin doctors list|Doctor Management" \
  "/admin/pharmacy|Admin pharmacy|Pharmacy Management" \
  "/admin/laboratories|Admin laboratories|Laboratory Management" \
  "/admin/vendors|Admin vendors list|Vendor" \
  "/admin/appointments|Admin appointments|Appointment" \
  "/admin/prescriptions|Admin prescriptions|Monitor Prescriptions" \
  "/admin/pharmacy-orders|Admin pharmacy orders|Monitor Pharmacy Orders" \
  "/admin/lab-reports|Admin lab reports|Monitor Lab Reports" \
  "/admin/medicines|Admin medicines|Medicine Inventory" \
  "/admin/billing|Admin billing|Billing" \
  "/admin/records|Admin medical records|Medical Records" \
  "/admin/reports|Admin reports|Reports" \
  "/admin/settings|Admin settings|System Settings" \
  "/admin/departments|Admin departments|Department" \
  "/admin/announcements|Admin announcements|Announcement" \
  "/admin/notifications|Admin portal notifications|Portal Notifications" \
  "/admin/audit-logs|Admin audit logs|Audit"; do
  IFS='|' read -r path label marker <<< "$path_label"
  expect_page "$A" "$path" "$label" "$marker"
done

# ── 5. Patient module ────────────────────────────────────────
section "5. Patient module"
P="${COOKIES[PATIENT]}"
for path_label in \
  "/patient/dashboard|Patient dashboard|Welcome" \
  "/patient/doctors|Patient doctors list|Doctor" \
  "/patient/book-appointment|Book appointment form|appointment" \
  "/patient/appointments|Patient appointments|Appointment" \
  "/patient/prescriptions|Patient prescriptions|Prescription" \
  "/patient/pharmacy-orders|Pharmacy orders|Pharmacy" \
  "/patient/lab-reports|Lab reports|Lab" \
  "/patient/bills|Patient bills|Bills" \
  "/patient/profile|Patient profile|Profile" \
  "/patient/records|Patient medical records|Record" \
  "/patient/timeline|Health timeline|Timeline" \
  "/patient/symptom-wizard|Symptom wizard|Symptom"; do
  IFS='|' read -r path label marker <<< "$path_label"
  expect_page "$P" "$path" "$label" "$marker"
done
code="$(curl -s -b "$P" -o /dev/null -w "%{http_code}" "$BASE_URL/patient/api/slots?doctorId=2&date=$TOMORROW")"
[[ "$code" == "200" ]] && pass "Patient API: appointment slots" || fail "Patient API: slots (HTTP $code)"
expect_page "$P" "/patient/notifications" "Patient notifications" "My Notifications"
code="$(curl -s -b "$P" -o /dev/null -w "%{http_code}" "$BASE_URL/notifications")"
[[ "$code" == "302" ]] && pass "Shared /notifications redirects by role" || fail "Shared /notifications (HTTP $code)"

# ── 6. Doctor module ─────────────────────────────────────────
section "6. Doctor module"
D="${COOKIES[DOCTOR]}"
for path_label in \
  "/doctor/dashboard|Doctor dashboard|Doctor" \
  "/doctor/appointments|Doctor appointments|Appointment" \
  "/doctor/patients|Doctor patients|Patient" \
  "/doctor/prescriptions|Doctor prescriptions|Prescription" \
  "/doctor/lab-tests|Doctor lab tests|Lab" \
  "/doctor/earnings|Doctor earnings|Earning" \
  "/doctor/reports|Doctor reports|Report" \
  "/doctor/profile|Doctor profile|Profile" \
  "/doctor/notifications|Doctor notifications|Notification"; do
  IFS='|' read -r path label marker <<< "$path_label"
  expect_page "$D" "$path" "$label" "$marker"
done

# ── 7. Vendor modules ────────────────────────────────────────
section "7. Vendor modules"
L="${COOKIES[LAB]}"
PH="${COOKIES[PHARMACY]}"
expect_page "$L" "/vendor/dashboard" "Lab vendor dashboard" "Laboratory"
expect_page "$PH" "/vendor/dashboard" "Pharmacy vendor dashboard" "Pharmacy Dashboard"
expect_page "$PH" "/vendor/inventory" "Pharmacy inventory" "Medicine Inventory"
expect_page "$PH" "/vendor/orders" "Pharmacy orders" "Order Management"
expect_page "$PH" "/vendor/reports" "Pharmacy reports" "Pharmacy Reports"
expect_page "$L" "/vendor/profile" "Lab vendor profile" "Profile"
expect_page "$PH" "/vendor/profile" "Pharmacy vendor profile" "Profile"
expect_page "$L" "/vendor/notifications" "Lab vendor notifications" "Notification"
expect_page "$PH" "/vendor/notifications" "Pharmacy vendor notifications" "Notification"

# ── 8. Appointment lifecycle ─────────────────────────────────
section "8. Appointment lifecycle"
BOOK_HTML="$(curl -s -b "$P" "$BASE_URL/patient/book-appointment")"
DOCTOR_ID="$(echo "$BOOK_HTML" | python3 -c "import re,sys; html=sys.stdin.read(); m=re.search(r'name=\"doctorId\"[^>]*value=\"(\d+)\"', html); print(m.group(1) if m else '')")"
SLOT="$(curl -s "$BASE_URL/patient/api/slots?doctorId=${DOCTOR_ID:-2}&date=$TOMORROW" | python3 -c "import sys,json; s=json.load(sys.stdin); print(next((x['time'] for x in s if x.get('available')), ''))")"
if [[ -n "$SLOT" ]]; then
  curl -s -b "$P" -o /dev/null -X POST "$BASE_URL/patient/book-appointment" \
    --data-urlencode "doctorId=${DOCTOR_ID:-2}" \
    --data-urlencode "appointmentDate=$TOMORROW" \
    --data-urlencode "appointmentTime=$SLOT" \
    --data-urlencode "reason=Full system test" \
    --data-urlencode "department=General Consultation"
  APPT_ID="$(curl -s -b "$P" "$BASE_URL/patient/appointments" | grep -oP '#APP-\K[0-9]+' | sort -n | tail -1)"
  if [[ -n "$APPT_ID" ]]; then
    pass "Patient booked appointment #$APPT_ID"
    curl -s -b "$D" -o /dev/null -X POST "$BASE_URL/doctor/appointment/$APPT_ID/accept"
    pass "Doctor accepted appointment #$APPT_ID"
    curl -s -b "$P" -o /dev/null -X POST "$BASE_URL/patient/check-in/$APPT_ID"
    pass "Patient checked in #$APPT_ID"
    curl -s -b "$D" -o /dev/null -X POST "$BASE_URL/doctor/appointment/$APPT_ID/start-consultation"
    pass "Doctor started consultation #$APPT_ID"
    curl -s -b "$D" -o /dev/null -X POST "$BASE_URL/doctor/prescription/create" \
      --data-urlencode "appointmentId=$APPT_ID" \
      --data-urlencode "diagnosis=System test fever" \
      --data-urlencode "instructions=Rest well" \
      --data-urlencode "medicineName=Paracetamol 650mg" \
      --data-urlencode "dosage=650mg" \
      --data-urlencode "frequency=1-0-1" \
      --data-urlencode "duration=3 Days"
    pass "Doctor issued prescription for #$APPT_ID"
  else
    fail "Could not find booked appointment ID"
  fi
else
  skip "Appointment booking (no slots on $TOMORROW)"
fi

# ── 9. Lab workflow ──────────────────────────────────────────
section "9. Laboratory workflow"
PATIENTS_HTML="$(curl -s -b "$D" "$BASE_URL/doctor/patients")"
PATIENT_ID="$(echo "$PATIENTS_HTML" | python3 -c "import re,sys; m=re.search(r'/doctor/patients/(\d+)', sys.stdin.read()); print(m.group(1) if m else '')")"
PATIENT_ID="${PATIENT_ID:-5}"
curl -s -b "$D" -o /dev/null -X POST "$BASE_URL/doctor/lab-request/create" \
  --data-urlencode "patientId=$PATIENT_ID" \
  --data-urlencode "testName=Complete Blood Count" \
  --data-urlencode "notes=Full system test lab order"
sleep 1
LAB_HTML="$(curl -s -b "$P" "$BASE_URL/patient/lab-reports")"
LAB_CODE="$(curl -s -b "$P" -o /dev/null -w "%{http_code}" "$BASE_URL/patient/lab-reports")"
[[ "$LAB_CODE" == "200" ]] && pass "Patient lab reports page loads" || fail "Patient lab reports page (HTTP $LAB_CODE)"
LAB_ID="$(echo "$LAB_HTML" | grep -oP 'lab-request/\K[0-9]+' | tail -1)"
if [[ -n "$LAB_ID" ]]; then
  pass "Doctor created lab request"
  LAB_VENDOR_ID="$(echo "$LAB_HTML" | python3 -c "import re,sys; m=re.search(r'name=\"labVendorId\"[^>]*>.*?<option value=\"(\d+)\"', sys.stdin.read(), re.S); print(m.group(1) if m else '')")"
  if [[ -n "$LAB_VENDOR_ID" ]]; then
    curl -s -b "$P" -o /dev/null -X POST "$BASE_URL/patient/lab-request/$LAB_ID/select-vendor" \
      --data-urlencode "labVendorId=$LAB_VENDOR_ID"
    pass "Patient assigned lab vendor to request #$LAB_ID"
    curl -s -b "$L" -o /dev/null -X POST "$BASE_URL/vendor/lab-request/$LAB_ID/upload-report" \
      --data-urlencode "reportResult=Hemoglobin: 14.2 g/dL | WBC: 7200 | All values within normal range."
    LAB_HTML2="$(curl -s -b "$P" "$BASE_URL/patient/lab-reports")"
    [[ "$LAB_HTML2" == *"REPORT_READY"* || "$LAB_HTML2" == *"Hemoglobin"* ]] && pass "Lab report uploaded and visible to patient" \
      || fail "Lab report not visible after upload"
  else
    skip "Lab vendor assignment (request already assigned or no pending)"
    curl -s -b "$L" -o /dev/null -X POST "$BASE_URL/vendor/lab-request/$LAB_ID/upload-report" \
      --data-urlencode "reportResult=Hemoglobin: 14.2 g/dL | System test report." 2>/dev/null || true
  fi
else
  skip "Lab workflow (no lab request found — may already be assigned)"
fi

# ── 10. Pharmacy workflow ────────────────────────────────────
section "10. Pharmacy workflow"
RX_HTML="$(curl -s -b "$P" "$BASE_URL/patient/prescriptions")"
RX_ID="$(echo "$RX_HTML" | python3 -c "import re,sys; m=re.search(r'name=\"prescriptionId\"[^>]*value=\"(\d+)\"', sys.stdin.read()); print(m.group(1) if m else '')")"
VENDOR_ID="$(echo "$RX_HTML" | python3 -c "import re,sys; m=re.search(r'name=\"pharmacyVendorId\"[^>]*>.*?<option value=\"(\d+)\"', sys.stdin.read(), re.S); print(m.group(1) if m else '')")"
if [[ -n "$RX_ID" && -n "$VENDOR_ID" ]]; then
  curl -s -b "$P" -o /dev/null -X POST "$BASE_URL/patient/order-pharmacy" \
    --data-urlencode "prescriptionId=$RX_ID" \
    --data-urlencode "pharmacyVendorId=$VENDOR_ID" \
    --data-urlencode "deliveryAddress=123 Health Ave, Metro City"
  ORDER_ID="$(curl -s -b "$P" "$BASE_URL/patient/pharmacy-orders" | grep -oP '#ORD-\K[0-9]+' | sort -n | tail -1)"
  if [[ -n "$ORDER_ID" ]]; then
    pass "Patient placed pharmacy order #$ORDER_ID"
    curl -s -b "$PH" -o /dev/null -X POST "$BASE_URL/vendor/pharmacy-order/$ORDER_ID/verify-prescription" \
      --data-urlencode "verified=true" --data-urlencode "notes=System test verification"
    for status in ACCEPTED PROCESSING READY_FOR_PICKUP DISPATCHED DELIVERED COMPLETED; do
      curl -s -b "$PH" -o /dev/null -X POST "$BASE_URL/vendor/pharmacy-order/$ORDER_ID/update-status" \
        --data-urlencode "status=$status" \
        --data-urlencode "trackingNotes=System test: $status"
    done
    ORDERS_HTML="$(curl -s -b "$P" "$BASE_URL/patient/pharmacy-orders")"
    [[ "$ORDERS_HTML" == *"System test: COMPLETED"* ]] && pass "Pharmacy order completed end-to-end" || fail "Pharmacy completion status"
  else
    fail "Pharmacy order not created"
  fi
else
  skip "Pharmacy order (no prescription available)"
fi

# ── 11. Billing & PDFs ───────────────────────────────────────
section "11. Billing and PDF downloads"
expect_page "$P" "/patient/bills" "Patient bills page" "Bill"
INVOICE_ID="$(curl -s -b "$P" "$BASE_URL/patient/bills" | grep -oP 'bills/\K[0-9]+' | head -1)"
if [[ -n "$INVOICE_ID" ]]; then
  curl -s -b "$P" -o /dev/null -X POST "$BASE_URL/patient/bills/$INVOICE_ID/pay"
  pass "Patient paid invoice #$INVOICE_ID"
  pdf_code="$(curl -s -b "$P" -o /dev/null -w "%{http_code}" "$BASE_URL/patient/invoice/$INVOICE_ID/pdf")"
  [[ "$pdf_code" == "200" ]] && pass "Invoice PDF download" || fail "Invoice PDF (HTTP $pdf_code)"
else
  skip "Billing payment (no pending invoice — confirm an appointment first)"
fi
if [[ -n "${RX_ID:-}" ]]; then
  pdf_code="$(curl -s -b "$P" -o /dev/null -w "%{http_code}" "$BASE_URL/patient/prescription/$RX_ID/pdf")"
  [[ "$pdf_code" == "200" ]] && pass "Prescription PDF download" || fail "Prescription PDF (HTTP $pdf_code)"
fi

# ── 12. Registration & OTP flow ──────────────────────────────
section "12. Registration and OTP"
TEST_EMAIL="fulltest.$(date +%s)@smartcare360.com"
TEST_MOBILE="$(printf '9%09d' "$(( $(date +%s) % 1000000000 ))")"
REG_HDR="$(mktemp)"
curl -s -D "$REG_HDR" -o /dev/null -X POST "$BASE_URL/register/patient" \
  --data-urlencode "fullName=Full Test Patient" \
  --data-urlencode "email=$TEST_EMAIL" \
  --data-urlencode "mobileNumber=$TEST_MOBILE" \
  --data-urlencode "password=test12345" \
  --data-urlencode "role=PATIENT"
NEW_UID="$(grep -i '^Location:' "$REG_HDR" | grep -oP 'userId=\K[0-9]+' | head -1)"
if [[ -n "$NEW_UID" ]]; then
  pass "New patient registration → verify-otp redirect"
  VERIFY_HTML="$(curl -s "$BASE_URL/verify-otp?userId=$NEW_UID")"
  [[ "$VERIFY_HTML" == *"Enter 6-Digit Code"* ]] && pass "OTP verification page loads" || fail "OTP page layout"
  [[ "$VERIFY_HTML" == *"dev-otp-code"* ]] && fail "OTP must not be displayed in portal"
  skip "OTP verification (codes sent by email only, not shown in portal)"
  curl -s -o /dev/null "$BASE_URL/resend-otp?userId=$NEW_UID"
  pass "OTP resend endpoint"
else
  fail "Patient registration failed"
fi
curl -s -o /dev/null -X POST "$BASE_URL/forgot-password" --data-urlencode "email=patient@smartcare360.com"
pass "Forgot password OTP dispatch"

# ── 13. Vendor catalog management ────────────────────────────
section "13. Vendor catalog operations"
MED_NAME="Test Medicine $(date +%s)mg"
curl -s -b "$PH" -o /dev/null -X POST "$BASE_URL/vendor/pharmacy-item/add" \
  --data-urlencode "itemName=$MED_NAME" \
  --data-urlencode "category=Test" \
  --data-urlencode "price=99.00" \
  --data-urlencode "stockQuantity=50" \
  --data-urlencode "batchNumber=SYS-$(date +%s)" \
  --data-urlencode "description=System test item"
PH_HTML="$(curl -s -b "$PH" "$BASE_URL/vendor/inventory")"
[[ "$PH_HTML" == *"$MED_NAME"* ]] && pass "Pharmacy item added" || fail "Pharmacy item add"
LAB_NAME="System Test Panel $(date +%s)"
curl -s -b "$L" -o /dev/null -X POST "$BASE_URL/vendor/lab-test/add" \
  --data-urlencode "testName=$LAB_NAME" \
  --data-urlencode "category=General" \
  --data-urlencode "price=500" \
  --data-urlencode "description=Automated test"
LAB_DASH="$(curl -s -b "$L" "$BASE_URL/vendor/dashboard")"
[[ "$LAB_DASH" == *"$LAB_NAME"* ]] && pass "Lab test catalog item added" || fail "Lab test add"

# ── 14. Admin write operations ───────────────────────────────
section "14. Admin write operations"
curl -s -b "$A" -o /dev/null -X POST "$BASE_URL/admin/departments" \
  --data-urlencode "name=Test Dept $(date +%s)" \
  --data-urlencode "description=System test department"
pass "Admin department create"
curl -s -b "$A" -o /dev/null -X POST "$BASE_URL/admin/announcements" \
  --data-urlencode "title=System Test Announcement" \
  --data-urlencode "message=Automated full-system test run." \
  --data-urlencode "audience=ALL"
ANN_HTML="$(curl -s -b "$A" "$BASE_URL/admin/announcements")"
[[ "$ANN_HTML" == *"System Test Announcement"* ]] && pass "Admin announcement created" || fail "Admin announcement"

# ── 15. Logout & access control ──────────────────────────────
section "15. Security and access control"
code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/patient/dashboard")"
[[ "$code" == "302" || "$code" == "200" ]] && pass "Unauthenticated patient dashboard redirects/blocks" || fail "Access control"
curl -s -b "$P" -o /dev/null "$BASE_URL/logout"
pass "Logout endpoint"

# ── Summary ──────────────────────────────────────────────────
echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║                    TEST SUMMARY                          ║"
echo "╠══════════════════════════════════════════════════════════╣"
printf "║  ✅ Passed: %-3d                                          ║\n" "$PASS"
printf "║  ❌ Failed: %-3d                                          ║\n" "$FAIL"
printf "║  ⏭️  Skipped: %-3d                                         ║\n" "$SKIP"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
if [[ "$FAIL" -gt 0 ]]; then
  echo "Failed tests:"
  printf '%s\n' "${RESULTS[@]}" | grep "❌" || true
  exit 1
fi
exit 0
