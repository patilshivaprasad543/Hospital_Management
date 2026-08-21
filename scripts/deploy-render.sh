#!/usr/bin/env bash
# Create or redeploy the Docker web service on Render.
# Requires RENDER_API_KEY (Render Dashboard → Account Settings → API Keys).
set -euo pipefail

if [[ -z "${RENDER_API_KEY:-}" ]]; then
  echo "ERROR: RENDER_API_KEY is not set."
  echo "This agent cannot create a 24/7 URL without your Render (or Oracle) account."
  exit 1
fi

REPO_URL="${RENDER_REPO_URL:-https://github.com/patilshivaprasad543/Hospital_Management}"
SERVICE_NAME="${RENDER_SERVICE_NAME:-hospital-management-glt1-ge8d}"
ADMIN_EMAIL="${SMARTCARE_ADMIN_EMAIL:-admin@smartcare360.local}"
ADMIN_PASSWORD="${SMARTCARE_ADMIN_PASSWORD:-}"
if [[ -z "$ADMIN_PASSWORD" ]]; then
  ADMIN_PASSWORD="$(openssl rand -base64 18 | tr -d '/+=' | head -c 20)"
  GENERATED_ADMIN=1
else
  GENERATED_ADMIN=0
fi

python3 - "$REPO_URL" "$SERVICE_NAME" "$ADMIN_EMAIL" "$ADMIN_PASSWORD" "$GENERATED_ADMIN" <<'PY'
import json, os, sys, urllib.request, urllib.error

repo, name, admin_email, admin_password, generated = sys.argv[1:]
key = os.environ["RENDER_API_KEY"]

def req(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    r = urllib.request.Request(
        "https://api.render.com/v1" + path,
        data=data,
        method=method,
        headers={
            "Authorization": "Bearer " + key,
            "Accept": "application/json",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(r, timeout=60) as resp:
            raw = resp.read().decode()
            return resp.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        err = e.read().decode()
        print("Render API", e.code, path, err[:2000], file=sys.stderr)
        raise SystemExit(1)

status, owners = req("GET", "/owners?limit=20")
owner_id = None
items = owners if isinstance(owners, list) else []
for item in items:
    o = item.get("owner", item)
    owner_id = o.get("id")
    if owner_id:
        break
if not owner_id:
    print("No Render workspace found for this API key.", file=sys.stderr)
    raise SystemExit(1)

status, services = req("GET", "/services?limit=100")
existing = None
for item in services or []:
    s = item.get("service", item)
    if s.get("name") == name:
        existing = s
        break

env_vars = [
    {"key": "SPRING_PROFILES_ACTIVE", "value": "prod"},
    {"key": "PORT", "value": "8080"},
    {"key": "SMARTCARE_ADMIN_EMAIL", "value": admin_email},
    {"key": "SMARTCARE_ADMIN_PASSWORD", "value": admin_password},
    {"key": "SMARTCARE_ADMIN_NAME", "value": "System Administrator"},
    {"key": "SMARTCARE_ADMIN_MOBILE", "value": "9999999999"},
    {"key": "SMARTCARE_MAIL_PROVIDER", "value": "auto"},
    {"key": "SMARTCARE_WHATSAPP_ENABLED", "value": "false"},
    {"key": "SMARTCARE_APP_URL", "value": "https://hospital-management-glt1-ge8d.onrender.com"},
    {"key": "SMARTCARE_DB_DRIVER", "value": "org.postgresql.Driver"},
    {"key": "SMARTCARE_DB_DIALECT", "value": "org.hibernate.dialect.PostgreSQLDialect"},
]

if existing:
    sid = existing["id"]
    print("Service exists:", sid)
    req("PUT", f"/services/{sid}/env-vars", env_vars)
    status, deploy = req("POST", f"/services/{sid}/deploys", {"clearCache": "do_not_clear"})
    service = existing
else:
    body = {
        "type": "web_service",
        "name": name,
        "ownerId": owner_id,
        "repo": repo,
        "branch": "main",
        "autoDeploy": "yes",
        "envVars": env_vars,
        "serviceDetails": {
            "runtime": "docker",
            "plan": "free",
            "region": "singapore",
            "healthCheckPath": "/health",
            "disk": {
                "name": "smartcare-data",
                "mountPath": "/app/data",
                "sizeGB": 1,
            },
            "envSpecificDetails": {
                "dockerfilePath": "./Dockerfile",
                "dockerContext": ".",
            },
        },
    }
    status, created = req("POST", "/services", body)
    service = created.get("service", created) if isinstance(created, dict) else created

url = None
details = service.get("serviceDetails") or {}
url = details.get("url") or service.get("url")
slug = service.get("slug") or name
if not url:
    url = f"https://{slug}.onrender.com"

# Point the app at its public URL
env_vars.append({"key": "SMARTCARE_APP_URL", "value": url})
req("PUT", f"/services/{service['id']}/env-vars", env_vars)

print("LIVE_URL=" + url)
print("ADMIN_EMAIL=" + admin_email)
if generated == "1":
    print("ADMIN_PASSWORD=" + admin_password)
    print("Save the admin password. It is only shown in this log.")
print("Dashboard:", service.get("dashboardUrl", ""))
PY
