# Deploy SmartCare 360 to the Internet

This guide publishes the app on a public URL **without exposing admin credentials**.

## 24/7 free (Always Free VM)

**Render Free is not 24/7.** Idle services sleep after about 15 minutes. GitHub keep-alive helps but cannot guarantee a never-sleeping free Render instance.

The free way to stay up all day is an **always-on Linux VM** (Oracle Cloud Always Free Ampere, or a PC you leave on) with Docker:

1. Create a free Oracle Cloud account: [cloud.oracle.com](https://cloud.oracle.com) → Always Free **VM.Standard.A1.Flex** (or AMD micro) Ubuntu 22.04.
2. Open **ingress TCP 22, 80, and 8080** on the VCN security list / NSG.
3. SSH into the VM and run:

```bash
sudo bash -c 'curl -fsSL https://raw.githubusercontent.com/patilshivaprasad543/Hospital_Management/main/scripts/install-always-free.sh | bash'
```

The script installs Docker, builds the app, and prints the public URL plus a generated admin password. First build takes several minutes. Then open `http://YOUR_VM_PUBLIC_IP`.

This environment cannot create an Oracle account or click your cloud console for you. After the VM exists, that one command is the full install.

### Render 502 / 505 / “Application failed”

Those pages mean the Java process never stayed up (or there is no successful deploy). A common cause was missing `SMARTCARE_ADMIN_*` env vars, which used to crash startup. Current `main` generates admin credentials on first boot and writes them to the service logs (and `/app/data/admin-credentials.txt` on the disk).

**Current live site:** [https://hospital-management-glt1.onrender.com](https://hospital-management-glt1.onrender.com) — home, `/login`, and `/health` return 200. GitHub keep-alive pings this URL. In the Render dashboard set `SMARTCARE_APP_URL=https://hospital-management-glt1.onrender.com` so the app also pings itself.

### Keep logins and records (do this once in Render)

The Java app **already writes every user, appointment, and order through JPA** (`ddl-auto=update`). Nothing extra is missing in code. Data only survives if the **web service** is connected to Postgres.

**Live check (this repo):** `GET https://hospital-management-glt1.onrender.com/health` currently returns `"storage":"H2"` and `"databaseUrlSet":"no"`. Creating a Postgres instance in Render is **not** enough — the web process never received `DATABASE_URL`. Until that env var is on `hospital-management-glt1` and the service is redeployed, writes go to an in-container H2 file and are wiped on sleep/redeploy.

Do this on the **web** service (not only on the database page):

1. Render Dashboard → **New → PostgreSQL** → instance **Free** → create (skip if you already created one).
2. Open the database → copy **Internal Database URL** (`postgres://…@dpg-…-a/…`). Prefer Internal URL when the web service is on the same Render region.
3. Open **Web Service** `hospital-management-glt1` (not the Postgres instance) → **Environment** → **Add Environment Variable**:
   - Key must be exactly `DATABASE_URL`
   - Value = that Internal Database URL (paste the whole `postgres://user:pass@host:5432/dbname` string)
4. Optional but clearer: on the web service, **Connect** / **Link** the Postgres database so Render injects `DATABASE_URL` for you.
5. **Save** env changes, then **Manual Deploy** → deploy `main`.
6. Confirm `https://hospital-management-glt1.onrender.com/health` shows `"storage":"PostgreSQL"`, `"databaseUrlSet":"yes"`, `"persistent":"yes"`.

Demo logins are still seeded if missing. Your own registrations persist only after step 6 succeeds.

`render.yaml` deploys the **web service only** (no Blueprint Postgres/disk — those often fail on Render Free). This environment cannot set Render dashboard env vars for you.

---

## Fix: `Couldn't find a package.json file` / 502 no-deploy

Render created this service as **Node**. The repo now includes `package.json` so that service can build Java 21 and start with `npm start`. Push `main` (or Manual Deploy) and wait for the build (several minutes).

If you prefer Docker: New → Blueprint (`render.yaml`) or New → Web Service → Language = **Docker**.

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

## 5. Docker for free (your PC or a free VM)

Docker Desktop / Docker Engine is free. The app stays up as long as the machine stays on (`restart: unless-stopped`).

```bash
cp .env.example .env
# Edit .env: set SMARTCARE_ADMIN_EMAIL and SMARTCARE_ADMIN_PASSWORD
# For a VM, also set SMARTCARE_APP_URL=http://YOUR_PUBLIC_IP:8080

docker compose up -d --build
```

Open http://localhost:8080 (or `http://YOUR_PUBLIC_IP:8080` on a VM).

Useful commands:

```bash
docker compose logs -f
docker compose ps
docker compose down          # stop
```

Data is stored in the Docker volume `smartcare-data`, so restarts keep records.

Without Compose:

```bash
docker build -t smartcare360 .
docker run -d --restart unless-stopped --name smartcare360 \
  -p 8080:8080 --env-file .env \
  -e SPRING_PROFILES_ACTIVE=prod -e PORT=8080 \
  -v smartcare-data:/app/data \
  smartcare360
```

A free always-on host is your own computer left running, or an Oracle Cloud Always Free VM. On Ubuntu as root: `bash scripts/install-always-free.sh` (opens ports 80 and 8080, builds Docker, prints the public URL). Also open TCP 80/8080 in the Oracle VCN security list.

## 6. After deployment

- Share your public URL and the **demo accounts** from README (not admin).
- Test registration OTP, appointment booking, and notification log at `/admin/notification-log` (admin login only).

## 7. Stay live 24/7 for free

Render Free sleeps after about 15 minutes with no traffic. This repo already turns on **every free keep-alive we can run in GitHub + the app**. No paid Render plan is required.

What is already on `main`:

| Free mechanism | What it does |
|----------------|----------------|
| GitHub Action **Keep site live** | Pings `/health`, `/ping`, `/login`, `/` on the hardcoded Render URLs on several 5-minute cron offsets |
| GitHub Action **Keep site live (backup)** | Second independent pinger so one missed schedule still wakes the service |
| App **KeepAlivePinger** | While the process is up, it pings its own `/health` every 3 minutes |
| Cheap health URLs | `/health`, `/healthz`, `/ping`, `/keepalive` |
| Faster JVM start | Dockerfile flags so a wake-from-sleep is shorter |

Optional extra (also free, one-time in a browser): create an [UptimeRobot](https://uptimerobot.com) or [cron-job.org](https://cron-job.org) HTTPS monitor every 5 minutes to `https://YOUR-RENDER-URL/health`. That is not required if GitHub Actions is running.

GitHub only runs these schedules after the workflow files are on **main**. This branch is meant to be on `main`.

If the first visitor still waits ~1 minute, that is a cold start after a deploy — not a missing keep-alive.

## Send me your credentials

When you are ready, provide email and WhatsApp details **only through your hosting provider’s secret environment variable UI** (Render dashboard), not in chat or in code. Use `.env.example` as the checklist.
