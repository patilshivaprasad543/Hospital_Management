#!/usr/bin/env bash
# Free keep-alive: ping the Postgres-backed live site only. Never fail the job.
set -u
timeout_secs="${KEEPALIVE_TIMEOUT:-90}"
urls="https://hospital-management-glt1-ge8d.onrender.com"
paths="/health /healthz /ping /keepalive /login /"
while IFS= read -r base; do
  [ -z "$base" ] && continue
  for path in $paths; do
    echo "Pinging ${base}${path}"
    curl -fsS --max-time "$timeout_secs" -A "SmartCare360-KeepAlive" "${base}${path}" >/dev/null && echo "OK ${base}${path}" || true
  done
done <<< "$urls"
exit 0
