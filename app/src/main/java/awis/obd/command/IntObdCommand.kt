package awis.obd.command

open class IntObdCommand(
    cmd: String? = null,
    desc: String = "",
    resType: String = "",
    impType: String = ""
) : ObdCommand(cmd, desc, resType, impType) {

    var intValue: Int = -9999
        protected set

    override fun copyCommand(): ObdCommand {
        return IntObdCommand(cmd, desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun formatResult(): String {
        val res = super.formatResult()
        if (res.contains("NODATA") || res.length < 6) {
            return "NODATA"
        }
        return try {
            val byteStr = res.substring(4, 6)
            val b = byteStr.toInt(16)
            intValue = transform(b)
            rawValue = intValue
            if (isImperial) {
                "${getImperialInt()} $impType"
            } else {
                "$intValue $resType"
            }
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }

    open fun transform(b: Int): Int = b

    open fun getImperialInt(): Int = intValue
}
