#!/usr/bin/env bash
# Deploy SmartCare 360 to Render via API (requires RENDER_API_KEY)
# Get API key: Render Dashboard → Account Settings → API Keys
set -euo pipefail

if [[ -z "${RENDER_API_KEY:-}" ]]; then
  echo "ERROR: Set RENDER_API_KEY environment variable first."
  echo "Create key at: https://dashboard.render.com/u/settings#api-keys"
  exit 1
fi

REPO_URL="${RENDER_REPO_URL:-https://github.com/patilshivaprasad543/Hospital_Management}"
SERVICE_NAME="${RENDER_SERVICE_NAME:-smartcare360}"

echo "Creating/updating Render Docker web service: $SERVICE_NAME"
echo "Repo: $REPO_URL"

# Check if service exists
EXISTING=$(curl -s -H "Authorization: Bearer $RENDER_API_KEY" \
  "https://api.render.com/v1/services?limit=100" | python3 -c "
import sys, json
name = '$SERVICE_NAME'
try:
    data = json.load(sys.stdin)
    for item in data:
        s = item.get('service', item)
        if s.get('name') == name:
            print(s.get('id', ''))
            break
except: pass
" 2>/dev/null || true)

if [[ -n "$EXISTING" ]]; then
  echo "Service exists ($EXISTING). Triggering deploy..."
  curl -s -X POST -H "Authorization: Bearer $RENDER_API_KEY" \
    "https://api.render.com/v1/services/$EXISTING/deploys" \
    -H "Content-Type: application/json" \
    -d '{"clearCache": "do_not_clear"}' | python3 -m json.tool 2>/dev/null || true
else
  echo "Create service manually once via Blueprint (render.yaml on main), then re-run this script."
  echo "Or use: Render Dashboard → New → Blueprint → connect GitHub repo"
  exit 1
fi
