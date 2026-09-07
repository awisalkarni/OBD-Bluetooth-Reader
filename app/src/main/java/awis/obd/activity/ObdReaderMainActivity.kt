package awis.obd.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import awis.obd.command.ObdCommand
import awis.obd.config.ObdPreferences
import awis.obd.io.ConnectionState
import awis.obd.io.ObdConnection
import awis.obd.io.ObdReaderService
import awis.obd.ui.CommandScreen
import awis.obd.ui.DashboardScreen
import awis.obd.ui.DtcScannerScreen
import awis.obd.ui.ObdReaderTheme
import awis.obd.ui.SettingsScreen
import awis.obd.ui.SetupWizardScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val title: String, val badge: String) {
    data object Dashboard : Screen("Dashboard", "📊")
    data object Diagnostics : Screen("Faults", "⚠️")
    data object Terminal : Screen("Terminal", "💻")
    data object Settings : Screen("Settings", "⚙️")
}

class ObdReaderMainActivity : ComponentActivity() {

    private lateinit var prefs: ObdPreferences
    private val snackbarHostState = SnackbarHostState()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(
                this,
                "Bluetooth and Location permissions are required to connect to OBD adapter",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = ObdPreferences(this)
        checkAndRequestPermissions()

        setContent {
            ObdReaderTheme {
                MainApp()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startObdService() {
        if (prefs.selectedDeviceAddress.isNullOrBlank()) {
            Toast.makeText(this, "Please select an OBD adapter in Settings first", Toast.LENGTH_LONG).show()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val intent = Intent(this, ObdReaderService::class.java).apply {
            action = ObdReaderService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopObdService() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val intent = Intent(this, ObdReaderService::class.java).apply {
            action = ObdReaderService.ACTION_STOP
        }
        startService(intent)
    }

    @SuppressLint("MissingPermission")
    private suspend fun runSingleCommandDirectly(command: ObdCommand): String = withContext(Dispatchers.IO) {
        val deviceAddress = prefs.selectedDeviceAddress
            ?: return@withContext "Error: No Bluetooth OBD adapter selected in Settings"

        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext "Error: Bluetooth is unavailable"

        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            return@withContext "Error resolving device: ${e.message}"
        }

        val socket = device.createRfcommSocketToServiceRecord(ObdConnection.SPP_UUID)
        try {
            socket.connect()
            val inStream = socket.inputStream
            val outStream = socket.outputStream
            command.isImperial = prefs.isImperialUnits
            val result = command.execute(inStream, outStream)
            val raw = command.rawResult.trim()
            if (raw.isNotBlank() && raw != result) {
                "$result (raw: $raw)"
            } else {
                result
            }
        } catch (e: Exception) {
            "Connection error: ${e.message}"
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainApp() {
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        var showWizard by remember {
            mutableStateOf(!prefs.isWizardCompleted && prefs.selectedDeviceAddress == null)
        }
        val telemetry by ObdReaderService.serviceState.collectAsState()
        val scope = rememberCoroutineScope()

        val screens = listOf(Screen.Dashboard, Screen.Diagnostics, Screen.Terminal, Screen.Settings)

        if (showWizard) {
            SetupWizardScreen(
                prefs = prefs,
                onFinish = {
                    showWizard = false
                    selectedTabIndex = 0
                },
                onCancel = {
                    showWizard = false
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "OBD-II Reader",
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF1E2228)
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF1E2228)
                    ) {
                        screens.forEachIndexed { index, screen ->
                            NavigationBarItem(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                icon = { Text(screen.badge, fontSize = 20.sp) },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTabIndex) {
                        0 -> DashboardScreen(
                            telemetry = telemetry,
                            selectedDeviceName = prefs.selectedDeviceName,
                            onStartService = { startObdService() },
                            onStopService = { stopObdService() },
                            onLaunchWizard = { showWizard = true }
                        )
                        1 -> DtcScannerScreen(
                            onRunCommand = { cmd -> runSingleCommandDirectly(cmd) },
                            isConnected = telemetry.connectionState == ConnectionState.CONNECTED
                        )
                        2 -> CommandScreen(
                            onRunCommand = { cmd -> runSingleCommandDirectly(cmd) },
                            isConnected = telemetry.connectionState == ConnectionState.CONNECTED
                        )
                        3 -> SettingsScreen(
                            prefs = prefs,
                            onSettingsChanged = {
                                // If service is running, restart it to apply new settings
                                if (ObdReaderService.isServiceRunning) {
                                    stopObdService()
                                    startObdService()
                                }
                            },
                            onLaunchWizard = { showWizard = true }
                        )
                    }
                }
            }
        }
    }
}
