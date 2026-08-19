# Running SmartCare 360 in Eclipse

This project is configured for **Eclipse IDE** with **Buildship (Gradle)** and **Java 21**.

## Requirements

1. **Eclipse IDE for Enterprise Java and Web Developers** (2024-03 or newer recommended)
2. **Java JDK 21** installed and configured in Eclipse  
   `Window → Preferences → Java → Installed JREs`
3. **Buildship Gradle Integration** (included in Eclipse IDE for Enterprise Java)
4. **Spring Tools 4** (optional, for Spring Boot Dashboard and Boot launch config)

## Import the project

### Option A — Import as Gradle project (recommended)

1. Open Eclipse
2. `File → Import → Gradle → Existing Gradle Project`
3. Select the project root folder (`Hospital_Management`)
4. Click **Finish**
5. Wait for Gradle sync to complete (dependencies download on first import)

### Option B — Import as existing project

1. `File → Import → General → Existing Projects into Workspace`
2. Select the project root folder
3. Ensure **Hospital_Management** is checked → **Finish**
4. Right-click project → **Gradle → Refresh Gradle Project**

## Run the application

After Gradle sync finishes:

### Using the shared launch configuration

1. `Run → Run Configurations…`
2. Open **Java Application → Hospital_Management** (or **Spring Boot App → Hospital_Management (Spring Boot)** if Spring Tools is installed)
3. Click **Run**

Or right-click `HospitalManagementApplication.java` → **Run As → Java Application** (or **Spring Boot App**).

The app starts at: **http://localhost:8080**

## Default login accounts

| Role     | Email                         | Password   |
|----------|-------------------------------|------------|
| Admin    | admin@smartcare360.com        | Admin@360  |
| Patient  | patient@smartcare360.com      | patient123 |
| Doctor   | sarah.jenkins@smartcare360.com| doc123     |
| Pharmacy | pharmacy@smartcare360.com     | vendor123  |

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Project has errors after import | Right-click project → **Gradle → Refresh Gradle Project** |
| Java version mismatch | Set project JRE to **JavaSE-21** in `Project → Properties → Java Build Path → Libraries` |
| Dependencies missing | Run `./gradlew build` once from terminal, then refresh Gradle project |
| Port 8080 in use | Change `server.port` in `src/main/resources/application.properties` |
| Eclipse metadata out of date | Run `./gradlew eclipse` then refresh project |

## Refresh Eclipse files

If you change dependencies in `build.gradle`:

```bash
./gradlew eclipse
```

Then in Eclipse: right-click project → **Refresh** (F5).

## Build from Eclipse

- **Gradle tasks:** `Window → Show View → Gradle` → run `build` or `bootRun`
- **Terminal:** `./gradlew bootRun`
