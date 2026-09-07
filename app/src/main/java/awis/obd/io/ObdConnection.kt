package awis.obd.io

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import awis.obd.command.AirIntakeTempObdCommand
import awis.obd.command.EngineRPMObdCommand
import awis.obd.command.FuelEconomyMAPObdCommand
import awis.obd.command.FuelEconomyObdCommand
import awis.obd.command.IntakeManifoldPressureObdCommand
import awis.obd.command.MassAirFlowObdCommand
import awis.obd.command.ObdCommand
import awis.obd.command.SpeedObdCommand
import awis.obd.config.ObdConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    INITIALIZING,
    CONNECTED,
    ERROR
}

data class ObdTelemetry(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val statusMessage: String = "Disconnected",
    val rpm: Int = 0,
    val speed: Int = 0,
    val speedUnit: String = "km/h",
    val coolantTemp: Int = 0,
    val coolantUnit: String = "C",
    val fuelEconomy: String = "--",
    val ambientAirTemp: String = "--",
    val intakeAirTemp: String = "--",
    val runTime: String = "--",
    val dataMap: Map<String, String> = emptyMap()
)

class ObdConnection(
    private val device: BluetoothDevice,
    private val commands: List<ObdCommand>,
    private val updatePeriodMs: Long = 2000L,
    private val isImperial: Boolean = false,
    private val volumetricEfficiency: Double = 0.85,
    private val engineDisplacement: Double = 1.6,
    private val initCommands: List<String> = listOf("ATZ", "ATE0", "ATSP0")
) {
    companion object {
        private const val TAG = "ObdConnection"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _telemetry = MutableStateFlow(ObdTelemetry(ConnectionState.DISCONNECTED))
    val telemetry: StateFlow<ObdTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var isRunning = false

    @SuppressLint("MissingPermission")
    suspend fun start() = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        isRunning = true

        _telemetry.value = _telemetry.value.copy(
            connectionState = ConnectionState.CONNECTING,
            statusMessage = "Connecting to ${device.name ?: device.address}..."
        )

        try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            _telemetry.value = _telemetry.value.copy(
                connectionState = ConnectionState.INITIALIZING,
                statusMessage = "Initializing ELM327 adapter..."
            )

            val inStream = inputStream ?: throw IllegalStateException("Input stream is null")
            val outStream = outputStream ?: throw IllegalStateException("Output stream is null")

            // Run initialization AT commands
            for (cmd in initCommands) {
                if (!isActive || !isRunning) break
                val atCmd = ObdCommand(cmd, "Init $cmd")
                atCmd.execute(inStream, outStream)
                kotlinx.coroutines.delay(200)
            }

            _telemetry.value = _telemetry.value.copy(
                connectionState = ConnectionState.CONNECTED,
                statusMessage = "Connected"
            )

            val currentData = mutableMapOf<String, String>()

            // Keep track of values for fuel economy calculations
            var lastMaf = -9999.0
            var lastSpeed = -9999.0
            var lastRpm = -9999.0
            var lastMap = -9999.0
            var lastIntakeTemp = -9999.0

            while (isActive && isRunning) {
                for (cmd in commands) {
                    if (!isActive || !isRunning) break
                    val activeCmd = cmd.copyCommand()
                    activeCmd.isImperial = isImperial

                    val result = try {
                        activeCmd.execute(inStream, outStream)
                    } catch (e: Exception) {
                        Log.e(TAG, "Command ${cmd.desc} failed", e)
                        "--"
                    }

                    currentData[cmd.desc] = result

                    when (activeCmd) {
                        is EngineRPMObdCommand -> {
                            if (activeCmd.intValue >= 0) {
                                lastRpm = activeCmd.intValue.toDouble()
                            }
                        }
                        is SpeedObdCommand -> {
                            if (activeCmd.intValue >= 0) {
                                lastSpeed = activeCmd.intValue.toDouble()
                            }
                        }
                        is MassAirFlowObdCommand -> {
                            if (activeCmd.maf > 0) {
                                lastMaf = activeCmd.maf
                            }
                        }
                        is IntakeManifoldPressureObdCommand -> {
                            if (activeCmd.intValue > 0) {
                                lastMap = activeCmd.intValue.toDouble()
                            }
                        }
                        is AirIntakeTempObdCommand -> {
                            if (activeCmd.intValue > -40) {
                                lastIntakeTemp = activeCmd.intValue.toDouble()
                            }
                        }
                        is FuelEconomyObdCommand -> {
                            if (lastMaf > 0 && lastSpeed >= 0) {
                                activeCmd.calculateFromMafAndSpeed(lastMaf, lastSpeed)
                                currentData[cmd.desc] = activeCmd.formatResult()
                            }
                        }
                        is FuelEconomyMAPObdCommand -> {
                            if (lastRpm > 0 && lastMap > 0 && lastSpeed >= 0 && lastIntakeTemp > -40) {
                                activeCmd.calculateFromMap(
                                    rpm = lastRpm,
                                    mapKpa = lastMap,
                                    tempC = lastIntakeTemp,
                                    speedKmH = lastSpeed,
                                    ve = volumetricEfficiency,
                                    edLiters = engineDisplacement
                                )
                                currentData[cmd.desc] = activeCmd.formatResult()
                            }
                        }
                    }

                    // Update UI telemetry state
                    val rpmVal = lastRpm.toInt().coerceAtLeast(0)
                    val speedVal = if (isImperial) (lastSpeed * 0.625).toInt() else lastSpeed.toInt().coerceAtLeast(0)
                    val coolantStr = currentData[ObdConfig.COOLANT_TEMP] ?: ""
                    val coolantVal = coolantStr.split(" ").firstOrNull()?.toIntOrNull() ?: 0
                    val fuelStr = currentData[ObdConfig.FUEL_ECON] ?: currentData[ObdConfig.FUEL_ECON_MAP] ?: "--"

                    _telemetry.value = _telemetry.value.copy(
                        connectionState = ConnectionState.CONNECTED,
                        statusMessage = "Connected",
                        rpm = rpmVal,
                        speed = speedVal,
                        speedUnit = if (isImperial) "mph" else "km/h",
                        coolantTemp = coolantVal,
                        coolantUnit = if (isImperial) "°F" else "°C",
                        fuelEconomy = fuelStr,
                        ambientAirTemp = currentData[ObdConfig.AIR_TEMP] ?: "--",
                        intakeAirTemp = currentData[ObdConfig.INTAKE_TEMP] ?: "--",
                        runTime = currentData[ObdConfig.RUN_TIME] ?: "--",
                        dataMap = currentData.toMap()
                    )

                    kotlinx.coroutines.delay(100)
                }

                kotlinx.coroutines.delay(updatePeriodMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "OBD connection error", e)
            _telemetry.value = _telemetry.value.copy(
                connectionState = ConnectionState.ERROR,
                statusMessage = "Error: ${e.localizedMessage ?: e.message}"
            )
        } finally {
            close()
        }
    }

    fun stop() {
        isRunning = false
        close()
        _telemetry.value = _telemetry.value.copy(
            connectionState = ConnectionState.DISCONNECTED,
            statusMessage = "Disconnected"
        )
    }

    private fun close() {
        try {
            inputStream?.close()
        } catch (_: Exception) {}
        try {
            outputStream?.close()
        } catch (_: Exception) {}
        try {
            socket?.close()
        } catch (_: Exception) {}
        inputStream = null
        outputStream = null
        socket = null
    }

    suspend fun executeSingleCommand(command: ObdCommand): String = withContext(Dispatchers.IO) {
        val inStream = inputStream ?: throw IllegalStateException("Not connected")
        val outStream = outputStream ?: throw IllegalStateException("Not connected")
        command.isImperial = isImperial
        command.execute(inStream, outStream)
    }
}
