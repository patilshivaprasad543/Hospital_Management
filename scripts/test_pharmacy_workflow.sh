#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TODAY="$(date +%Y-%m-%d)"
TOMORROW="$(date -d '+1 day' +%Y-%m-%d 2>/dev/null || date -v+1d +%Y-%m-%d)"
APPT_DATE="${APPT_DATE:-$TOMORROW}"
APPT_TIME=""

PATIENT_EMAIL="patient@smartcare360.com"
PATIENT_PASS="patient123"
DOCTOR_EMAIL="sarah.jenkins@smartcare360.com"
DOCTOR_PASS="doc123"
PHARMACY_EMAIL="pharmacy@smartcare360.com"
PHARMACY_PASS="vendor123"

P_COOKIE="$(mktemp)"
D_COOKIE="$(mktemp)"
V_COOKIE="$(mktemp)"
trap 'rm -f "$P_COOKIE" "$D_COOKIE" "$V_COOKIE"' EXIT

pass() { echo "✅ $1"; }
fail() { echo "❌ $1"; exit 1; }
step() { echo ""; echo "==> $1"; }

login() {
  local role="$1" email="$2" password="$3" jar="$4"
  local code
  code="$(curl -s -c "$jar" -b "$jar" -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/login" \
    --data-urlencode "email=$email" \
    --data-urlencode "password=$password" \
    --data-urlencode "portalRole=$role")"
  [[ "$code" == "302" || "$code" == "200" ]] || fail "Login failed for $email (HTTP $code)"
}

extract_first() {
  local pattern="$1"
  grep -oP "$pattern" | head -1 || true
}

step "1. Patient login"
login PATIENT "$PATIENT_EMAIL" "$PATIENT_PASS" "$P_COOKIE"
pass "Patient logged in"

step "2. Patient books appointment"
BOOK_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/book-appointment")"
DOCTOR_ID="$(echo "$BOOK_HTML" | python3 -c "import re,sys; html=sys.stdin.read(); m=re.search(r'name=\"doctorId\"[^>]*value=\"(\d+)\"', html) or re.search(r'name=\\\"doctorId\\\"[^>]*>.*?<option value=\\\"(\d+)\\\"', html, re.S); print(m.group(1) if m else '')")"
[[ -n "$DOCTOR_ID" ]] || fail "Could not find doctor ID on book-appointment page"

SLOTS_JSON="$(curl -s "$BASE_URL/patient/api/slots?doctorId=$DOCTOR_ID&date=$APPT_DATE")"
APPT_TIME="$(echo "$SLOTS_JSON" | python3 -c "import sys,json; slots=json.load(sys.stdin); avail=[s['time'] for s in slots if s.get('available')]; print(avail[0] if avail else '')")"
[[ -n "$APPT_TIME" ]] || fail "No available slots for doctor #$DOCTOR_ID on $APPT_DATE"

curl -s -b "$P_COOKIE" -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/patient/book-appointment" \
  --data-urlencode "doctorId=$DOCTOR_ID" \
  --data-urlencode "appointmentDate=$APPT_DATE" \
  --data-urlencode "appointmentTime=$APPT_TIME" \
  --data-urlencode "reason=Pharmacy workflow test consultation" \
  --data-urlencode "department=General Consultation" | grep -qE '302|200' \
  || fail "Appointment booking failed"

APPT_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/appointments")"
APPT_ID="$(echo "$APPT_HTML" | extract_first '#APP-\K[0-9]+')"
[[ -n "$APPT_ID" ]] || fail "Could not find appointment ID"
pass "Appointment #$APPT_ID booked with doctor #$DOCTOR_ID at $APPT_DATE $APPT_TIME"

step "3. Doctor accepts appointment"
login DOCTOR "$DOCTOR_EMAIL" "$DOCTOR_PASS" "$D_COOKIE"
curl -s -b "$D_COOKIE" -o /dev/null \
  -X POST "$BASE_URL/doctor/appointment/$APPT_ID/accept" \
  --data-urlencode "notes=Confirmed for pharmacy workflow test"
pass "Doctor accepted appointment"

step "4. Patient checks in"
curl -s -b "$P_COOKIE" -o /dev/null \
  -X POST "$BASE_URL/patient/check-in/$APPT_ID"
pass "Patient checked in"

step "5. Doctor starts consultation and issues prescription"
curl -s -b "$D_COOKIE" -o /dev/null \
  -X POST "$BASE_URL/doctor/appointment/$APPT_ID/start-consultation"

curl -s -b "$D_COOKIE" -o /dev/null \
  -X POST "$BASE_URL/doctor/prescription/create" \
  --data-urlencode "appointmentId=$APPT_ID" \
  --data-urlencode "diagnosis=Acute Viral Fever" \
  --data-urlencode "instructions=Take medicines after food and rest well." \
  --data-urlencode "medicineName=Paracetamol 650mg" \
  --data-urlencode "dosage=650mg" \
  --data-urlencode "frequency=1-0-1" \
  --data-urlencode "duration=5 Days"
pass "Prescription issued with Paracetamol 650mg"

step "6. Patient places pharmacy order"
RX_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/prescriptions")"
RX_ID="$(echo "$RX_HTML" | python3 -c "import re,sys; html=sys.stdin.read(); m=re.search(r'name=\"prescriptionId\"[^>]*value=\"(\d+)\"', html); print(m.group(1) if m else '')")"
[[ -n "$RX_ID" ]] || fail "No prescription found for patient"

VENDOR_ID="$(echo "$RX_HTML" | python3 -c "import re,sys; html=sys.stdin.read(); m=re.search(r'name=\"pharmacyVendorId\"[^>]*>.*?<option value=\"(\d+)\"', html, re.S); print(m.group(1) if m else '')")"
[[ -n "$VENDOR_ID" ]] || fail "No pharmacy vendor found"

curl -s -b "$P_COOKIE" -L -o /tmp/pharmacy-order-redirect.html \
  -X POST "$BASE_URL/patient/order-pharmacy" \
  --data-urlencode "prescriptionId=$RX_ID" \
  --data-urlencode "pharmacyVendorId=$VENDOR_ID" \
  --data-urlencode "deliveryAddress=123 Health Ave, Metro City"

ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
ORDER_ID="$(echo "$ORDERS_HTML" | extract_first '#ORD-\K[0-9]+')"
[[ -n "$ORDER_ID" ]] || fail "Pharmacy order was not created"
echo "$ORDERS_HTML" | grep -q "Pending" || fail "Initial status not PLACED"
pass "Pharmacy order #$ORDER_ID placed (status: Pending)"

step "7. Pharmacy vendor updates order through delivery"
login PHARMACY "$PHARMACY_EMAIL" "$PHARMACY_PASS" "$V_COOKIE"

DASH="$(curl -s -b "$V_COOKIE" "$BASE_URL/vendor/dashboard")"
echo "$DASH" | grep -q "Pharmacy Dashboard" || fail "Pharmacy dashboard not reachable"
INV="$(curl -s -b "$V_COOKIE" "$BASE_URL/vendor/inventory")"
echo "$INV" | grep -q "Medicine Inventory" || fail "Inventory page missing"
ORD="$(curl -s -b "$V_COOKIE" "$BASE_URL/vendor/orders")"
echo "$ORD" | grep -q "#ORD-$ORDER_ID" || fail "Order not listed for pharmacy"
REP="$(curl -s -b "$V_COOKIE" "$BASE_URL/vendor/reports")"
echo "$REP" | grep -q "Pharmacy Reports" || fail "Reports page missing"
pass "Pharmacy dashboard, inventory, orders, and reports are available"

update_status() {
  local status="$1" notes="${2:-}"
  curl -s -b "$V_COOKIE" -o /dev/null \
    -X POST "$BASE_URL/vendor/pharmacy-order/$ORDER_ID/update-status" \
    --data-urlencode "status=$status" \
    --data-urlencode "trackingNotes=$notes"
}

curl -s -b "$V_COOKIE" -o /dev/null \
  -X POST "$BASE_URL/vendor/pharmacy-order/$ORDER_ID/verify-prescription" \
  --data-urlencode "verified=true" \
  --data-urlencode "notes=Doctor prescription verified against patient identity"
pass "Prescription verified"

update_status ACCEPTED "Order accepted and being prepared"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Accepted" || fail "Status not ACCEPTED"
pass "Status updated to Accepted"

update_status PROCESSING "Medicines being packed"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Preparing" || fail "Status not PROCESSING/Preparing"
pass "Status updated to Preparing"

update_status READY_FOR_PICKUP "Ready at pharmacy counter"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Ready for pickup" || fail "Status not READY_FOR_PICKUP"
pass "Status updated to Ready for pickup"

update_status DISPATCHED "Out for delivery via hospital courier"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Dispatched" || fail "Status not DISPATCHED"
echo "$ORDERS_HTML" | grep -q "Out for delivery via hospital courier" || fail "Tracking notes not visible after dispatch"
pass "Status updated to Dispatched"

update_status DELIVERED "Delivered to patient doorstep"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Delivered" || fail "Status not DELIVERED"
echo "$ORDERS_HTML" | grep -q "Delivered to patient doorstep" || fail "Final tracking notes not visible"
pass "Status updated to Delivered with tracking notes"

update_status COMPLETED "Order completed and inventory updated"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Order completed and inventory updated" || fail "Status not COMPLETED"
pass "Status updated to Completed"

step "8. Verify invalid status transition is blocked"
INVALID_CODE="$(curl -s -b "$V_COOKIE" -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/vendor/pharmacy-order/$ORDER_ID/update-status" \
  --data-urlencode "status=DISPATCHED")"
ORDERS_HTML="$(curl -s -b "$P_COOKIE" "$BASE_URL/patient/pharmacy-orders")"
echo "$ORDERS_HTML" | grep -q "Order completed and inventory updated" || fail "Order status changed after terminal state"
pass "Terminal order cannot be changed (still Completed)"

echo ""
echo "=========================================="
echo " PHARMACY WORKFLOW TEST: ALL STEPS PASSED"
echo "=========================================="
echo "Appointment ID : $APPT_ID"
echo "Prescription ID: $RX_ID"
echo "Pharmacy Order : #ORD-$ORDER_ID"
echo "Final Status   : Completed"
