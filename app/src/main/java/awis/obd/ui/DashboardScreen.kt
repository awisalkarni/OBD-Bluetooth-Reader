package awis.obd.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import awis.obd.io.ConnectionState
import awis.obd.io.ObdTelemetry

@Composable
fun DashboardScreen(
    telemetry: ObdTelemetry,
    selectedDeviceName: String?,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status & Connection Bar
        item {
            ConnectionBanner(
                telemetry = telemetry,
                deviceName = selectedDeviceName,
                onStartService = onStartService,
                onStopService = onStopService
            )
        }

        // Primary Gauges: Speed & RPM
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "SPEED",
                    value = "${telemetry.speed}",
                    unit = telemetry.speedUnit,
                    accentColor = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "RPM",
                    value = "${telemetry.rpm}",
                    unit = "RPM",
                    accentColor = Color(0xFFFF9100),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Coolant Gauge
        item {
            CoolantGauge(
                tempValue = telemetry.coolantTemp,
                tempUnit = telemetry.coolantUnit,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Secondary Stats: Fuel Economy & Air Temp
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "FUEL ECONOMY",
                    value = telemetry.fuelEconomy,
                    unit = "",
                    accentColor = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "RUNTIME",
                    value = telemetry.runTime,
                    unit = "",
                    accentColor = Color(0xFFAB47BC),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Additional Stats Row: Intake & Ambient Temp
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "INTAKE TEMP",
                    value = telemetry.intakeAirTemp,
                    unit = "",
                    accentColor = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "AMBIENT TEMP",
                    value = telemetry.ambientAirTemp,
                    unit = "",
                    accentColor = Color(0xFF26A69A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Data Table Header
        item {
            Text(
                text = "Live Vehicle Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        if (telemetry.dataMap.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (telemetry.connectionState == ConnectionState.CONNECTED)
                                "Reading live OBD-II data..."
                            else
                                "Connect to ELM327 adapter to view live parameters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            val sortedEntries = telemetry.dataMap.entries.sortedBy { it.key }
            items(sortedEntries) { (key, value) ->
                DataTableRow(paramName = key, paramValue = value)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ConnectionBanner(
    telemetry: ObdTelemetry,
    deviceName: String?,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusDotColor = when (telemetry.connectionState) {
                        ConnectionState.CONNECTED -> Color(0xFF00E676)
                        ConnectionState.CONNECTING, ConnectionState.INITIALIZING -> Color(0xFFFF9100)
                        ConnectionState.ERROR -> Color(0xFFFF5252)
                        ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
                    }
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = telemetry.statusMessage,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = deviceName ?: "No adapter selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (telemetry.connectionState == ConnectionState.CONNECTED ||
                telemetry.connectionState == ConnectionState.CONNECTING ||
                telemetry.connectionState == ConnectionState.INITIALIZING
            ) {
                OutlinedButton(
                    onClick = onStopService,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Stop")
                }
            } else {
                Button(
                    onClick = onStartService,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text("Start Live")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DataTableRow(paramName: String, paramValue: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = paramName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = paramValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        }
    }
}
