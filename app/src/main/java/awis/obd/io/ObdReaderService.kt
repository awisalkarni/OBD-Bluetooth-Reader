package awis.obd.io

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import awis.obd.activity.ObdReaderMainActivity
import awis.obd.config.ObdConfig
import awis.obd.config.ObdPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ObdReaderService : Service() {

    companion object {
        const val CHANNEL_ID = "obd_reader_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "awis.obd.action.START"
        const val ACTION_STOP = "awis.obd.action.STOP"

        private val _serviceState = MutableStateFlow(ObdTelemetry())
        val serviceState: StateFlow<ObdTelemetry> = _serviceState.asStateFlow()

        var isServiceRunning: Boolean = false
            private set
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var connectionJob: Job? = null
    private var obdConnection: ObdConnection? = null

    inner class LocalBinder : Binder() {
        fun getService(): ObdReaderService = this@ObdReaderService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopObdService()
            }
            ACTION_START, null -> {
                startObdService()
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startObdService() {
        if (isServiceRunning) return
        isServiceRunning = true

        val notification = buildNotification("OBD Service Starting...")
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
        } catch (e: Exception) {
            // Fallback for devices where foreground service permissions/types have nuances
            startForeground(NOTIFICATION_ID, notification)
        }

        val prefs = ObdPreferences(this)
        val deviceAddress = prefs.selectedDeviceAddress

        if (deviceAddress.isNullOrBlank()) {
            _serviceState.value = ObdTelemetry(
                connectionState = ConnectionState.ERROR,
                statusMessage = "No Bluetooth OBD device selected in Settings"
            )
            updateNotification("No device selected")
            return
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _serviceState.value = ObdTelemetry(
                connectionState = ConnectionState.ERROR,
                statusMessage = "Bluetooth is disabled or unavailable"
            )
            updateNotification("Bluetooth unavailable")
            return
        }

        val device = try {
            bluetoothAdapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            _serviceState.value = ObdTelemetry(
                connectionState = ConnectionState.ERROR,
                statusMessage = "Invalid Bluetooth device: ${e.message}"
            )
            return
        }

        // Filter enabled commands
        val activeCommands = ObdConfig.getCommands().filter {
            prefs.isCommandEnabled(it.desc)
        }

        val initCommands = prefs.readerConfigCommands
            .split("\n", "\r")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val connection = ObdConnection(
            device = device,
            commands = activeCommands,
            updatePeriodMs = (prefs.updatePeriodSeconds * 1000L),
            isImperial = prefs.isImperialUnits,
            volumetricEfficiency = prefs.volumetricEfficiency,
            engineDisplacement = prefs.engineDisplacement,
            initCommands = if (initCommands.isNotEmpty()) initCommands else listOf("ATZ", "ATE0", "ATSP0")
        )
        obdConnection = connection

        connectionJob = serviceScope.launch {
            connection.telemetry.collect { telemetry ->
                _serviceState.value = telemetry
                val status = when (telemetry.connectionState) {
                    ConnectionState.CONNECTED -> "RPM: ${telemetry.rpm} | Speed: ${telemetry.speed} ${telemetry.speedUnit}"
                    ConnectionState.CONNECTING -> "Connecting to ${device.name ?: device.address}..."
                    ConnectionState.INITIALIZING -> "Initializing adapter..."
                    ConnectionState.ERROR -> telemetry.statusMessage
                    ConnectionState.DISCONNECTED -> "Disconnected"
                }
                updateNotification(status)
            }
        }

        serviceScope.launch {
            connection.start()
        }
    }

    private fun stopObdService() {
        isServiceRunning = false
        connectionJob?.cancel()
        obdConnection?.stop()
        obdConnection = null
        _serviceState.value = ObdTelemetry(
            connectionState = ConnectionState.DISCONNECTED,
            statusMessage = "Stopped"
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OBD-II Bluetooth Reader",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live OBD vehicle data and connection status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val launchIntent = Intent(this, ObdReaderMainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OBD Bluetooth Reader")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        stopObdService()
        serviceScope.cancel()
    }
}
