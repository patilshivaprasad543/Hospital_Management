#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
export PORT="${PORT:-8085}"

mkdir -p data uploads

if curl -sf "http://localhost:${PORT}/health" >/dev/null 2>&1; then
  echo "SmartCare 360 already running on port ${PORT}"
  exit 0
fi

nohup ./gradlew bootRun --no-daemon > /tmp/smartcare-bootrun.log 2>&1 &
echo $! > /tmp/smartcare-bootrun.pid

for _ in $(seq 1 90); do
  if curl -sf "http://localhost:${PORT}/health" >/dev/null 2>&1; then
    echo "SmartCare 360 ready at http://localhost:${PORT}"
    exit 0
  fi
  sleep 2
done

echo "SmartCare 360 failed to become ready within 3 minutes" >&2
tail -50 /tmp/smartcare-bootrun.log >&2 || true
exit 1
