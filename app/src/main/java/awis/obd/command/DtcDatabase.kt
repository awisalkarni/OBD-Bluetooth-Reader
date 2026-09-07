package awis.obd.command

enum class DtcSeverity {
    CRITICAL,   // Emissions or immediate engine damage risk (misfires, catalyst, fuel pressure)
    WARNING,    // Sensors, circuit faults, rationality checks
    INFO        // Pending or advisory codes
}

data class DtcInfo(
    val code: String,
    val title: String,
    val system: String,
    val severity: DtcSeverity,
    val possibleCauses: List<String> = emptyList(),
    val symptoms: String = ""
)

object DtcDatabase {

    private val DATABASE: Map<String, DtcInfo> = mapOf(
        "P0100" to DtcInfo(
            code = "P0100",
            title = "Mass or Volume Air Flow Circuit Malfunction",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Faulty MAF sensor", "Intake air leak", "Damaged wiring harness"),
            symptoms = "Engine hesitation, rough idle, poor acceleration"
        ),
        "P0101" to DtcInfo(
            code = "P0101",
            title = "Mass or Volume Air Flow Circuit Range/Performance Problem",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Dirty MAF sensing wire", "Vacuum leak", "Restricted air filter"),
            symptoms = "Stalling, black smoke, loss of engine power"
        ),
        "P0102" to DtcInfo(
            code = "P0102",
            title = "Mass or Volume Air Flow Circuit Low Input",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Short to ground", "Faulty sensor connection", "Sensor element failure"),
            symptoms = "Check engine light ON, hard starting, poor MPG"
        ),
        "P0103" to DtcInfo(
            code = "P0103",
            title = "Mass or Volume Air Flow Circuit High Input",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Short to voltage", "Defective MAF sensor", "Open signal circuit"),
            symptoms = "Rich fuel mixture, black exhaust, engine running rough"
        ),
        "P0115" to DtcInfo(
            code = "P0115",
            title = "Engine Coolant Temperature Circuit Malfunction",
            system = "Cooling System",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Faulty ECT sensor", "Corroded connector", "Low coolant level"),
            symptoms = "Hard cold start, cooling fan constantly on, rich running"
        ),
        "P0118" to DtcInfo(
            code = "P0118",
            title = "Engine Coolant Temperature Circuit High Input",
            system = "Cooling System",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Open ECT sensor circuit", "Sensor unplugged or damaged"),
            symptoms = "High idle speed, poor fuel economy, temp gauge reads cold"
        ),
        "P0120" to DtcInfo(
            code = "P0120",
            title = "Throttle Position Sensor / Switch A Circuit Malfunction",
            system = "Throttle Control",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Worn TPS potentiometer", "Loose connector pin", "Sticking throttle plate"),
            symptoms = "Sudden surging, limp home mode, hesitation under throttle"
        ),
        "P0130" to DtcInfo(
            code = "P0130",
            title = "O2 Sensor Circuit Malfunction (Bank 1, Sensor 1)",
            system = "Exhaust Emissions",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Failed oxygen sensor heater or cell", "Exhaust leak before sensor"),
            symptoms = "High fuel consumption, failed emissions test"
        ),
        "P0133" to DtcInfo(
            code = "P0133",
            title = "O2 Sensor Circuit Slow Response (Bank 1, Sensor 1)",
            system = "Exhaust Emissions",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Aged O2 sensor", "Contaminated fuel", "Exhaust manifold leak"),
            symptoms = "Decreased fuel economy, sluggish acceleration"
        ),
        "P0171" to DtcInfo(
            code = "P0171",
            title = "System Too Lean (Bank 1)",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Intake manifold vacuum leak", "Dirty MAF", "Weak fuel pump or clogged filter"),
            symptoms = "Misfires, pinging/knocking under load, rough idling"
        ),
        "P0172" to DtcInfo(
            code = "P0172",
            title = "System Too Rich (Bank 1)",
            system = "Air & Fuel Metering",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Leaking fuel injector", "Excessive fuel pressure", "Stuck purge valve"),
            symptoms = "Strong gas smell, fouled spark plugs, black exhaust smoke"
        ),
        "P0300" to DtcInfo(
            code = "P0300",
            title = "Random / Multiple Cylinder Misfire Detected",
            system = "Ignition / Combustion",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Worn spark plugs", "Failing ignition coils", "Low fuel pressure", "Vacuum leak"),
            symptoms = "Shaking engine, flashing Check Engine Light, catalytic converter danger"
        ),
        "P0301" to DtcInfo(
            code = "P0301",
            title = "Cylinder 1 Misfire Detected",
            system = "Ignition / Cylinder 1",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Cylinder 1 spark plug/coil failure", "Injector 1 clog", "Low compression"),
            symptoms = "Engine stutter under load, loss of power"
        ),
        "P0302" to DtcInfo(
            code = "P0302",
            title = "Cylinder 2 Misfire Detected",
            system = "Ignition / Cylinder 2",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Cylinder 2 spark plug/coil failure", "Injector 2 clog"),
            symptoms = "Engine shudder, irregular exhaust note"
        ),
        "P0303" to DtcInfo(
            code = "P0303",
            title = "Cylinder 3 Misfire Detected",
            system = "Ignition / Cylinder 3",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Cylinder 3 spark plug/coil failure", "Injector 3 clog"),
            symptoms = "Engine shudder, irregular exhaust note"
        ),
        "P0304" to DtcInfo(
            code = "P0304",
            title = "Cylinder 4 Misfire Detected",
            system = "Ignition / Cylinder 4",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Cylinder 4 spark plug/coil failure", "Injector 4 clog"),
            symptoms = "Engine shudder, irregular exhaust note"
        ),
        "P0420" to DtcInfo(
            code = "P0420",
            title = "Catalyst System Efficiency Below Threshold (Bank 1)",
            system = "Emissions Control",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Degraded catalytic converter", "Downstream O2 sensor fault", "Exhaust leak"),
            symptoms = "Check engine light ON, rotten egg sulfur smell, failed inspection"
        ),
        "P0440" to DtcInfo(
            code = "P0440",
            title = "Evaporative Emission Control System Malfunction",
            system = "EVAP System",
            severity = DtcSeverity.INFO,
            possibleCauses = listOf("Loose or defective gas cap", "Cracked EVAP vapor line", "Canister vent valve"),
            symptoms = "Check engine light ON, subtle fuel odor"
        ),
        "P0442" to DtcInfo(
            code = "P0442",
            title = "Evaporative Emission Control System Leak Detected (Small Leak)",
            system = "EVAP System",
            severity = DtcSeverity.INFO,
            possibleCauses = listOf("Loose gas cap", "Worn purge solenoid seal", "Minor hose leak"),
            symptoms = "Check engine light ON, no noticeable driveability symptoms"
        ),
        "P0500" to DtcInfo(
            code = "P0500",
            title = "Vehicle Speed Sensor Malfunction",
            system = "Vehicle Speed & Transmission",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Faulty VSS sensor", "Damaged gear drive", "Instrument cluster wiring"),
            symptoms = "Speedometer inoperative or erratic, harsh automatic gear shifts"
        ),
        "P0505" to DtcInfo(
            code = "P0505",
            title = "Idle Air Control System Malfunction",
            system = "Idle Speed Control",
            severity = DtcSeverity.WARNING,
            possibleCauses = listOf("Carbon buildup on IAC valve", "IAC motor fault", "Throttle body air leak"),
            symptoms = "Engine dies coming to a stop, unusually high or low idle speed"
        ),
        "P0600" to DtcInfo(
            code = "P0600",
            title = "Serial Communication Link Malfunction",
            system = "CAN Bus & Network",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("CAN Bus wiring short", "Loss of module power/ground", "Faulty ECU"),
            symptoms = "Multiple warning lights, no-start or transmission limp mode"
        ),
        "U0100" to DtcInfo(
            code = "U0100",
            title = "Lost Communication with ECM / PCM 'A'",
            system = "CAN Bus Communication",
            severity = DtcSeverity.CRITICAL,
            possibleCauses = listOf("Dead battery/ground", "Corroded PCM connector", "CAN High/Low circuit open"),
            symptoms = "Vehicle may crank but not start, total cluster outage"
        )
    )

    fun lookup(code: String): DtcInfo {
        val cleanCode = code.trim().uppercase()
        return DATABASE[cleanCode] ?: generateGeneric(cleanCode)
    }

    private fun generateGeneric(code: String): DtcInfo {
        val letter = code.firstOrNull() ?: 'P'
        val system = when (letter) {
            'P' -> "Powertrain (Engine & Transmission)"
            'C' -> "Chassis (ABS, Steering, Suspension)"
            'B' -> "Body (Airbags, AC, Interior Electronics)"
            'U' -> "Network & CAN Bus Communication"
            else -> "Vehicle Electronic Subsystem"
        }
        val isManufacturerSpecific = code.length >= 2 && (code[1] == '1' || code[1] == '2' || code[1] == '3')
        val title = if (isManufacturerSpecific) {
            "Manufacturer-Specific Diagnostic Code"
        } else {
            "Standard SAE OBD-II Fault Code"
        }

        return DtcInfo(
            code = code,
            title = title,
            system = system,
            severity = if (letter == 'P' || letter == 'U') DtcSeverity.WARNING else DtcSeverity.INFO,
            possibleCauses = listOf("Consult manufacturer service manual for code $code"),
            symptoms = "Check engine light illuminated"
        )
    }
}
