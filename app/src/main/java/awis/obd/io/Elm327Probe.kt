package awis.obd.io

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import awis.obd.command.ObdCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

data class ElmProbeStep(
    val title: String,
    val command: String,
    val rawResponse: String = "",
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class ElmProbeResult(
    val isSuccess: Boolean,
    val elmVersion: String = "",
    val protocolDescription: String = "",
    val voltage: String = "",
    val steps: List<ElmProbeStep> = emptyList(),
    val errorMessage: String? = null
)

object Elm327Probe {

    @SuppressLint("MissingPermission")
    suspend fun probeDevice(device: BluetoothDevice): ElmProbeResult = withContext(Dispatchers.IO) {
        val steps = mutableListOf<ElmProbeStep>()
        var socket: BluetoothSocket? = null
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            socket = device.createRfcommSocketToServiceRecord(ObdConnection.SPP_UUID)
            socket.connect()

            inputStream = socket.inputStream
            outputStream = socket.outputStream

            val inStream = inputStream ?: throw IllegalStateException("Input stream unavailable")
            val outStream = outputStream ?: throw IllegalStateException("Output stream unavailable")

            // Step 1: ATZ (Reset)
            val atzCmd = ObdCommand("ATZ", "Reset ELM327")
            val atzRes = atzCmd.execute(inStream, outStream)
            val atzClean = atzRes.replace(" ", "").trim()
            val atzOk = atzClean.contains("ELM327") || atzClean.contains("OK") || atzRes.isNotEmpty()
            steps.add(
                ElmProbeStep(
                    title = "Adapter Reset (ATZ)",
                    command = "ATZ",
                    rawResponse = atzRes,
                    isSuccess = atzOk
                )
            )
            delay(300)

            // Step 2: ATE0 (Echo Off)
            val ate0Cmd = ObdCommand("ATE0", "Echo Off")
            val ate0Res = ate0Cmd.execute(inStream, outStream)
            val ate0Clean = ate0Res.replace(" ", "").trim()
            val ate0Ok = ate0Clean.contains("OK") || ate0Res.isNotEmpty()
            steps.add(
                ElmProbeStep(
                    title = "Disable Echo (ATE0)",
                    command = "ATE0",
                    rawResponse = ate0Res,
                    isSuccess = ate0Ok
                )
            )
            delay(150)

            // Step 3: ATRV (Read Battery Voltage)
            val atrvCmd = ObdCommand("ATRV", "Read Voltage")
            val atrvRes = atrvCmd.execute(inStream, outStream)
            val voltage = atrvRes.trim()
            steps.add(
                ElmProbeStep(
                    title = "Battery Voltage (ATRV)",
                    command = "ATRV",
                    rawResponse = atrvRes,
                    isSuccess = voltage.contains("V") || voltage.isNotEmpty()
                )
            )
            delay(150)

            // Step 4: ATSP0 (Set Protocol Auto)
            val atsp0Cmd = ObdCommand("ATSP0", "Set Protocol Auto")
            val atsp0Res = atsp0Cmd.execute(inStream, outStream)
            val atsp0Clean = atsp0Res.replace(" ", "").trim()
            steps.add(
                ElmProbeStep(
                    title = "Auto-Detect Protocol (ATSP0)",
                    command = "ATSP0",
                    rawResponse = atsp0Res,
                    isSuccess = atsp0Clean.contains("OK") || atsp0Res.isNotEmpty()
                )
            )
            delay(150)

            // Step 5: ATDP (Describe Protocol)
            val atdpCmd = ObdCommand("ATDP", "Describe Protocol")
            val atdpRes = atdpCmd.execute(inStream, outStream)
            val protocol = atdpRes.trim()
            steps.add(
                ElmProbeStep(
                    title = "Protocol Identification (ATDP)",
                    command = "ATDP",
                    rawResponse = atdpRes,
                    isSuccess = protocol.isNotEmpty() && !protocol.contains("ERROR")
                )
            )

            ElmProbeResult(
                isSuccess = true,
                elmVersion = if (atzClean.isNotEmpty()) atzClean else "ELM327 Compatible",
                protocolDescription = if (protocol.isNotEmpty()) protocol else "AUTO (ISO 15765-4 CAN)",
                voltage = if (voltage.isNotEmpty()) voltage else "N/A",
                steps = steps
            )
        } catch (e: Exception) {
            steps.add(
                ElmProbeStep(
                    title = "Connection Verification",
                    command = "CONNECT",
                    rawResponse = "",
                    isSuccess = false,
                    error = e.localizedMessage ?: e.message
                )
            )
            ElmProbeResult(
                isSuccess = false,
                steps = steps,
                errorMessage = e.localizedMessage ?: e.message ?: "Failed to connect to adapter"
            )
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }
}
