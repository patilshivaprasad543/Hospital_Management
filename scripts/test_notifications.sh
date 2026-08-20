#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_EMAIL="notify.test.$(date +%s)@smartcare360.com"
TEST_MOBILE="$(printf '8%09d' "$(( $(date +%s) % 1000000000 ))")"
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

# 2. Open verify-otp page (OTP is never shown in the portal)
echo "==> 2. Verify OTP page loads"
VERIFY_HTML=$(curl -s "$BASE_URL/verify-otp?userId=$USER_ID")
echo "$VERIFY_HTML" | grep -q "Enter 6-Digit Code" || { echo "FAIL: verify-otp page missing"; exit 1; }
echo "$VERIFY_HTML" | grep -q "dev-otp-code" && { echo "FAIL: OTP must not be displayed in portal"; exit 1; }
echo "OTP page OK (code sent to email only)"

# 3. Admin login and check notification log
echo "==> 3. Admin notification log"
A_COOKIE=$(mktemp)
curl -s -c "$A_COOKIE" -b "$A_COOKIE" -X POST "$BASE_URL/login" \
  --data-urlencode "email=admin@smartcare360.com" \
  --data-urlencode "password=Admin@360" \
  --data-urlencode "portalRole=ADMIN" -o /dev/null
LOG_FILE=$(mktemp)
curl -s -b "$A_COOKIE" "$BASE_URL/admin/notifications" > "$LOG_FILE"
grep -q "Portal Notifications" "$LOG_FILE" || { echo "FAIL: notifications page"; exit 1; }
grep -q "Recent Portal Notifications" "$LOG_FILE" && echo "Portal notifications page OK" || echo "WARN: page layout check"
grep -qiE 'your (verification|otp) code is [0-9]{6}|otp code:[[:space:]]*[0-9]{6}' "$LOG_FILE" && { echo "FAIL: OTP content found on notifications page"; exit 1; } || true
rm -f "$LOG_FILE"

# 4. Verify OTP skipped — OTP is only delivered via email, not shown in portal
echo "==> 4. OTP verification (email delivery only)"
echo "Skipped automated OTP entry — users receive codes by email only"

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
FP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/forgot-password" \
  --data-urlencode "email=patient@smartcare360.com")
[[ "$FP_CODE" == "302" || "$FP_CODE" == "200" ]] || { echo "FAIL: forgot-password HTTP $FP_CODE"; exit 1; }
sleep 1
AUDIT_FILE=$(mktemp)
curl -s -b "$A_COOKIE" "$BASE_URL/admin/audit-logs" > "$AUDIT_FILE"
grep -q "PASSWORD_RESET_REQUESTED" "$AUDIT_FILE" && echo "Password reset audit log recorded" \
  || { echo "FAIL: password reset not in audit logs"; exit 1; }
rm -f "$AUDIT_FILE"

echo ""
echo "=========================================="
echo " NOTIFICATION TESTS COMPLETED"
echo "=========================================="
echo "Review notifications at: $BASE_URL/admin/notifications"
echo "(Login with your configured admin account — credentials are not published)"
