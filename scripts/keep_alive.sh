#!/usr/bin/env bash
# Ping SmartCare 360 so a Render Free instance does not sleep.
set -u

timeout_secs="${KEEPALIVE_TIMEOUT:-90}"
ok=0
tried=0

add_url() {
  local raw="${1:-}"
  raw="$(echo "$raw" | tr -d '\r' | xargs)"
  if [ -z "$raw" ] || [[ "$raw" == \#* ]]; then
    return
  fi
  raw="${raw%/}"
  echo "$raw"
}

urls=""
if [ -n "${SECRET_URL:-}" ]; then
  urls="$(add_url "$SECRET_URL")"$'\n'"$urls"
fi
if [ -n "${HOMEPAGE:-}" ]; then
  urls="$(add_url "$HOMEPAGE")"$'\n'"$urls"
fi
if [ -f .github/keep-alive-urls.txt ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    u="$(add_url "$line")"
    if [ -n "$u" ]; then
      urls="$u"$'\n'"$urls"
    fi
  done < .github/keep-alive-urls.txt
fi

unique_urls="$(echo "$urls" | awk 'NF && !seen[$0]++')"

if [ -z "$unique_urls" ]; then
  echo "No keep-alive URL configured."
  echo "Set GitHub secret SMARTCARE_APP_URL or put the Render URL in .github/keep-alive-urls.txt"
  exit 0
fi

ping_one() {
  local base="$1"
  local path="$2"
  local url="${base}${path}"
  echo "Pinging $url"
  if curl -fsS --retry 2 --retry-delay 8 --max-time "$timeout_secs" "$url" >/tmp/keepalive-body 2>/tmp/keepalive-err; then
    echo "OK $url"
    return 0
  fi
  echo "MISS $url ($(tr '\n' ' ' < /tmp/keepalive-err))"
  return 1
}

while IFS= read -r base; do
  [ -z "$base" ] && continue
  tried=$((tried + 1))
  if ping_one "$base" "/health" || ping_one "$base" "/"; then
    ok=$((ok + 1))
  fi
done <<< "$unique_urls"

echo "Keep-alive finished: $ok reachable of $tried URL(s)."
if [ "$ok" -eq 0 ]; then
  echo "Nothing responded. Confirm the Render URL in .github/keep-alive-urls.txt (or secret SMARTCARE_APP_URL)."
  echo "The next scheduled run will try again."
  exit 0
fi
