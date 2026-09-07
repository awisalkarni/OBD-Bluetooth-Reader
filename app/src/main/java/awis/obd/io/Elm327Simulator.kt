package awis.obd.io

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Simulated ELM327 OBD-II adapter engine.
 * Generates realistic dynamic vehicle telemetry (RPM, speed, coolant, MAF, fuel economy)
 * and responds to AT commands and standard Mode 01 / Mode 03 / Mode 04 requests.
 * Allows users to test every feature of the app without needing a physical vehicle or OBD adapter.
 */
object Elm327Simulator {
    private const val TAG = "Elm327Simulator"

    private val _telemetry = MutableStateFlow(ObdTelemetry(ConnectionState.DISCONNECTED))
    val telemetry: StateFlow<ObdTelemetry> = _telemetry.asStateFlow()

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    @Volatile
    var isSimulating: Boolean = false
        private set

    // Synthetic dynamic parameters
    private var tick: Long = 0
    private var baseSpeed: Double = 45.0
    private var baseRpm: Double = 2200.0
    private var baseCoolant: Double = 88.0

    fun start() {
        if (isSimulating) return
        isSimulating = true
        tick = 0

        _telemetry.value = ObdTelemetry(
            connectionState = ConnectionState.CONNECTING,
            statusMessage = "Connecting to ELM327 Simulator..."
        )

        simulationJob = scope.launch {
            delay(600)
            _telemetry.value = ObdTelemetry(
                connectionState = ConnectionState.INITIALIZING,
                statusMessage = "Simulator: ATZ, ATE0, ATSP0 OK"
            )
            delay(500)

            while (isActive && isSimulating) {
                tick++
                // Calculate realistic undulating vehicle values
                // Speed oscillates between 30 and 110 km/h
                val speed = (65.0 + 35.0 * sin(tick * 0.1) + Random.nextDouble(-2.0, 2.0)).toInt().coerceIn(0, 180)
                // RPM correlates with speed and gear shifting simulation
                val gear = when {
                    speed < 20 -> 1
                    speed < 45 -> 2
                    speed < 70 -> 3
                    speed < 95 -> 4
                    else -> 5
                }
                val rpm = ((speed * 40.0 / gear) + 800.0 + Random.nextDouble(-50.0, 50.0)).toInt().coerceIn(800, 6500)
                val coolant = (88.0 + 4.0 * sin(tick * 0.02)).toInt().coerceIn(75, 105)
                val ambient = 24
                val intake = coolant - 45 + (speed / 10)
                val fuelEcon = String.format(java.util.Locale.US, "%.1f l/100km", (14.0 - (speed * 0.08)).coerceIn(5.2, 18.0))

                val dataMap = mapOf(
                    "Engine RPM" to "$rpm RPM",
                    "Vehicle Speed" to "$speed km/h",
                    "Engine Coolant Temperature" to "$coolant C",
                    "Air Intake Temperature" to "$intake C",
                    "Ambient Air Temperature" to "$ambient C",
                    "Fuel Economy" to fuelEcon,
                    "Engine Run Time" to "${tick * 2} sec",
                    "Mass Air Flow" to String.format(java.util.Locale.US, "%.2f g/s", (rpm * 0.012)),
                    "Intake Manifold Absolute Pressure" to "${(45 + (rpm / 100))} kPa",
                    "Control Module Voltage" to "14.2 V",
                    "Barometric Pressure" to "101 kPa",
                    "Distance Traveled with MIL on" to "0 km"
                )

                _telemetry.value = ObdTelemetry(
                    connectionState = ConnectionState.CONNECTED,
                    statusMessage = "Connected (Demo Simulator)",
                    rpm = rpm,
                    speed = speed,
                    speedUnit = "km/h",
                    coolantTemp = coolant,
                    coolantUnit = "°C",
                    fuelEconomy = fuelEcon,
                    ambientAirTemp = "$ambient C",
                    intakeAirTemp = "$intake C",
                    runTime = "${tick * 2}s",
                    dataMap = dataMap
                )

                delay(1000)
            }
        }
    }

    fun stop() {
        isSimulating = false
        simulationJob?.cancel()
        simulationJob = null
        _telemetry.value = ObdTelemetry(
            connectionState = ConnectionState.DISCONNECTED,
            statusMessage = "Simulator Disconnected"
        )
    }

    /**
     * Responds to raw ELM / OBD commands sent through Terminal or Diagnostic queries
     */
    fun respondToCommand(rawCommand: String): String {
        val clean = rawCommand.trim().uppercase(java.util.Locale.US)
        return when {
            clean == "ATZ" || clean == "AT Z" -> "ELM327 v1.5"
            clean == "ATE0" || clean == "AT E0" -> "OK"
            clean == "ATSP0" || clean == "AT SP 0" -> "OK"
            clean == "ATDP" || clean == "AT DP" -> "ISO 15765-4 (CAN 11/500)"
            clean == "ATRV" || clean == "AT RV" -> "14.2V"
            clean == "0100" -> "41 00 BE 3F B8 13"
            clean == "0101" -> "41 01 82 07 65 04" // MIL ON, 2 DTC codes
            clean == "0105" -> {
                val hex = String.format("%02X", (_telemetry.value.coolantTemp + 40).coerceIn(0, 255))
                "41 05 $hex"
            }
            clean == "010C" -> {
                val rawVal = _telemetry.value.rpm * 4
                val a = String.format("%02X", (rawVal shr 8) and 0xFF)
                val b = String.format("%02X", rawVal and 0xFF)
                "41 0C $a $b"
            }
            clean == "010D" -> {
                val hex = String.format("%02X", _telemetry.value.speed.coerceIn(0, 255))
                "41 0D $hex"
            }
            clean == "010F" -> "41 0F 4E" // ~38 C
            clean == "0110" -> "41 10 08 FE" // MAF
            clean == "011F" -> "41 1F 01 2C" // Run time 300s
            clean == "03" -> "43 01 71 03 00 00 00" // P0171 (System Too Lean Bank 1), P0300 (Random Misfire)
            clean == "04" -> "44" // Clear codes success
            clean == "07" -> "47 00 00 00 00 00 00" // No pending codes
            else -> "41 00 00 00"
        }
    }
}
