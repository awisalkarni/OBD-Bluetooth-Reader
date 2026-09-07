package awis.obd.command

import awis.obd.config.ObdConfig
import java.util.Locale

open class FuelEconomyObdCommand(
    cmd: String = "",
    desc: String = ObdConfig.FUEL_ECON,
    resType: String = "kml",
    impType: String = "mpg"
) : ObdCommand(cmd, desc, resType, impType) {

    var fuelEcon: Double = -9999.0

    override fun copyCommand(): ObdCommand {
        return FuelEconomyObdCommand(cmd ?: "", desc, resType, impType).also {
            it.isImperial = isImperial
            it.fuelEcon = fuelEcon
        }
    }

    fun calculateFromMafAndSpeed(mafGramsPerSec: Double, speedKmH: Double): Double {
        if (mafGramsPerSec <= 0.0 || speedKmH <= 0.0) {
            fuelEcon = -9999.0
            return fuelEcon
        }
        // Formula: 14.7 AFR, 6.17 lbs/gal gasoline, 454 grams/lb, speed in mph
        fuelEcon = (14.7 * 6.17 * 454.0 * speedKmH * 0.621371) / (3600.0 * mafGramsPerSec)
        rawValue = fuelEcon
        return fuelEcon
    }

    override fun formatResult(): String {
        if (fuelEcon <= 0) return "NODATA"
        return if (!isImperial) {
            val kml = fuelEcon * 0.354013
            String.format(Locale.US, "%.1f %s", kml, resType)
        } else {
            String.format(Locale.US, "%.1f %s", fuelEcon, impType)
        }
    }
}

class FuelEconomyMAPObdCommand : FuelEconomyObdCommand(
    cmd = "",
    desc = ObdConfig.FUEL_ECON_MAP,
    resType = "kml",
    impType = "mpg"
) {
    override fun copyCommand(): ObdCommand {
        return FuelEconomyMAPObdCommand().also {
            it.isImperial = isImperial
            it.fuelEcon = fuelEcon
        }
    }

    fun calculateFromMap(
        rpm: Double,
        mapKpa: Double,
        tempC: Double,
        speedKmH: Double,
        ve: Double = 0.85,
        edLiters: Double = 1.6,
        equivRatio: Double = 1.0
    ): Double {
        val tempK = tempC + 273.15
        if (tempK <= 0 || rpm <= 0 || speedKmH <= 0) {
            fuelEcon = -9999.0
            return fuelEcon
        }
        val imap = (rpm * mapKpa) / tempK
        val mafGramsPerSec = (imap / 120.0) * ve * edLiters * 28.97 / 8.314
        if (mafGramsPerSec <= 0) {
            fuelEcon = -9999.0
            return fuelEcon
        }
        fuelEcon = (14.7 * equivRatio * 6.17 * 454.0 * speedKmH * 0.621371) / (3600.0 * mafGramsPerSec)
        rawValue = fuelEcon
        return fuelEcon
    }
}

class FuelEconomyCommandedMAPObdCommand : FuelEconomyObdCommand(
    cmd = "",
    desc = "Fuel Economy Cmd. MAP",
    resType = "kml",
    impType = "mpg"
) {
    override fun copyCommand(): ObdCommand {
        return FuelEconomyCommandedMAPObdCommand().also {
            it.isImperial = isImperial
            it.fuelEcon = fuelEcon
        }
    }
}

class AverageFuelEconomyObdCommand : ObdCommand("", "Fuel Economy Average", "kml", "mpg") {

    private var sumMpg = 0.0
    private var count = 0

    override fun copyCommand(): ObdCommand {
        return AverageFuelEconomyObdCommand().also {
            it.isImperial = isImperial
            it.sumMpg = sumMpg
            it.count = count
        }
    }

    fun addSample(mpg: Double) {
        if (mpg > 0) {
            sumMpg += mpg
            count++
        }
    }

    fun reset() {
        sumMpg = 0.0
        count = 0
    }

    val averageMpg: Double
        get() = if (count > 0) sumMpg / count else 0.0

    override fun formatResult(): String {
        val avg = averageMpg
        if (avg <= 0) return "NODATA"
        return if (!isImperial) {
            val kml = avg * 0.354013
            String.format(Locale.US, "%.1f %s", kml, resType)
        } else {
            String.format(Locale.US, "%.1f %s", avg, impType)
        }
    }
}
