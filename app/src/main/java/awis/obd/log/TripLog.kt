package awis.obd.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Individual data point recorded during an OBD trip session.
 */
data class TripDataPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempC: Int = 0,
    val fuelEconomy: String = "--",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

/**
 * Summary stats for a completed or ongoing trip session.
 */
data class TripSummary(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long,
    val pointCount: Int,
    val maxSpeedKmh: Int,
    val avgSpeedKmh: Double,
    val maxRpm: Int,
    val avgRpm: Double,
    val maxCoolantTempC: Int,
    val distanceKm: Double
)

/**
 * Metadata for a trip log file stored on disk.
 */
data class TripLogFile(
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val formattedDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
)
