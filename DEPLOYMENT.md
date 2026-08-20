# Deploy SmartCare 360 to the Internet

This guide publishes the app on a public URL **without exposing admin credentials**.

## Fix: `Couldn't find a package.json file`

This error means Render is trying to deploy as **Node.js**, but SmartCare 360 is a **Java / Spring Boot** app.

**You cannot change runtime on an existing service.** Do this:

1. **Delete** the failed Render web service (Dashboard → Service → Settings → Delete).
2. Create again using **one** of these methods:

### Option A — Blueprint (recommended)

1. Render Dashboard → **New** → **Blueprint**
2. Connect repo `patilshivaprasad543/Hospital_Management`
3. Render reads `render.yaml` at the repo root (`runtime: docker`)
4. Add secret env vars (admin, Gmail) in the dashboard
5. Deploy

### Option B — Manual Docker web service

1. Render Dashboard → **New** → **Web Service**
2. Connect your GitHub repo
3. Set **Language** to **Docker** (not Node, not Python)
4. **Dockerfile Path:** `Dockerfile` (repo root)
5. **Root Directory:** leave empty (or `.`)
6. Add environment variables from the table below
7. Deploy

Do **not** use Node, npm, or `package.json` — this project uses Gradle (`build.gradle`) and Docker.

---

## 1. Choose a host (recommended: Render — free tier)

1. Push this repository to GitHub.
2. Sign up at [render.com](https://render.com).
3. **New → Blueprint** → connect your GitHub repo (Render reads `render.yaml`).
4. Set **secret environment variables** in the Render dashboard (never commit these):

| Variable | Description |
|----------|-------------|
| `SMARTCARE_APP_URL` | Your public URL, e.g. `https://smartcare360.onrender.com` |
| `SMARTCARE_ADMIN_EMAIL` | **Your private admin email** (not published) |
| `SMARTCARE_ADMIN_PASSWORD` | **Strong password** (not `Admin@360`) |
| `SMARTCARE_ADMIN_MOBILE` | Admin mobile for notifications |
| `SMARTCARE_MAIL_USERNAME` | Gmail or SMTP email for OTP/notifications |
| `SMARTCARE_MAIL_PASSWORD` | Gmail app password or SMTP password |
| `TWILIO_ACCOUNT_SID` | (Optional) Twilio WhatsApp |
| `TWILIO_AUTH_TOKEN` | (Optional) Twilio auth token |
| `TWILIO_WHATSAPP_FROM` | (Optional) Twilio WhatsApp sender |
| `SMARTCARE_WHATSAPP_ENABLED` | `true` when Twilio is configured |

5. Deploy. Render builds the Docker image and assigns a URL like `https://smartcare360.onrender.com`.

## 2. Email setup (OTP & notifications)

### Why Gmail SMTP may not work on Render free

**Render free web services block outbound SMTP ports (25, 465, 587).** Gmail SMTP uses port 587, so registration and forgot-password emails will **not send** on Render free even with correct Gmail credentials. This is a hosting restriction, not an app bug.

**Options:**

| Option | Best for |
|--------|----------|
| **Brevo API (recommended)** | Render free tier — uses HTTPS (port 443) |
| **Gmail SMTP** | Local development (`./scripts/start.sh`) |
| **Paid Render plan** | Gmail SMTP works on paid instances |

### Option A — Brevo (production on Render free)

1. Sign up at [brevo.com](https://www.brevo.com) (free tier: 300 emails/day).
2. Create an **API key**: Settings → SMTP & API → API Keys.
3. Add and verify a **sender email** in Brevo (Settings → Senders).
4. Set these in Render environment variables:

| Variable | Value |
|----------|-------|
| `SMARTCARE_MAIL_PROVIDER` | `brevo` |
| `SMARTCARE_BREVO_API_KEY` | Your Brevo API key |
| `SMARTCARE_BREVO_SENDER_EMAIL` | Verified sender email in Brevo |
| `SMARTCARE_BREVO_SENDER_NAME` | `SmartCare 360` |

`render.yaml` already defaults `SMARTCARE_MAIL_PROVIDER=brevo` for Blueprint deploys.

### Option B — Gmail SMTP (local development)

1. Enable 2FA on your Google account.
2. Create an **App Password**: Google Account → Security → App passwords.
3. Add to local `.env` (gitignored):

```
SMARTCARE_MAIL_PROVIDER=smtp
SMARTCARE_MAIL_USERNAME=your@gmail.com
SMARTCARE_MAIL_PASSWORD=your-16-char-app-password
```

4. Start with `./scripts/start.sh` so `.env` is loaded.

### Option C — Gmail SMTP on paid Render

Upgrade the Render web service to any **paid** instance type. Then set `SMARTCARE_MAIL_PROVIDER=smtp` with Gmail credentials.


## 3. WhatsApp setup (optional, via Twilio)

1. Create a [Twilio](https://www.twilio.com) account.
2. Enable WhatsApp Sandbox or a WhatsApp sender.
3. Set `SMARTCARE_WHATSAPP_ENABLED=true` and the Twilio variables above.

## 4. What is public vs private

| Public (on website / README) | Private (env vars only) |
|------------------------------|-------------------------|
| Patient, doctor, lab, pharmacy demo logins | Admin email & password |
| Registration & OTP flows | SMTP / Twilio secrets |
| Home, About, Contact pages | |

Production startup **fails** if admin password is missing or still the default `Admin@360`.

## 5. Docker (any VPS)

```bash
docker build -t smartcare360 .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SMARTCARE_APP_URL=https://your-domain.com \
  -e SMARTCARE_ADMIN_EMAIL=your@email.com \
  -e SMARTCARE_ADMIN_PASSWORD=your-strong-password \
  -e SMARTCARE_MAIL_USERNAME=... \
  -e SMARTCARE_MAIL_PASSWORD=... \
  -v smartcare-data:/app/data \
  smartcare360
```

## 6. After deployment

- Share your public URL and the **demo accounts** from README (not admin).
- Test registration OTP, appointment booking, and notification log at `/admin/notification-log` (admin login only).

## 7. Keep the site live 24/7 (no “waking up”)

Render **free** web services **sleep after about 15 minutes with no traffic**. The next visitor sees a host “service waking up” page for 30–90 seconds. That is the host, not an application crash.

### What you should do (pick one)

| Option | Cost | Result |
|--------|------|--------|
| **A. Upgrade Render to Starter (recommended)** | Paid monthly | Instance stays running. This is the only reliable always-on option. |
| **B. External uptime ping + GitHub keep-alive** | Free | Pings `/health` every few minutes so free instances usually do not sleep. GitHub cron can be late; use UptimeRobot as well. |
| **C. Your own VPS / Railway / Fly.io paid VM** | Varies | You control uptime; use the Docker run command in section 5. |

### Option A — Render paid (do this if you need it always live)

1. Open the **smartcare360** service on Render.
2. **Settings → Instance type** → change **Free** to **Starter** (or higher).
3. Keep the persistent disk mounted at `/app/data`.
4. Confirm health check path is `/health`.

Do not leave `plan: free` in the dashboard if you need zero sleep. Blueprint YAML still says `free` so new Blueprint deploys stay free until you upgrade.

### Option B — Free keep-alive (reduce sleep)

1. Confirm `GET https://YOUR-URL/health` returns `{"status":"ok"}`.
2. **UptimeRobot** (or cron-job.org): create an HTTPS monitor every **5 minutes** to `https://YOUR-URL/health`.
3. **GitHub Actions:** repo **Settings → Secrets and variables → Actions** → add secret `SMARTCARE_APP_URL` = `https://YOUR-URL` (no trailing slash). The workflow `.github/workflows/keep-alive.yml` pings `/health` every 10 minutes.

Pings cannot help if Render pauses the service for other reasons (account limits, deploy in progress, disk/build failure).

### After you upgrade or add pings

Redeploy so `/health` and faster JVM startup from this update are live. First request after a deploy can still take a minute while the container starts.

## Send me your credentials

When you are ready, provide email and WhatsApp details **only through your hosting provider’s secret environment variable UI** (Render dashboard), not in chat or in code. Use `.env.example` as the checklist.
