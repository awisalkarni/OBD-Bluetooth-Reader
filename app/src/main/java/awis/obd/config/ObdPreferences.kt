package awis.obd.config

import android.content.Context
import android.content.SharedPreferences

class ObdPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "awis_obd_prefs"

        const val KEY_BLUETOOTH_DEVICE = "bluetooth_device_address"
        const val KEY_BLUETOOTH_NAME = "bluetooth_device_name"
        const val KEY_IMPERIAL_UNITS = "imperial_units"
        const val KEY_ENABLE_GPS = "enable_gps"
        const val KEY_UPDATE_PERIOD = "update_period"
        const val KEY_MAX_FUEL_ECON = "max_fuel_econ"
        const val KEY_VOLUMETRIC_EFFICIENCY = "volumetric_efficiency"
        const val KEY_ENGINE_DISPLACEMENT = "engine_displacement"
        const val KEY_READER_CONFIG = "reader_config"
        const val PREFIX_CMD_ENABLED = "cmd_enabled_"
    }

    var selectedDeviceAddress: String?
        get() = prefs.getString(KEY_BLUETOOTH_DEVICE, null)
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_DEVICE, value).apply()

    var selectedDeviceName: String?
        get() = prefs.getString(KEY_BLUETOOTH_NAME, null)
        set(value) = prefs.edit().putString(KEY_BLUETOOTH_NAME, value).apply()

    var isImperialUnits: Boolean
        get() = prefs.getBoolean(KEY_IMPERIAL_UNITS, false)
        set(value) = prefs.edit().putBoolean(KEY_IMPERIAL_UNITS, value).apply()

    var isGpsEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLE_GPS, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLE_GPS, value).apply()

    var updatePeriodSeconds: Int
        get() = prefs.getInt(KEY_UPDATE_PERIOD, 4).coerceAtLeast(1)
        set(value) = prefs.edit().putInt(KEY_UPDATE_PERIOD, value).apply()

    var maxFuelEconomy: Double
        get() = prefs.getString(KEY_MAX_FUEL_ECON, "70")?.toDoubleOrNull() ?: 70.0
        set(value) = prefs.edit().putString(KEY_MAX_FUEL_ECON, value.toString()).apply()

    var volumetricEfficiency: Double
        get() = prefs.getString(KEY_VOLUMETRIC_EFFICIENCY, "0.85")?.toDoubleOrNull() ?: 0.85
        set(value) = prefs.edit().putString(KEY_VOLUMETRIC_EFFICIENCY, value.toString()).apply()

    var engineDisplacement: Double
        get() = prefs.getString(KEY_ENGINE_DISPLACEMENT, "1.6")?.toDoubleOrNull() ?: 1.6
        set(value) = prefs.edit().putString(KEY_ENGINE_DISPLACEMENT, value.toString()).apply()

    var readerConfigCommands: String
        get() = prefs.getString(KEY_READER_CONFIG, "atsp0\natz") ?: "atsp0\natz"
        set(value) = prefs.edit().putString(KEY_READER_CONFIG, value).apply()

    var vehicleProfileName: String
        get() = prefs.getString("vehicle_profile_name", "My Vehicle") ?: "My Vehicle"
        set(value) = prefs.edit().putString("vehicle_profile_name", value).apply()

    var isWizardCompleted: Boolean
        get() = prefs.getBoolean("is_wizard_completed", false)
        set(value) = prefs.edit().putBoolean("is_wizard_completed", value).apply()

    fun isCommandEnabled(commandDesc: String): Boolean {
        return prefs.getBoolean(PREFIX_CMD_ENABLED + commandDesc, true)
    }

    fun setCommandEnabled(commandDesc: String, enabled: Boolean) {
        prefs.edit().putBoolean(PREFIX_CMD_ENABLED + commandDesc, enabled).apply()
    }
}
