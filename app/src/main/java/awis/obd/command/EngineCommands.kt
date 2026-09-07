package awis.obd.command

class ThrottleObdCommand(
    cmd: String = "0111",
    desc: String = "Throttle Position",
    resType: String = "%"
) : IntObdCommand(cmd, desc, resType, resType) {

    override fun copyCommand(): ObdCommand {
        return ThrottleObdCommand(cmd ?: "0111", desc, resType).also {
            it.isImperial = isImperial
        }
    }

    override fun transform(b: Int): Int = ((b * 100.0) / 255.0).toInt()
}

class FuelTrimObdCommand(
    cmd: String = "0107",
    desc: String = "Long Term Fuel Trim",
    resType: String = "%"
) : IntObdCommand(cmd, desc, resType, resType) {

    override fun copyCommand(): ObdCommand {
        return FuelTrimObdCommand(cmd ?: "0107", desc, resType).also {
            it.isImperial = isImperial
        }
    }

    override fun transform(b: Int): Int = ((b - 128) * (100.0 / 128.0)).toInt()
}

class TimingAdvanceObdCommand : ObdCommand("010E", "Timing Advance", "deg", "deg") {

    override fun copyCommand(): ObdCommand {
        return TimingAdvanceObdCommand().also { it.isImperial = isImperial }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 6) return "NODATA"
        return try {
            val a = res.substring(4, 6).toInt(16)
            val adv = (a / 2.0) / 64.0
            rawValue = adv
            java.lang.String.format(java.util.Locale.US, "%.1f %s", adv, resType)
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}

class EngineRunTimeObdCommand : ObdCommand("011F", "Engine Runtime", "", "") {

    override fun copyCommand(): ObdCommand {
        return EngineRunTimeObdCommand().also { it.isImperial = isImperial }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 8) return "NODATA"
        return try {
            val a = res.substring(4, 6).toInt(16)
            val b = res.substring(6, 8).toInt(16)
            val sec = (a * 256) + b
            rawValue = sec
            val hh = String.format(java.util.Locale.US, "%02d", sec / 3600)
            val mm = String.format(java.util.Locale.US, "%02d", (sec % 3600) / 60)
            val ss = String.format(java.util.Locale.US, "%02d", sec % 60)
            "$hh:$mm:$ss"
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}

class MassAirFlowObdCommand : ObdCommand("0110", "Mass Air Flow", "g/s", "g/s") {

    var maf: Double = -9999.0
        private set

    override fun copyCommand(): ObdCommand {
        return MassAirFlowObdCommand().also { it.isImperial = isImperial }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 8) return "NODATA"
        return try {
            val a = res.substring(4, 6).toInt(16)
            val b = res.substring(6, 8).toInt(16)
            maf = ((256.0 * a) + b) / 100.0
            rawValue = maf
            String.format(java.util.Locale.US, "%.2f %s", maf, resType)
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}

class CommandEquivRatioObdCommand : ObdCommand("0144", "Command Equivalence Ratio", "", "") {

    var ratio: Double = 1.0
        private set

    override fun copyCommand(): ObdCommand {
        return CommandEquivRatioObdCommand().also { it.isImperial = isImperial }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 8) return "NODATA"
        return try {
            val a = res.substring(4, 6).toInt(16)
            val b = res.substring(6, 8).toInt(16)
            ratio = ((a * 256) + b) * 0.0000305
            rawValue = ratio
            String.format(java.util.Locale.US, "%.2f", ratio)
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}
