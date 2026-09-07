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

### 1. Live Telemetry Dashboard & Sport Cockpit
- **Sport Cockpit Mode:** Custom high-refresh canvas `CircularDialGauge` for RPM and Speed, plus dynamic G-Force Accelerometer bubble.
- **Coolant Temperature Gauge:** Dynamic color-coded temperature bar with cold/normal/hot status alerts.
- **Fuel Economy Engine:** Instant and average fuel consumption calculated dynamically from MAF or MAP.
- **Environment & Runtime:** Ambient air temperature, intake air temperature, and engine runtime.
- **Live Parameters Table:** Full scrollable list of all queried OBD-II sensors and status readings.
- **Foreground Service:** Continuous background reading with status notification.

### 2. Setup Wizard & ELM327 Probe
- **3-Step Guided Wizard:** Bluetooth permissions, device discovery, automated handshake probing (`ATZ`, `ATE0`, `ATRV`, `ATSP0`, `ATDP`).
- **Vehicle Presets:** 1-tap configuration presets (Compact Sedan, Turbo Hatch, SUV/Truck, Custom).

### 3. DTC Fault Code Scanner & Diagnostics
- **Comprehensive Fault Code Registry:** Embedded database with descriptions, severity ratings (`CRITICAL`, `WARNING`, `INFO`), symptoms, and probable causes.
- **Mode 0101 Status Check:** Real-time MIL (Check Engine Light) indicator and active DTC count.
- **Mode 03 Scan & Mode 04 Clear:** Read active diagnostic fault codes with one-tap clearing (with safety confirmation dialog).

### 4. Trip Telemetry Logging & CSV/JSON Export
- **Automated / Manual Session Recorder:** Record timestamped RPM, speed, coolant temp, fuel economy, and optional GPS coordinates.
- **Trip Summaries:** Calculates peak speed, average speed, max RPM, duration, and approximate distance.
- **One-Tap Export:** Share log files directly to Google Drive, email, or messaging apps via Android `FileProvider` in CSV or JSON format.

### 5. ELM327 Demo Simulator Mode
- **Zero-Hardware Testing:** Built-in dynamic vehicle emulator simulating realistic driving curves, gear changes, coolant warming, and DTC fault injection.
- **Full App Compatibility:** Allows complete testing of dashboards, gauges, diagnostic scanning, terminal queries, and logging without an OBD adapter or car.

### 6. ELM327 Command Terminal
- Run ad-hoc standard OBD-II PIDs and ELM327 AT commands (`ATZ`, `ATE0`, `ATSP0`, `0100`, etc.).
- Inspect diagnostic trouble codes (`Mode 03`) and reset DTC error codes (`Mode 04`).
- Interactive console with formatted results and raw hex buffers.

### 7. Settings & Preferences
- **Bluetooth Adapter Picker:** Discover and select from bonded ELM327 Bluetooth devices.
- **Simulator Mode & Auto Logging Toggles:** Switch between hardware Bluetooth and simulation instantly.
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
