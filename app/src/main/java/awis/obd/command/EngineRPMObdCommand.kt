package awis.obd.command

import awis.obd.config.ObdConfig

class EngineRPMObdCommand(
    cmd: String = "010C",
    desc: String = ObdConfig.RPM,
    resType: String = "RPM",
    impType: String = "RPM"
) : IntObdCommand(cmd, desc, resType, impType) {

    override fun copyCommand(): ObdCommand {
        return EngineRPMObdCommand(cmd ?: "010C", desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 8) {
            return "NODATA"
        }
        return try {
            val byteStrOne = res.substring(4, 6)
            val byteStrTwo = res.substring(6, 8)
            val a = byteStrOne.toInt(16)
            val b = byteStrTwo.toInt(16)
            intValue = ((a * 256 + b) / 4.0).toInt()
            rawValue = intValue
            "$intValue $resType"
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}
