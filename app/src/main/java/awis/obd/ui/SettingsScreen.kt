package awis.obd.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import awis.obd.config.ObdConfig
import awis.obd.config.ObdPreferences

data class PairedDevice(val name: String, val address: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: ObdPreferences,
    onSettingsChanged: () -> Unit,
    onLaunchWizard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isImperial by remember { mutableStateOf(prefs.isImperialUnits) }
    var isGps by remember { mutableStateOf(prefs.isGpsEnabled) }
    var updatePeriod by remember { mutableStateOf(prefs.updatePeriodSeconds.toString()) }
    var engineDisplacement by remember { mutableStateOf(prefs.engineDisplacement.toString()) }
    var volumetricEfficiency by remember { mutableStateOf(prefs.volumetricEfficiency.toString()) }
    var maxFuelEconomy by remember { mutableStateOf(prefs.maxFuelEconomy.toString()) }
    var readerConfig by remember { mutableStateOf(prefs.readerConfigCommands) }

    var selectedDeviceAddress by remember { mutableStateOf(prefs.selectedDeviceAddress) }
    var selectedDeviceName by remember { mutableStateOf(prefs.selectedDeviceName) }

    val pairedDevices = remember { mutableStateListOf<PairedDevice>() }
    var isDeviceDropdownExpanded by remember { mutableStateOf(false) }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        pairedDevices.clear()
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.forEach { device ->
                pairedDevices.add(PairedDevice(name = device.name ?: "Unknown Device", address = device.address))
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        refreshPairedDevices()
    }

    val availableCommands = remember { ObdConfig.getCommands() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(onClick = onLaunchWizard) {
                    Text("Run Setup Wizard")
                }
            }
        }

        // Bluetooth Device Picker Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "OBD-II Bluetooth Adapter",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Select your paired ELM327 Bluetooth adapter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = isDeviceDropdownExpanded,
                        onExpandedChange = { isDeviceDropdownExpanded = !isDeviceDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (!selectedDeviceAddress.isNullOrBlank())
                                "${selectedDeviceName ?: "Device"} ($selectedDeviceAddress)"
                            else
                                "Choose paired adapter...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDeviceDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = isDeviceDropdownExpanded,
                            onDismissRequest = { isDeviceDropdownExpanded = false }
                        ) {
                            if (pairedDevices.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No paired devices found (pair in Android settings first)") },
                                    onClick = { isDeviceDropdownExpanded = false }
                                )
                            } else {
                                pairedDevices.forEach { dev ->
                                    DropdownMenuItem(
                                        text = { Text("${dev.name} (${dev.address})") },
                                        onClick = {
                                            selectedDeviceAddress = dev.address
                                            selectedDeviceName = dev.name
                                            prefs.selectedDeviceAddress = dev.address
                                            prefs.selectedDeviceName = dev.name
                                            isDeviceDropdownExpanded = false
                                            onSettingsChanged()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // General Settings Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "General Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Imperial Units", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Show mph, °F, and mpg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isImperial,
                            onCheckedChange = {
                                isImperial = it
                                prefs.isImperialUnits = it
                                onSettingsChanged()
                            }
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable GPS", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Log GPS coordinates and speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isGps,
                            onCheckedChange = {
                                isGps = it
                                prefs.isGpsEnabled = it
                                onSettingsChanged()
                            }
                        )
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = updatePeriod,
                        onValueChange = {
                            updatePeriod = it
                            it.toIntOrNull()?.let { period ->
                                prefs.updatePeriodSeconds = period
                                onSettingsChanged()
                            }
                        },
                        label = { Text("Update Period (seconds)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = engineDisplacement,
                        onValueChange = {
                            engineDisplacement = it
                            it.toDoubleOrNull()?.let { ed ->
                                prefs.engineDisplacement = ed
                                onSettingsChanged()
                            }
                        },
                        label = { Text("Engine Displacement (Liters)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = volumetricEfficiency,
                        onValueChange = {
                            volumetricEfficiency = it
                            it.toDoubleOrNull()?.let { ve ->
                                prefs.volumetricEfficiency = ve
                                onSettingsChanged()
                            }
                        },
                        label = { Text("Volumetric Efficiency (e.g. 0.85)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = maxFuelEconomy,
                        onValueChange = {
                            maxFuelEconomy = it
                            it.toDoubleOrNull()?.let { max ->
                                prefs.maxFuelEconomy = max
                                onSettingsChanged()
                            }
                        },
                        label = { Text("Max Fuel Economy Threshold") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = readerConfig,
                        onValueChange = {
                            readerConfig = it
                            prefs.readerConfigCommands = it
                            onSettingsChanged()
                        },
                        label = { Text("ELM327 Init Commands (separated by new line)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Active Commands Selection
        item {
            Text(
                text = "Live Queried PIDs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(availableCommands) { cmd ->
            var isEnabled by remember(cmd.desc) { mutableStateOf(prefs.isCommandEnabled(cmd.desc)) }
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cmd.desc, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        cmd.cmd?.let {
                            Text("PID: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Checkbox(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            isEnabled = checked
                            prefs.setCommandEnabled(cmd.desc, checked)
                            onSettingsChanged()
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
