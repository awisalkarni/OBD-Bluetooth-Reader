package awis.obd.log

import android.content.Context
import android.util.Log
import awis.obd.io.ObdTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Singleton service responsible for logging real-time OBD telemetry to CSV/JSON files,
 * computing live trip statistics, and providing export capabilities.
 */
object TripLogger {
    private const val TAG = "TripLogger"
    private const val LOG_DIR_NAME = "trip_logs"

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _currentSessionPoints = MutableStateFlow<List<TripDataPoint>>(emptyList())
    val currentSessionPoints: StateFlow<List<TripDataPoint>> = _currentSessionPoints.asStateFlow()

    private var currentSessionId: String = ""
    private var sessionStartTime: Long = 0L
    private var currentCsvFile: File? = null
    private var csvWriter: FileWriter? = null

    fun getLogDirectory(context: Context): File {
        val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, LOG_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @Synchronized
    fun startTrip(context: Context): String {
        if (_isLogging.value) return currentSessionId

        sessionStartTime = System.currentTimeMillis()
        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(sessionStartTime))
        currentSessionId = "trip_$timeStr"

        try {
            val logDir = getLogDirectory(context)
            val file = File(logDir, "${currentSessionId}.csv")
            currentCsvFile = file
            csvWriter = FileWriter(file, true)
            // Write CSV Header
            csvWriter?.append("Timestamp,TimeISO,RPM,Speed_kmh,CoolantTemp_C,FuelEconomy,Latitude,Longitude,Altitude\n")
            csvWriter?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize CSV trip log file", e)
        }

        _currentSessionPoints.value = emptyList()
        _isLogging.value = true
        return currentSessionId
    }

    @Synchronized
    fun recordPoint(
        telemetry: ObdTelemetry,
        latitude: Double? = null,
        longitude: Double? = null,
        altitude: Double? = null
    ) {
        if (!_isLogging.value) return

        val now = System.currentTimeMillis()
        val point = TripDataPoint(
            timestamp = now,
            rpm = telemetry.rpm,
            speedKmh = telemetry.speed,
            coolantTempC = telemetry.coolantTemp,
            fuelEconomy = telemetry.fuelEconomy,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )

        val updated = _currentSessionPoints.value.toMutableList()
        updated.add(point)
        _currentSessionPoints.value = updated

        // Append to file asynchronously or flush
        try {
            val timeIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(Date(now))
            val cleanFuel = telemetry.fuelEconomy.replace(",", "")
            val latStr = latitude?.toString() ?: ""
            val lonStr = longitude?.toString() ?: ""
            val altStr = altitude?.toString() ?: ""

            csvWriter?.append("$now,$timeIso,${telemetry.rpm},${telemetry.speed},${telemetry.coolantTemp},$cleanFuel,$latStr,$lonStr,$altStr\n")
            csvWriter?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing point to CSV", e)
        }
    }

    @Synchronized
    fun stopTrip(): TripSummary? {
        if (!_isLogging.value) return null

        try {
            csvWriter?.flush()
            csvWriter?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing CSV writer", e)
        }
        csvWriter = null

        val endTime = System.currentTimeMillis()
        val points = _currentSessionPoints.value
        _isLogging.value = false

        if (points.isEmpty()) return null

        val maxSpeed = points.maxOfOrNull { it.speedKmh } ?: 0
        val avgSpeed = if (points.isNotEmpty()) points.map { it.speedKmh }.average() else 0.0
        val maxRpm = points.maxOfOrNull { it.rpm } ?: 0
        val avgRpm = if (points.isNotEmpty()) points.map { it.rpm }.average() else 0.0
        val maxCoolant = points.maxOfOrNull { it.coolantTempC } ?: 0

        // Distance approx based on speeds and delta times
        var distanceKm = 0.0
        for (i in 1 until points.size) {
            val deltaHours = (points[i].timestamp - points[i - 1].timestamp) / 3600000.0
            val speed = points[i].speedKmh
            distanceKm += speed * deltaHours
        }

        return TripSummary(
            sessionId = currentSessionId,
            startTime = sessionStartTime,
            endTime = endTime,
            pointCount = points.size,
            maxSpeedKmh = maxSpeed,
            avgSpeedKmh = avgSpeed,
            maxRpm = maxRpm,
            avgRpm = avgRpm,
            maxCoolantTempC = maxCoolant,
            distanceKm = distanceKm
        )
    }

    suspend fun listSavedLogs(context: Context): List<TripLogFile> = withContext(Dispatchers.IO) {
        val dir = getLogDirectory(context)
        val files = dir.listFiles { f -> f.isFile && (f.extension == "csv" || f.extension == "json") } ?: emptyArray()
        files.map { file ->
            TripLogFile(
                fileName = file.name,
                filePath = file.absolutePath,
                timestamp = file.lastModified(),
                sizeBytes = file.length()
            )
        }.sortedByDescending { it.timestamp }
    }

    suspend fun deleteLog(filePath: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (file.exists()) file.delete() else false
    }

    suspend fun exportAsJson(context: Context, csvFile: File): File = withContext(Dispatchers.IO) {
        val jsonFile = File(getLogDirectory(context), "${csvFile.nameWithoutExtension}.json")
        val lines = csvFile.readLines()
        if (lines.isEmpty()) {
            jsonFile.writeText("[]")
            return@withContext jsonFile
        }

        val jsonBuilder = StringBuilder("[\n")
        val dataLines = lines.drop(1)
        dataLines.forEachIndexed { index, line ->
            val parts = line.split(",")
            if (parts.size >= 6) {
                jsonBuilder.append("  {\n")
                jsonBuilder.append("    \"timestamp\": ${parts[0]},\n")
                jsonBuilder.append("    \"timeIso\": \"${parts[1]}\",\n")
                jsonBuilder.append("    \"rpm\": ${parts.getOrNull(2)?.toIntOrNull() ?: 0},\n")
                jsonBuilder.append("    \"speedKmh\": ${parts.getOrNull(3)?.toIntOrNull() ?: 0},\n")
                jsonBuilder.append("    \"coolantTempC\": ${parts.getOrNull(4)?.toIntOrNull() ?: 0},\n")
                jsonBuilder.append("    \"fuelEconomy\": \"${parts.getOrNull(5) ?: "--"}\",\n")
                jsonBuilder.append("    \"latitude\": ${parts.getOrNull(6)?.toDoubleOrNull() ?: "null"},\n")
                jsonBuilder.append("    \"longitude\": ${parts.getOrNull(7)?.toDoubleOrNull() ?: "null"},\n")
                jsonBuilder.append("    \"altitude\": ${parts.getOrNull(8)?.toDoubleOrNull() ?: "null"}\n")
                jsonBuilder.append("  }${if (index < dataLines.size - 1) "," else ""}\n")
            }
        }
        jsonBuilder.append("]\n")
        jsonFile.writeText(jsonBuilder.toString())
        jsonFile
    }
}
