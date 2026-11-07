# AutoPulse Telemetry — Android Auto Vehicle Diagnostics & Cloud Sync (Revised)

A real-time vehicle telemetry and diagnostic platform running seamlessly on Android Auto and mobile devices via the Android for Cars App Library and CarService APIs, featuring dual speed/distance sources (Car Hardware vs. GPS/Google Maps), optional decoupled cloud sync (Firebase Firestore & Google Drive), and distraction-free car-optimized cockpit displays.

---

## User Review & Critical Decisions (Feedback Incorporated)

> [!IMPORTANT]
> The implementation plan has been updated to reflect your exact comments and requirements:

- **Confirmed Decision 1 (Zero OBD Hardware Needed)**: You do **not** need an OBD 16-pin physical adapter. All telemetry is obtained directly through Android Auto's native `CarAppService` and `CarHardwareManager` APIs (`CarInfo`, `CarSensors`, `Speed`, `Mileage`, `EnergyLevel`, `Compass`).
- **Confirmed Decision 2 (Dual Telemetry Source: Car Hardware vs. GPS / Google Maps)**:
  - Users can switch their Speed and Distance measurement source anytime via the UI/Settings:
    1. **Car Hardware (Default)**: Reads high-accuracy wheel-tick and cluster speed/odometer from `CarHardwareManager.carInfo`.
    2. **GPS / Google Maps**: Calculates real-time ground speed and Haversine distance using Android's `FusedLocationProviderClient` / GPS telemetry.
  - Smooth fallback: if vehicle hardware sensors are unavailable on a specific car model, the app can automatically fall back to GPS speed with a clear visual source badge.
- **Confirmed Decision 3 (Decoupled, Optional Cloud Sync)**:
  - **100% Optional**: Driving, telemetry logging, and dashboard diagnostics work completely offline out-of-the-box without requiring user login.
  - **Independent Cloud Options**: The user can choose to sync to **Firebase Firestore**, **Google Drive**, **Both**, or **Local Only**.
  - **Non-Blocking Background Sync**: Sync operations run independently in the background; network issues or token expiry in one cloud provider will never block, fail, or interrupt sync to the other.
- **Confirmed Decision 4 (Google Sign-In Authentication)**:
  - When the user chooses to enable cloud sync, they authenticate securely with their Google Account using Jetpack `CredentialManager`.
  - User profile info (email, display name, avatar) is retrieved to configure Firestore user isolation and Google Drive export folders.
- **Confirmed Decision 5 (Android Auto Real-Time UI Optimization)**:
  - Designed specifically for vehicular environments: ultra-high legibility typography, high-contrast status icons, glanceable gauge tiles, and smooth UI updates (throttled/batched at ~1–2 Hz to ensure zero screen flicker and strict adherence to Google Car App Quality Guidelines).
- **Confirmed Decision 6 (Git Versioning & Commits)**:
  - Structured commits tracking initial plan/architecture, telemetry engine, Android Auto CarAppService, mobile Compose dashboard, and cloud sync pipelines.

---

## 1. Overview & Core Concept

- **What It Does**: AutoPulse connects your smartphone to the car via Android Auto projection, extracting real-time vehicle telemetry (`Speed`, `Mileage`, `EnergyLevel`, `Compass`, `Sensors`) and presenting a distraction-free, automotive-grade dashboard on the vehicle screen as well as a companion phone app.
- **Dual Speed Tracking**: Allows drivers to benchmark Car Cluster Speed vs. True GPS Ground Speed.
- **Cloud Analytics**: Users can selectively backup trip logs and performance diagnostics to Firebase Firestore or export structured JSON/CSV files to Google Drive.
- **No Extra Hardware**: Operates 100% through the phone's standard Android Auto cable or wireless connection and built-in sensors.

---

## 2. User Experience & Visual Design

### Android Auto Head Unit Display

1. **Live Cockpit Screen (`PaneTemplate` / `GridTemplate`)**:
   - Primary metric: Current Speed (large legible numerals, km/h or mph).
   - Source pill badge: `[ CAR HARDWARE ]` or `[ GPS GROUND ]` indicating active data stream.
   - Secondary gauges: Total Vehicle Mileage (Odometer), Fuel/Battery Percentage with visual level bar, and Estimated Remaining Driving Range.
   - Live Compass heading (N, NE, E, etc.) with angle readout.
2. **Trip Performance Logger (`ListTemplate`)**:
   - Active Trip Duration, Distance Travelled, Average Speed, Peak Speed.
   - Real-time Efficiency metric (km/L or kWh/100km).
   - Quick action buttons: `[ Start Trip ]`, `[ Pause ]`, `[ Save Trip ]`.
3. **Vehicle Health & Diagnostics Screen (`GridTemplate`)**:
   - Fuel Type / EV Profile (EV connector type, fuel grade).
   - Low Fuel / Battery Alert indicator.
   - Sensor Status (Compass, Accelerometer, Gyroscope availability).
   - Speed limiter / over-speed warning alerts.

### Mobile Phone Companion UI (Jetpack Compose)

- **Cockpit Dashboard**: Custom Canvas-rendered circular speedometer, live power arc, and odometer readout.
- **Source Switcher**: Floating toggle pill between "Car Hardware" and "GPS / Google Maps" with side-by-side delta display.
- **Telemetry Simulator**: Interactive dock for off-car testing (throttle slider, auto-cruise modes: Urban stop-and-go, Highway 110 km/h, Mountain climb).
- **Cloud Sync Settings**:
  - Google Account card with "Sign in with Google" button and profile summary.
  - Toggle switches: `Sync to Firebase Firestore` [ON/OFF], `Export to Google Drive` [ON/OFF].
  - Sync History log showing status, timestamp, and retry controls.
- **Trip History & Analytics**: Card list of saved trips with interactive drill-down and one-click JSON/CSV export.

### Visual Aesthetic & Theme

- **Cockpit Night Mode**: Dominant background `#0A0E17`, cards `#131B2A`, rim glow `#1E293B`.
- **Accent Signals**: Electric Cyan (`#00F2FE`) for speed, Eco Green (`#10B981`) for battery/fuel, Safety Amber (`#F59E0B`) for warnings, Rose Crimson (`#F43F5E`) for critical limits.
- **Typography**: Monospace automotive display numerals paired with high-legibility sans-serif labels.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Direct Android Auto CarHardware vs. OBD Dongle**
  - *Chosen Approach*: Exclusively use Android Auto's `CarHardwareManager` (`CarInfo` & `CarSensors`).
  - *Why*: Eliminates expensive OBD-II Bluetooth adapters, requires zero physical installation, and directly interfaces with Google-certified vehicle systems.
- **Decision 2: Dual Speed & Mileage Engine**
  - *Chosen Approach*: `TelemetryDataSourceManager` with pluggable `CarHardwareSource` and `GpsLocationSource`.
  - *Why*: Gives users complete choice, enables cross-validation between cluster speedometer and GPS ground speed, and ensures the app functions on all vehicles even if a specific OEM restricts certain hardware properties.
- **Decision 3: Independent, Fault-Tolerant Cloud Pipelines**
  - *Chosen Approach*: Decoupled sync workers (`FirestoreSyncWorker` and `GoogleDriveExportWorker`).
  - *Why*: A failure in Google Drive (e.g. token expired, quota) never interrupts Firestore live database sync, and vice versa. Both are completely optional.
- **Decision 4: Distraction-Free Android Auto Template Throttling**
  - *Chosen Approach*: Stream live data at up to 10 Hz internally for the phone canvas, but debounce/throttle head unit template refreshes to ~1 Hz.
  - *Why*: Android Auto imposes strict rate limits on template invalidation to prevent driver distraction and UI jank.

---

## 4. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          AutoPulse Telemetry                           │
└────────────────────────────────────────────────────────────────────────┘
                                    │
           ┌────────────────────────┴────────────────────────┐
           ▼                                                 ▼
┌──────────────────────────────┐              ┌──────────────────────────────┐
│  Source A: Car Hardware API  │              │   Source B: GPS / Location   │
│ (CarHardwareManager on AA)   │              │   (FusedLocationProvider)    │
│ - Car Speed (Display & Raw)  │              │ - GPS Ground Speed           │
│ - Odometer / Mileage         │              │ - Haversine Distance Calc    │
│ - Energy / Fuel Level        │              │ - Altitude & Heading         │
└──────────────┬───────────────┘              └──────────────┬───────────────┘
               │                                             │
               └──────────────────────┬──────────────────────┘
                                      ▼
                      ┌───────────────────────────────┐
                      │    TelemetrySourceSelector    │
                      │  (Car Hardware vs. GPS Ground)│
                      └───────────────┬───────────────┘
                                      │
           ┌──────────────────────────┼──────────────────────────┐
           ▼                          ▼                          ▼
┌──────────────────────┐   ┌──────────────────────┐   ┌──────────────────────┐
│  Android Auto Screen │   │  Jetpack Compose UI  │   │   Trip Recorder &    │
│  (CarAppService AA)  │   │     (Phone App)      │   │    Cloud Sync Hub    │
│ - Cockpit Live View  │   │ - Vector Speedometer │   │ - Local Room Cache   │
│ - Trip Performance   │   │ - Source Switcher    │   │ - Optional Google ID │
│ - Diagnostic Status  │   │ - Offline Simulator  │   │ - Firebase Firestore │
│ - Glanceable Alerts  │   │ - Trip History & CSV │   │ - Google Drive Sync  │
└──────────────────────┘   └──────────────────────┘   └──────────────────────┘
```

### Data Models

```kotlin
// Source selection enumeration
enum class TelemetrySource {
    CAR_HARDWARE,
    GPS_LOCATION
}

// Unified live telemetry state
data class LiveVehicleTelemetry(
    val currentSpeedKmh: Float = 0f,
    val displaySpeedKmh: Float = 0f,
    val gpsSpeedKmh: Float = 0f,
    val activeSource: TelemetrySource = TelemetrySource.CAR_HARDWARE,
    val odometerKm: Double = 0.0,
    val tripDistanceKm: Double = 0.0,
    val fuelLevelPercent: Float? = null,
    val batteryLevelPercent: Float? = null,
    val rangeRemainingKm: Float? = null,
    val compassDegrees: Float = 0f,
    val isFuelLow: Boolean = false,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

// Cloud-synced trip record (Firestore & Google Drive JSON/CSV)
data class TripData(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val totalDistanceKm: Double = 0.0,
    val averageSpeedKmh: Float = 0f,
    val topSpeedKmh: Float = 0f,
    val sourceUsed: String = "CAR_HARDWARE",
    val syncedToFirestore: Boolean = false,
    val exportedToDrive: Boolean = false,
    val samples: List<TelemetryPoint> = emptyList()
)
```

---

## 5. Phased Execution Steps

1. **Phase 1: Dependencies, Manifest & Permissions**
   - Add `androidx.car.app:app` and `androidx.car.app:app-projected`.
   - Add `play-services-location` for GPS ground speed and distance.
   - Configure automotive app descriptor and permissions (`CAR_SPEED`, `CAR_MILEAGE`, `CAR_FUEL`, `ACCESS_FINE_LOCATION`).
2. **Phase 2: Dual Telemetry Engine & Vehicle Simulator**
   - Implement `CarHardwareSource` using `CarHardwareManager.carInfo` and `carSensors`.
   - Implement `GpsTelemetrySource` using `FusedLocationProviderClient`.
   - Build unified `TelemetryRepository` with source selection and realistic simulator mode.
3. **Phase 3: Android Auto Cockpit Screens**
   - `AutoPulseCarAppService` and `AutoPulseSession`.
   - `DashboardScreen`: Glanceable speedometer, odometer, and range bar.
   - `TripStatsScreen`: Live trip logger, averages, and source toggle.
   - `VehicleDiagnosticsScreen`: Vehicle health indicators.
4. **Phase 4: Phone Companion App (Jetpack Compose)**
   - High-contrast cockpit UI with Canvas gauge, source selector, and live graphs.
   - Built-in vehicle simulator dock for off-car testing.
   - Trip logger recorder bar with start/pause/stop and local database storage.
5. **Phase 5: Optional Google Sign-In & Decoupled Cloud Sync**
   - Jetpack `CredentialManager` Google Sign-In (optional login flow).
   - Firebase Firestore sync repository with multi-user isolation.
   - Google Drive JSON/CSV export engine with SAF and Drive API integration.
   - Non-blocking background sync execution.
6. **Phase 6: Git Versioning & Compilation Verification**
   - Verify build with `compile_applet`.
   - Create progressive git commits reflecting the milestones.
