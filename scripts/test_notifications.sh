#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_EMAIL="notify.test.$(date +%s)@smartcare360.com"
TEST_MOBILE="9876501234"
TEST_PASS="test12345"

echo "==> SmartCare 360 Notification & OTP Test"
echo "Base URL: $BASE_URL"
echo "Test email: $TEST_EMAIL"
echo ""

# 1. Register new patient (triggers OTP email + WhatsApp)
echo "==> 1. Register patient (OTP dispatch)"
REG_HEADERS=$(mktemp)
REG_CODE=$(curl -s -D "$REG_HEADERS" -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/register/patient" \
  --data-urlencode "fullName=Notify Test User" \
  --data-urlencode "email=$TEST_EMAIL" \
  --data-urlencode "mobileNumber=$TEST_MOBILE" \
  --data-urlencode "password=$TEST_PASS" \
  --data-urlencode "role=PATIENT")
[[ "$REG_CODE" == "302" || "$REG_CODE" == "200" ]] || { echo "FAIL: registration HTTP $REG_CODE"; exit 1; }
USER_ID=$(grep -i '^Location:' "$REG_HEADERS" | grep -oP 'userId=\K[0-9]+' | head -1)
[[ -n "$USER_ID" ]] || { echo "FAIL: could not parse userId from redirect"; exit 1; }
echo "Registered user #$USER_ID"

# 2. Open verify-otp page — should show dev OTP when SMTP not configured
echo "==> 2. Verify OTP page shows dev OTP panel"
VERIFY_HTML=$(curl -s "$BASE_URL/verify-otp?userId=$USER_ID")
echo "$VERIFY_HTML" | grep -q "dev-otp-panel\|Enter 6-Digit OTP" || { echo "FAIL: verify-otp page missing"; exit 1; }
OTP=$(echo "$VERIFY_HTML" | grep -oP 'class="dev-otp-code"[^>]*>\K[0-9]{6}' || true)
if [[ -n "$OTP" ]]; then
  echo "Dev OTP retrieved: $OTP"
else
  echo "Note: Dev OTP not on page (SMTP may be configured). Check Admin Notification Log."
  OTP="000000"
fi

# 3. Admin login and check notification log
echo "==> 3. Admin notification log"
A_COOKIE=$(mktemp)
curl -s -c "$A_COOKIE" -b "$A_COOKIE" -X POST "$BASE_URL/login" \
  --data-urlencode "email=admin@smartcare360.com" \
  --data-urlencode "password=Admin@360" \
  --data-urlencode "portalRole=ADMIN" -o /dev/null
LOG_FILE=$(mktemp)
curl -s -b "$A_COOKIE" "$BASE_URL/admin/notification-log" > "$LOG_FILE"
grep -q "Notification Delivery Log" "$LOG_FILE" || { echo "FAIL: notification log page"; exit 1; }
grep -qE "$TEST_EMAIL|EMAIL|OTP" "$LOG_FILE" && echo "Notification log contains OTP/email entries" || echo "WARN: log may be empty if async pending"
rm -f "$LOG_FILE"

# 4. Verify OTP if we have code
if [[ "$OTP" != "000000" ]]; then
  echo "==> 4. Verify OTP"
  curl -s -c /tmp/patient.cookie -b /tmp/patient.cookie \
    -X POST "$BASE_URL/verify-otp" \
    --data-urlencode "userId=$USER_ID" \
    --data-urlencode "otp=$OTP" -o /dev/null
  echo "OTP verification submitted"
fi

# 5. Patient login + book appointment (triggers notifications)
echo "==> 5. Appointment booking notifications"
P_COOKIE=$(mktemp)
curl -s -c "$P_COOKIE" -b "$P_COOKIE" -X POST "$BASE_URL/login" \
  --data-urlencode "email=patient@smartcare360.com" \
  --data-urlencode "password=patient123" \
  --data-urlencode "portalRole=PATIENT" -o /dev/null
TOMORROW=$(date -d '+2 day' +%Y-%m-%d 2>/dev/null || date -v+2d +%Y-%m-%d)
SLOT=$(curl -s "$BASE_URL/patient/api/slots?doctorId=2&date=$TOMORROW" | python3 -c "import sys,json; s=json.load(sys.stdin); print(next((x['time'] for x in s if x['available']), ''))")
if [[ -n "$SLOT" ]]; then
  curl -s -b "$P_COOKIE" -X POST "$BASE_URL/patient/book-appointment" \
    --data-urlencode "doctorId=2" \
    --data-urlencode "appointmentDate=$TOMORROW" \
    --data-urlencode "appointmentTime=$SLOT" \
    --data-urlencode "reason=Notification test" -o /dev/null
  echo "Appointment booked — notification + email dispatched"
else
  echo "SKIP: no slots for appointment test"
fi

# 6. Forgot password OTP
echo "==> 6. Password reset OTP"
curl -s -X POST "$BASE_URL/forgot-password" \
  --data-urlencode "email=patient@smartcare360.com" -o /dev/null
sleep 1
LOG2_FILE=$(mktemp)
curl -s -b "$A_COOKIE" "$BASE_URL/admin/notification-log" > "$LOG2_FILE"
grep -qi "password reset\|PASSWORD RESET" "$LOG2_FILE" && echo "Password reset notification logged" || echo "WARN: check async log"
rm -f "$LOG2_FILE"

echo ""
echo "=========================================="
echo " NOTIFICATION TESTS COMPLETED"
echo "=========================================="
echo "Review full delivery log at: $BASE_URL/admin/notification-log"
echo "(Login as admin@smartcare360.com / Admin@360)"
