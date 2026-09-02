# SmartCare 360 Android App

This directory contains the native Android wrapper project for **SmartCare 360 - Hospital Management System**.

- **Package Name:** `com.smartcare360.app`
- **Minimum Android Version:** Android 7.0 (API 24)
- **Target Android Version:** Android 14 (API 34)
- **Default Server URL:** `https://hospital-management-glt1-ge8d.onrender.com`

---

## 🚀 How to Download & Build the APK

### Method 1: Download via GitHub Actions (Zero Setup Required)

You do **NOT** need Android Studio or the Android SDK installed on your computer.

1. Go to your GitHub repository: [patilshivaprasad543/Hospital_Management](https://github.com/patilshivaprasad543/Hospital_Management)
2. Click on the **Actions** tab at the top.
3. In the left sidebar, click **Build Android APK**.
4. Click **Run workflow** (select the branch `main` and click the green button).
5. After ~2 minutes, the workflow finishes. Click on the completed run, scroll down to **Artifacts**, and download `SmartCare360-APK`.
6. Transfer or download the `.apk` file to your Android phone and tap to install!

---

### Method 2: Build Locally with Android Studio

1. Open **Android Studio**.
2. Select **Open** and choose the `Hospital__Management/android` folder.
3. Wait for Gradle sync to complete.
4. From the top menu, select **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. The generated APK will be located at:
   ```
   android/app/build/outputs/apk/debug/app-debug.apk
   ```

---

### Method 3: Command Line (if Android SDK is configured)

```bash
cd android
./gradlew assembleDebug
```

The APK will be generated at `android/app/build/outputs/apk/debug/app-debug.apk`.

---

## 📱 Features Included in the App

1. **Pull-to-Refresh:** Pull down anytime to reload the active hospital portal.
2. **File & Camera Uploads:** Directly snap camera photos of prescriptions or choose lab test reports from the phone gallery.
3. **Automatic PDF Downloads:** Consultation summaries, medical invoices, and lab reports are downloaded into your phone's `Downloads` folder with progress notifications.
4. **Persistent Session:** Keep doctors and patients logged in securely without having to re-authenticate on every app launch.
5. **Native Intent Routing:** Tapping phone numbers dials immediately; WhatsApp links open directly in WhatsApp.
6. **Offline Detection & Fallback:** Shows a friendly retry screen if your connection drops or while the Render free-tier server spins up.
