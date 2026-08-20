# Deploy SmartCare 360 to the Internet

This guide publishes the app on a public URL **without exposing admin credentials**.

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

## 2. Gmail setup (for email OTP)

1. Enable 2FA on your Google account.
2. Create an **App Password**: Google Account → Security → App passwords.
3. Set `SMARTCARE_MAIL_USERNAME` = your Gmail address.
4. Set `SMARTCARE_MAIL_PASSWORD` = the 16-character app password.

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

## Send me your credentials

When you are ready, provide email and WhatsApp details **only through your hosting provider’s secret environment variable UI** (Render dashboard), not in chat or in code. Use `.env.example` as the checklist.
