package awis.obd.command

import awis.obd.config.ObdConfig

class SpeedObdCommand(
    cmd: String = "010D",
    desc: String = ObdConfig.SPEED,
    resType: String = "km/h",
    impType: String = "mph"
) : IntObdCommand(cmd, desc, resType, impType) {

    override fun copyCommand(): ObdCommand {
        return SpeedObdCommand(cmd ?: "010D", desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun getImperialInt(): Int {
        if (intValue <= 0) return 0
        return (intValue * 0.625).toInt()
    }
}
