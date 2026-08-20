#!/usr/bin/env bash
# Install SmartCare 360 on a free always-on Linux VM (Oracle Cloud Always Free, etc.).
# Run on the VM as root:  curl -fsSL ... | bash   or   bash scripts/install-always-free.sh
set -euo pipefail

REPO_URL="${REPO_URL:-https://github.com/patilshivaprasad543/Hospital_Management.git}"
INSTALL_DIR="${INSTALL_DIR:-/opt/smartcare360}"
ADMIN_EMAIL="${SMARTCARE_ADMIN_EMAIL:-admin@localhost}"

if [[ "$(id -u)" -ne 0 ]]; then
  echo "Run as root: sudo bash $0"
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y ca-certificates curl git openssl

if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

if ! docker compose version >/dev/null 2>&1; then
  apt-get install -y docker-compose-plugin || true
fi

mkdir -p "$INSTALL_DIR"
if [[ -d "$INSTALL_DIR/.git" ]]; then
  git -C "$INSTALL_DIR" pull --ff-only origin main || git -C "$INSTALL_DIR" pull --ff-only || true
else
  git clone --depth 1 "$REPO_URL" "$INSTALL_DIR"
fi
cd "$INSTALL_DIR"

PUBLIC_IP="$(curl -fsS --max-time 8 https://api.ipify.org || true)"
if [[ -z "$PUBLIC_IP" ]]; then
  PUBLIC_IP="$(hostname -I 2>/dev/null | awk '{print $1}')"
fi
APP_URL="http://${PUBLIC_IP}"

if [[ ! -f .env ]]; then
  ADMIN_PASS="$(openssl rand -base64 18 | tr -d '/+=' | head -c 20)"
  cp .env.example .env
  sed -i "s|^SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod|" .env
  sed -i "s|^SMARTCARE_APP_URL=.*|SMARTCARE_APP_URL=${APP_URL}|" .env
  sed -i "s|^SMARTCARE_ADMIN_EMAIL=.*|SMARTCARE_ADMIN_EMAIL=${ADMIN_EMAIL}|" .env
  sed -i "s|^SMARTCARE_ADMIN_PASSWORD=.*|SMARTCARE_ADMIN_PASSWORD=${ADMIN_PASS}|" .env
  echo
  echo "Created $INSTALL_DIR/.env"
  echo "  Admin email:    ${ADMIN_EMAIL}"
  echo "  Admin password: ${ADMIN_PASS}"
  echo "  Public URL:     ${APP_URL}"
  echo "Save the admin password. It is not printed again."
  echo
else
  grep -q '^SPRING_PROFILES_ACTIVE=' .env && sed -i "s|^SPRING_PROFILES_ACTIVE=.*|SPRING_PROFILES_ACTIVE=prod|" .env
  if grep -q '^SMARTCARE_APP_URL=' .env; then
    sed -i "s|^SMARTCARE_APP_URL=.*|SMARTCARE_APP_URL=${APP_URL}|" .env
  else
    echo "SMARTCARE_APP_URL=${APP_URL}" >> .env
  fi
fi

if command -v ufw >/dev/null 2>&1; then
  ufw allow 22/tcp || true
  ufw allow 80/tcp || true
  ufw allow 8080/tcp || true
fi
iptables -C INPUT -p tcp --dport 80 -j ACCEPT 2>/dev/null || iptables -I INPUT -p tcp --dport 80 -j ACCEPT || true
iptables -C INPUT -p tcp --dport 8080 -j ACCEPT 2>/dev/null || iptables -I INPUT -p tcp --dport 8080 -j ACCEPT || true

docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --build

echo
echo "SmartCare 360 is starting (first build can take several minutes)."
echo "Open:  ${APP_URL}   or   ${APP_URL}:8080"
echo "Health: ${APP_URL}/health"
echo
echo "Oracle Cloud: also open TCP 80 and 8080 on the VCN Security List / NSG"
echo "(Compute → Instance → Subnet → Default Security List → Ingress)."
echo
echo "Logs: docker compose -f $INSTALL_DIR/docker-compose.yml -f $INSTALL_DIR/docker-compose.public.yml logs -f"
