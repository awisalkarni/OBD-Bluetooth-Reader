package awis.obd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import awis.obd.command.DtcDatabase
import awis.obd.command.DtcInfo
import awis.obd.command.DtcNumberObdCommand
import awis.obd.command.DtcSeverity
import awis.obd.command.ObdCommand
import awis.obd.command.TroubleCodesObdCommand
import kotlinx.coroutines.launch

data class ScanResultState(
    val milOn: Boolean = false,
    val codeCount: Int = 0,
    val codes: List<DtcInfo> = emptyList(),
    val rawTroubleOutput: String = "",
    val hasScanned: Boolean = false
)

@Composable
fun DtcScannerScreen(
    onRunCommand: suspend (ObdCommand) -> String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf(ScanResultState()) }
    var selectedDtcDetail by remember { mutableStateOf<DtcInfo?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun runDtcScan() {
        if (isScanning) return
        isScanning = true
        statusMessage = null

        scope.launch {
            try {
                // Step 1: Check DTC Status (Mode 0101)
                val statusCmd = DtcNumberObdCommand()
                onRunCommand(statusCmd)

                // Step 2: Query Trouble Codes (Mode 03)
                val tcCmd = TroubleCodesObdCommand()
                val rawResult = onRunCommand(tcCmd)

                // Map parsed codes to database info
                val detectedCodes = tcCmd.parsedCodeList.map { code ->
                    DtcDatabase.lookup(code)
                }

                scanResult = ScanResultState(
                    milOn = statusCmd.milOn,
                    codeCount = statusCmd.codeCount.coerceAtLeast(detectedCodes.size),
                    codes = detectedCodes,
                    rawTroubleOutput = rawResult,
                    hasScanned = true
                )

                statusMessage = if (detectedCodes.isEmpty() && !statusCmd.milOn) {
                    "✓ System Clean: No diagnostic trouble codes detected."
                } else {
                    "Scan Complete: Found ${detectedCodes.size} trouble code(s)."
                }
            } catch (e: Exception) {
                statusMessage = "Scan Error: ${e.localizedMessage ?: e.message}"
            } finally {
                isScanning = false
            }
        }
    }

    fun clearTroubleCodes() {
        showResetConfirmDialog = false
        isResetting = true
        scope.launch {
            try {
                // Mode 04: Clear diagnostic trouble codes
                val resetCmd = ObdCommand("04", "Clear DTC Codes")
                val resetResult = onRunCommand(resetCmd)

                statusMessage = "Codes Cleared: $resetResult. Re-scanning..."
                runDtcScan()
            } catch (e: Exception) {
                statusMessage = "Failed to clear codes: ${e.localizedMessage ?: e.message}"
            } finally {
                isResetting = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title & Description
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DTC Diagnostics & Faults",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Scan Mode 03 & Clear Mode 04 Faults",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Bar (Scan & Clear Buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { runDtcScan() },
                enabled = !isScanning && !isResetting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.weight(1.2f)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...")
                } else {
                    Text("Scan for Faults")
                }
            }

            OutlinedButton(
                onClick = { showResetConfirmDialog = true },
                enabled = !isScanning && !isResetting && scanResult.hasScanned && (scanResult.codeCount > 0 || scanResult.milOn),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                modifier = Modifier.weight(1f)
            ) {
                if (isResetting) {
                    CircularProgressIndicator(color = Color(0xFFFF5252), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Clear Codes")
                }
            }
        }

        // Status Card
        if (scanResult.hasScanned) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (scanResult.milOn || scanResult.codes.isNotEmpty()) Color(0xFF2B1B1B) else Color(0xFF14291E)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (scanResult.milOn) Color(0xFFFF5252) else Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (scanResult.milOn) "Check Engine Light (MIL) is ON" else "MIL Indicator is OFF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (scanResult.milOn) Color(0xFFFF5252) else Color(0xFF00E676)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${scanResult.codeCount} diagnostic trouble code(s) stored in ECU",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2228))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${scanResult.codes.size} PIDs",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            Text(
                text = msg,
                fontSize = 12.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // List of DTC Faults
        if (!scanResult.hasScanned) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Engine Diagnostics Ready",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Scan for Faults' to query ECU stored trouble codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (scanResult.codes.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Fault Codes Detected",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The vehicle ECU reports no active Diagnostic Trouble Codes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scanResult.codes) { dtc ->
                    DtcCard(
                        dtc = dtc,
                        onClick = { selectedDtcDetail = dtc }
                    )
                }
            }
        }
    }

    // Modal: DTC Details Dialog
    selectedDtcDetail?.let { dtc ->
        AlertDialog(
            onDismissRequest = { selectedDtcDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dtc.code, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    DtcSeverityBadge(severity = dtc.severity)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dtc.title, fontWeight = FontWeight.Bold)
                    Text("System: ${dtc.system}", fontSize = 12.sp, color = Color.Gray)
                    HorizontalDivider()
                    if (dtc.symptoms.isNotBlank()) {
                        Text("Symptoms:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(dtc.symptoms, fontSize = 12.sp, color = Color.LightGray)
                    }
                    if (dtc.possibleCauses.isNotEmpty()) {
                        Text("Common Causes:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        dtc.possibleCauses.forEach { cause ->
                            Text("• $cause", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDtcDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal: Confirmation for Clearing Codes
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text("Clear Trouble Codes (Mode 04)?", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Clearing fault codes will turn off the Check Engine Light and reset all OBD-II emissions readiness monitors.",
                        fontSize = 13.sp
                    )
                    Text(
                        "Note: If the underlying mechanical fault is not repaired, the code and light will return after driving cycles.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { clearTroubleCodes() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White)
                ) {
                    Text("Yes, Clear Codes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DtcCard(
    dtc: DtcInfo,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dtc.code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                DtcSeverityBadge(severity = dtc.severity)
            }

            Text(
                text = dtc.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dtc.system,
                    fontSize = 11.sp,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "Details →",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun DtcSeverityBadge(severity: DtcSeverity) {
    val (label, bg, fg) = when (severity) {
        DtcSeverity.CRITICAL -> Triple("CRITICAL", Color(0xFF421515), Color(0xFFFF5252))
        DtcSeverity.WARNING -> Triple("WARNING", Color(0xFF423015), Color(0xFFFF9100))
        DtcSeverity.INFO -> Triple("ADVISORY", Color(0xFF153342), Color(0xFF00E5FF))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}
