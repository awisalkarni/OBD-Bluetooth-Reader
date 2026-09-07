package awis.obd.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import awis.obd.config.ObdPreferences
import awis.obd.io.Elm327Probe
import awis.obd.io.ElmProbeResult
import kotlinx.coroutines.launch

data class DiscoveredDevice(
    val name: String,
    val address: String,
    val isBonded: Boolean,
    val device: BluetoothDevice
)

data class VehiclePreset(
    val title: String,
    val displacement: Double,
    val ve: Double,
    val description: String
)

val VEHICLE_PRESETS = listOf(
    VehiclePreset("Compact 4-Cyl (1.6L - 1.8L)", 1.6, 0.85, "Standard naturally aspirated sedan/hatchback"),
    VehiclePreset("Turbocharged 4-Cyl (2.0L)", 2.0, 0.92, "Modern turbo direct injection engine"),
    VehiclePreset("V6 Engine (3.0L - 3.5L)", 3.5, 0.82, "Mid-size sedan, crossover or SUV"),
    VehiclePreset("V8 Truck / Muscle (5.0L+)", 5.0, 0.80, "Large truck, pickup, or muscle car"),
    VehiclePreset("Subcompact / City Car (1.0L - 1.3L)", 1.2, 0.85, "Small city vehicle")
)

@Composable
fun SetupWizardScreen(
    prefs: ObdPreferences,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }

    // Step 1: Adapter Selection
    val devices = remember { mutableStateListOf<DiscoveredDevice>() }
    var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    // Step 2: Handshake Probe
    var probeResult by remember { mutableStateOf<ElmProbeResult?>(null) }
    var isProbing by remember { mutableStateOf(false) }

    // Step 3: Vehicle Profile & Preferences
    var vehicleName by remember { mutableStateOf(prefs.vehicleProfileName) }
    var engineDisplacement by remember { mutableStateOf(prefs.engineDisplacement.toString()) }
    var volumetricEfficiency by remember { mutableStateOf(prefs.volumetricEfficiency.toString()) }
    var isImperial by remember { mutableStateOf(prefs.isImperialUnits) }
    var isGps by remember { mutableStateOf(prefs.isGpsEnabled) }

    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter() }

    @SuppressLint("MissingPermission")
    fun refreshDevices() {
        devices.clear()
        bluetoothAdapter?.bondedDevices?.forEach { dev ->
            devices.add(
                DiscoveredDevice(
                    name = dev.name ?: "Unknown Adapter",
                    address = dev.address,
                    isBonded = true,
                    device = dev
                )
            )
        }
        // Auto-select if matches saved address
        if (selectedDevice == null && prefs.selectedDeviceAddress != null) {
            selectedDevice = devices.firstOrNull { it.address == prefs.selectedDeviceAddress }
        }
    }

    // Bluetooth discovery receiver for new devices
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        if (dev != null && devices.none { it.address == dev.address }) {
                            devices.add(
                                DiscoveredDevice(
                                    name = dev.name ?: "Unknown Device",
                                    address = dev.address,
                                    isBonded = dev.bondState == BluetoothDevice.BOND_BONDED,
                                    device = dev
                                )
                            )
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)
        refreshDevices()

        onDispose {
            try {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter.cancelDiscovery()
                }
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        isScanning = true
        bluetoothAdapter.startDiscovery()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Wizard Header with Step Indicator
        WizardHeader(
            currentStep = currentStep,
            onCancel = onCancel
        )

        HorizontalDivider(color = Color(0xFF2B303A))

        // Step Contents
        Box(modifier = Modifier.weight(1f)) {
            when (currentStep) {
                0 -> StepSelectAdapter(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    isScanning = isScanning,
                    onSelectDevice = { selectedDevice = it },
                    onScanClick = { startDiscovery() },
                    onRefreshClick = { refreshDevices() }
                )
                1 -> StepVerifyHandshake(
                    selectedDevice = selectedDevice,
                    probeResult = probeResult,
                    isProbing = isProbing,
                    onRunProbe = {
                        selectedDevice?.let { dev ->
                            isProbing = true
                            probeResult = null
                            scope.launch {
                                probeResult = Elm327Probe.probeDevice(dev.device)
                                isProbing = false
                            }
                        }
                    }
                )
                2 -> StepVehicleProfile(
                    vehicleName = vehicleName,
                    onVehicleNameChange = { vehicleName = it },
                    engineDisplacement = engineDisplacement,
                    onDisplacementChange = { engineDisplacement = it },
                    volumetricEfficiency = volumetricEfficiency,
                    onVeChange = { volumetricEfficiency = it },
                    isImperial = isImperial,
                    onImperialChange = { isImperial = it },
                    isGps = isGps,
                    onGpsChange = { isGps = it },
                    onSelectPreset = { preset ->
                        engineDisplacement = preset.displacement.toString()
                        volumetricEfficiency = preset.ve.toString()
                    }
                )
            }
        }

        // Navigation Footer (Back / Next / Finish)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 0) {
                OutlinedButton(onClick = { currentStep -= 1 }) {
                    Text("Back")
                }
            } else {
                OutlinedButton(onClick = onCancel) {
                    Text("Skip Wizard")
                }
            }

            if (currentStep < 2) {
                Button(
                    onClick = {
                        if (currentStep == 0) {
                            // Auto-trigger probe when moving to step 1
                            currentStep = 1
                            selectedDevice?.let { dev ->
                                isProbing = true
                                probeResult = null
                                scope.launch {
                                    probeResult = Elm327Probe.probeDevice(dev.device)
                                    isProbing = false
                                }
                            }
                        } else {
                            currentStep += 1
                        }
                    },
                    enabled = (currentStep == 0 && selectedDevice != null) || (currentStep == 1),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    Text(if (currentStep == 0) "Next: Test Adapter" else "Next: Vehicle Setup")
                }
            } else {
                Button(
                    onClick = {
                        // Save all configured preferences
                        selectedDevice?.let { dev ->
                            prefs.selectedDeviceAddress = dev.address
                            prefs.selectedDeviceName = dev.name
                        }
                        prefs.vehicleProfileName = vehicleName
                        engineDisplacement.toDoubleOrNull()?.let { prefs.engineDisplacement = it }
                        volumetricEfficiency.toDoubleOrNull()?.let { prefs.volumetricEfficiency = it }
                        prefs.isImperialUnits = isImperial
                        prefs.isGpsEnabled = isGps
                        prefs.isWizardCompleted = true
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black)
                ) {
                    Text("Complete Setup & Start")
                }
            }
        }
    }
}

@Composable
fun WizardHeader(
    currentStep: Int,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ELM327 Setup Wizard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Step ${currentStep + 1} of 3",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF00E5FF)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val stepTitles = listOf("1. Adapter", "2. Handshake", "3. Vehicle")
            stepTitles.forEachIndexed { index, title ->
                val isActive = index == currentStep
                val isDone = index < currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when {
                                isDone -> Color(0xFF00E676)
                                isActive -> Color(0xFF00E5FF)
                                else -> Color(0xFF2B303A)
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun StepSelectAdapter(
    devices: List<DiscoveredDevice>,
    selectedDevice: DiscoveredDevice?,
    isScanning: Boolean,
    onSelectDevice: (DiscoveredDevice) -> Unit,
    onScanClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select OBD-II Adapter",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Turn your vehicle's ignition ON and plug in your ELM327 Bluetooth adapter.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onScanClick,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black),
                modifier = Modifier.weight(1f)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 6.dp)
                    )
                    Text("Scanning...")
                } else {
                    Text("Scan Nearby")
                }
            }

            OutlinedButton(onClick = onRefreshClick) {
                Text("Refresh Paired")
            }
        }

        if (devices.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No Bluetooth devices found", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pair your ELM327 adapter in Android Settings or tap 'Scan Nearby'. PIN is usually 1234 or 0000.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { dev ->
                    val isSelected = dev.address == selectedDevice?.address
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E3A4A) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDevice(dev) }
                            .then(
                                if (isSelected) Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(10.dp))
                                else Modifier
                            )
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
                                    Text(
                                        text = dev.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (dev.isBonded) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2B303A))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Paired", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                        }
                                    }
                                }
                                Text(
                                    text = dev.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Text("✓ Selected", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepVerifyHandshake(
    selectedDevice: DiscoveredDevice?,
    probeResult: ElmProbeResult?,
    isProbing: Boolean,
    onRunProbe: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Adapter Handshake & Diagnostics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Testing ELM327 protocol compatibility with ${selectedDevice?.name ?: "selected adapter"}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isProbing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sending initialization sequence (ATZ, ATE0, ATRV, ATSP0)...")
                }
            }
        } else if (probeResult != null) {
            val result = probeResult
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (result.isSuccess) Color(0xFF14291E) else Color(0xFF331919)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (result.isSuccess) "✓ Handshake Successful" else "✗ Connection Issue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSuccess) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                    }

                    if (result.isSuccess) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Adapter Firmware:", color = Color.LightGray)
                            Text(result.elmVersion, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("OBD Protocol:", color = Color.LightGray)
                            Text(result.protocolDescription, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Battery Voltage:", color = Color.LightGray)
                            Text(result.voltage, fontWeight = FontWeight.Bold, color = Color(0xFFFF9100))
                        }
                    } else {
                        Text(
                            text = result.errorMessage ?: "Unknown adapter response",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Steps log
            Text("Diagnostic Command Log:", style = MaterialTheme.typography.labelLarge)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(result.steps) { step ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = step.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (step.rawResponse.isNotBlank()) {
                                    Text(
                                        text = "Response: ${step.rawResponse.trim()}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7EE787)
                                    )
                                }
                            }
                            Text(
                                text = if (step.isSuccess) "PASS" else "FAIL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (step.isSuccess) Color(0xFF00E676) else Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onRunProbe,
            enabled = !isProbing && selectedDevice != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-test Adapter")
        }
    }
}

@Composable
fun StepVehicleProfile(
    vehicleName: String,
    onVehicleNameChange: (String) -> Unit,
    engineDisplacement: String,
    onDisplacementChange: (String) -> Unit,
    volumetricEfficiency: String,
    onVeChange: (String) -> Unit,
    isImperial: Boolean,
    onImperialChange: (Boolean) -> Unit,
    isGps: Boolean,
    onGpsChange: (Boolean) -> Unit,
    onSelectPreset: (VehiclePreset) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Vehicle Profile & Presets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Used to calculate accurate real-time fuel consumption without a MAF sensor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Presets
        item {
            Text("Quick Presets:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                VEHICLE_PRESETS.forEach { preset ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPreset(preset) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(preset.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(preset.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${preset.displacement}L", fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }

        // Custom Inputs
        item {
            OutlinedTextField(
                value = vehicleName,
                onValueChange = onVehicleNameChange,
                label = { Text("Vehicle Profile Name") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = engineDisplacement,
                    onValueChange = onDisplacementChange,
                    label = { Text("Displacement (L)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = volumetricEfficiency,
                    onValueChange = onVeChange,
                    label = { Text("VE Ratio (e.g. 0.85)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Imperial Units", fontWeight = FontWeight.SemiBold)
                            Text("Use mph, °F, and mpg", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isImperial, onCheckedChange = onImperialChange)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable GPS Logging", fontWeight = FontWeight.SemiBold)
                            Text("Log GPS coordinates with trip data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isGps, onCheckedChange = onGpsChange)
                    }
                }
            }
        }
    }
}
