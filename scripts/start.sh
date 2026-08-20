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

exec ./gradlew bootRun "$@"
