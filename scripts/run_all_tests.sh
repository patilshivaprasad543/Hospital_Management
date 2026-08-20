#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "╔══════════════════════════════════════════════════════════╗"
echo "║     SmartCare 360 — Complete Project Test Runner        ║"
echo "╚══════════════════════════════════════════════════════════╝"

echo ""
echo "━━━ A. Backend unit / Spring tests ━━━"
./gradlew test --offline 2>/dev/null || ./gradlew test

echo ""
echo "━━━ B. Live HTTP workflow tests ($BASE_URL) ━━━"
code="$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/" || true)"
if [[ "$code" != "200" ]]; then
  echo "Server is not running on $BASE_URL (HTTP ${code:-none})."
  echo "Start it with ./gradlew bootRun, then re-run this script."
  exit 1
fi

bash scripts/test_admin_workflow.sh
bash scripts/test_pharmacy_workflow.sh
bash scripts/test_notifications.sh
bash scripts/test_full_system.sh

echo ""
echo "ALL PROJECT TESTS PASSED"
