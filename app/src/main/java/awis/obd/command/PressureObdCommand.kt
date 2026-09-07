package awis.obd.command

import java.util.Locale

open class PressureObdCommand(
    cmd: String = "0133",
    desc: String = "Barometric Press",
    resType: String = "kPa",
    impType: String = "atm"
) : IntObdCommand(cmd, desc, resType, impType) {

    override fun copyCommand(): ObdCommand {
        return PressureObdCommand(cmd ?: "0133", desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun formatResult(): String {
        val res = super.formatResult()
        if (!isImperial || res == "NODATA" || res == "ERR") {
            return res
        }
        val atm = intValue * 1.0 / 101.3
        return String.format(Locale.US, "%.2f %s", atm, impType)
    }
}

class IntakeManifoldPressureObdCommand : PressureObdCommand(
    cmd = "010B",
    desc = "Intake Manifold Press",
    resType = "kPa",
    impType = "atm"
) {
    override fun copyCommand(): ObdCommand {
        return IntakeManifoldPressureObdCommand().also { it.isImperial = isImperial }
    }
}

class FuelPressureObdCommand : PressureObdCommand(
    cmd = "010A",
    desc = "Fuel Press",
    resType = "kPa",
    impType = "atm"
) {
    override fun copyCommand(): ObdCommand {
        return FuelPressureObdCommand().also { it.isImperial = isImperial }
    }

    override fun transform(b: Int): Int = b * 3
}
