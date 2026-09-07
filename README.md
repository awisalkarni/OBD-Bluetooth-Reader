# OBD-II Bluetooth Reader

A modern Android application for communicating with ELM327 OBD-II Bluetooth automotive diagnostic adapters.

Originally developed for Android 2.x/3.x, this project has been modernized to modern Android development standards.

---

## Modern Tech Stack & Architecture

- **Language:** Kotlin 2.3+
- **Build System:** Gradle 9.x with Kotlin DSL (`build.gradle.kts`) & Version Catalog (`libs.versions.toml`)
- **Target SDK:** 36 (Android 14+) / Min SDK: 24 (Android 7.0+)
- **UI Framework:** Jetpack Compose with Material 3 (custom gauges, dark cockpit automotive styling)
- **Concurrency & State:** Kotlin Coroutines & `StateFlow` / `Channel`
- **Background Telemetry:** AndroidX Foreground Service (`connectedDevice` service type) with Notification Channels
- **Bluetooth:** Modern runtime permission handling (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`) and RFCOMM SPP socket communication
- **Unit Tests:** JUnit 4 test suite verifying PID calculation formulas and ELM327 response decoders

---

## Features

### 1. Live Telemetry Dashboard
- **Speed & RPM:** Real-time speed (km/h or mph) and engine RPM gauges.
- **Coolant Temperature Gauge:** Dynamic color-coded gradient temperature bar with cold/normal/hot status.
- **Fuel Economy:** Instant and average fuel consumption calculated dynamically from MAF (Mass Air Flow) or MAP (Manifold Absolute Pressure).
- **Environment & Runtime:** Ambient air temperature, intake air temperature, and engine runtime.
- **Live Parameters Table:** Full scrollable list of all queried OBD-II sensors and status readings.
- **Foreground Service:** Continuous background reading with status notification.

### 2. ELM327 Command Terminal
- Run ad-hoc standard OBD-II PIDs and ELM327 AT commands (`ATZ`, `ATE0`, `ATSP0`, `0100`, etc.).
- Inspect diagnostic trouble codes (`Mode 03`) and reset DTC error codes (`Mode 04`).
- Interactive console with formatted results and raw hex buffers.

### 3. Settings & Preferences
- **Bluetooth Adapter Picker:** Discover and select from bonded ELM327 Bluetooth devices.
- **Unit System:** Metric (km/h, °C, kPa) and Imperial (mph, °F, atm) conversion.
- **Vehicle Customization:** Engine displacement (liters) and volumetric efficiency calibration.
- **PID Selector:** Enable or disable individual sensor commands to optimize polling rates.

---

## Building and Running

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 36)

### Build Debug APK
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew test
```

### Clean
```bash
./gradlew clean
```
