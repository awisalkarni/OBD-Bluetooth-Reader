package awis.obd.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import awis.obd.io.ConnectionState
import awis.obd.io.ObdTelemetry
import awis.obd.log.TripLogFile
import awis.obd.log.TripLogger
import awis.obd.log.TripSummary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripLogScreen(
    telemetry: ObdTelemetry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLogging by TripLogger.isLogging.collectAsState()
    val recordedPoints by TripLogger.currentSessionPoints.collectAsState()

    var savedLogs by remember { mutableStateOf<List<TripLogFile>>(emptyList()) }
    var tripSummaryDialog by remember { mutableStateOf<TripSummary?>(null) }
    var logToShare by remember { mutableStateOf<File?>(null) }

    fun refreshLogs() {
        scope.launch {
            savedLogs = TripLogger.listSavedLogs(context)
        }
    }

    LaunchedEffect(Unit) {
        refreshLogs()
    }

    fun shareFile(file: File, mimeType: String = "text/csv") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "OBD-II Trip Log: ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "Attached vehicle telemetry log from OBD-II Reader.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Trip Log"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Session Controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Trip Telemetry Recorder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLogging) "Recording active session..." else "Recorder idle",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLogging) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLogging) Color(0x334CAF50) else Color(0x22888888))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isLogging) "● REC" else "○ IDLE",
                                color = if (isLogging) Color(0xFF4CAF50) else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isLogging) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatMiniBox(title = "Points", value = "${recordedPoints.size}")
                            val currSpeed = recordedPoints.lastOrNull()?.speedKmh ?: telemetry.speed
                            StatMiniBox(title = "Speed", value = "$currSpeed km/h")
                            val maxSpd = recordedPoints.maxOfOrNull { it.speedKmh } ?: telemetry.speed
                            StatMiniBox(title = "Max Speed", value = "$maxSpd km/h")
                            val maxRpm = recordedPoints.maxOfOrNull { it.rpm } ?: telemetry.rpm
                            StatMiniBox(title = "Max RPM", value = "$maxRpm")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!isLogging) {
                            Button(
                                onClick = {
                                    TripLogger.startTrip(context)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Start New Trip")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val summary = TripLogger.stopTrip()
                                    tripSummaryDialog = summary
                                    refreshLogs()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                            ) {
                                Text("Stop & Save Trip")
                            }
                        }
                    }
                }
            }
        }

        // Saved Trip Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Trip Logs (${savedLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { refreshLogs() }) {
                    Text("Refresh")
                }
            }
        }

        if (savedLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved trip logs yet.\nStart recording a trip while driving or running simulator.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(savedLogs) { logFile ->
                SavedTripLogItem(
                    file = logFile,
                    onExportCsv = {
                        shareFile(File(logFile.filePath), "text/csv")
                    },
                    onExportJson = {
                        scope.launch {
                            val json = TripLogger.exportAsJson(context, File(logFile.filePath))
                            shareFile(json, "application/json")
                        }
                    },
                    onDelete = {
                        scope.launch {
                            TripLogger.deleteLog(logFile.filePath)
                            refreshLogs()
                        }
                    }
                )
            }
        }
    }

    // Trip Summary Modal Dialog
    tripSummaryDialog?.let { summary ->
        AlertDialog(
            onDismissRequest = { tripSummaryDialog = null },
            title = { Text("Trip Summary", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Session: ${summary.sessionId}", fontSize = 13.sp, color = Color.Gray)
                    val durationMin = ((summary.endTime - summary.startTime) / 60000.0)
                    Text("Duration: ${String.format(Locale.US, "%.1f", durationMin)} min")
                    Text("Data Points Recorded: ${summary.pointCount}")
                    Text("Est. Distance: ${String.format(Locale.US, "%.2f", summary.distanceKm)} km")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Max Speed: ${summary.maxSpeedKmh} km/h")
                    Text("Avg Speed: ${String.format(Locale.US, "%.1f", summary.avgSpeedKmh)} km/h")
                    Text("Max RPM: ${summary.maxRpm}")
                    Text("Avg RPM: ${String.format(Locale.US, "%.0f", summary.avgRpm)}")
                    Text("Peak Coolant Temp: ${summary.maxCoolantTempC}°C")
                }
            },
            confirmButton = {
                Button(onClick = { tripSummaryDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun StatMiniBox(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 11.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SavedTripLogItem(
    file: TripLogFile,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = file.fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${file.formattedDate} • ${file.sizeBytes / 1024} KB",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export CSV", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onExportJson,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export JSON", fontSize = 12.sp)
                }
                TextButton(
                    onClick = onDelete
                ) {
                    Text("Delete", color = Color(0xFFFF5252), fontSize = 12.sp)
                }
            }
        }
    }
}
