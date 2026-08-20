#!/usr/bin/env bash
# Load local secrets from .env (gitignored) and start SmartCare 360
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
  echo "Loaded configuration from .env"
else
  echo "No .env file found. Copy .env.example and fill in your secrets."
fi

MAIL_PROVIDER="${SMARTCARE_MAIL_PROVIDER:-auto}"
echo "Mail provider: $MAIL_PROVIDER"
if [[ -n "${SMARTCARE_BREVO_API_KEY:-}" && -n "${SMARTCARE_BREVO_SENDER_EMAIL:-}" ]]; then
  echo "  Brevo API: configured"
fi
if [[ -n "${SMARTCARE_MAIL_USERNAME:-}" && -n "${SMARTCARE_MAIL_PASSWORD:-}" ]]; then
  echo "  Gmail SMTP: configured (registration OTP will be sent to the email entered on the form)"
else
  echo "  WARNING: Gmail SMTP not configured — set SMARTCARE_MAIL_USERNAME and SMARTCARE_MAIL_PASSWORD in .env"
fi

exec ./gradlew bootRun "$@"
