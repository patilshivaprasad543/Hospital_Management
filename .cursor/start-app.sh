#!/usr/bin/env bash
set -euo pipefail

cd /workspace

if curl -sf http://localhost:8080/ >/dev/null 2>&1; then
  echo "SmartCare 360 already running on port 8080"
  exit 0
fi

SESSION_NAME="smartcare-app"
TMUX_CONF="/exec-daemon/tmux.portal.conf"

if ! tmux -f "$TMUX_CONF" has-session -t "=$SESSION_NAME" 2>/dev/null; then
  tmux -f "$TMUX_CONF" new-session -d -s "$SESSION_NAME" -c "/workspace" -- "./gradlew bootRun --no-daemon"
fi

for _ in $(seq 1 90); do
  if curl -sf http://localhost:8080/ >/dev/null 2>&1; then
    echo "SmartCare 360 ready on port 8080"
    exit 0
  fi
  sleep 2
done

echo "Timed out waiting for SmartCare 360 on port 8080"
exit 1
