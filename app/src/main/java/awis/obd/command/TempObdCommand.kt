package awis.obd.command

open class TempObdCommand(
    cmd: String = "0105",
    desc: String = "Coolant Temp",
    resType: String = "C",
    impType: String = "F"
) : IntObdCommand(cmd, desc, resType, impType) {

    override fun copyCommand(): ObdCommand {
        return TempObdCommand(cmd ?: "0105", desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun transform(b: Int): Int = b - 40

    override fun getImperialInt(): Int = (intValue * 9 / 5) + 32
}

class AirIntakeTempObdCommand : TempObdCommand(
    cmd = "010F",
    desc = "Air Intake Temp",
    resType = "C",
    impType = "F"
) {
    override fun copyCommand(): ObdCommand {
        return AirIntakeTempObdCommand().also { it.isImperial = isImperial }
    }
}
