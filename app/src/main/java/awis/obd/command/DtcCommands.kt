package awis.obd.command

class DtcNumberObdCommand : ObdCommand("0101", "DTC Status", "", "") {

    var codeCount: Int = -1
        private set
    var milOn: Boolean = false
        private set

    override fun copyCommand(): ObdCommand {
        return DtcNumberObdCommand().also { it.isImperial = isImperial }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        if (res.contains("NODATA") || res.length < 6) return "NODATA"
        return try {
            val byte1 = res.substring(4, 6)
            val mil = byte1.toInt(16)
            milOn = (mil and 0x80) != 0
            codeCount = mil and 0x7F
            rawValue = codeCount
            val status = if (milOn) "MIL is on" else "MIL is off"
            "$status, $codeCount codes"
        } catch (e: Exception) {
            error = e
            "ERR"
        }
    }
}

class TroubleCodesObdCommand(
    cmd: String = "03",
    desc: String = "Trouble Codes",
    resType: String = "",
    impType: String = ""
) : ObdCommand(cmd, desc, resType, impType) {

    private val codes = StringBuilder()
    val parsedCodeList = mutableListOf<String>()

    companion object {
        private val DTC_LETTERS = charArrayOf('P', 'C', 'B', 'U')
    }

    override fun copyCommand(): ObdCommand {
        return TroubleCodesObdCommand(cmd ?: "03", desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun formatResult(): String {
        val res = cleanRawResult()
        parsedCodeList.clear()
        if (res.contains("NODATA") || res == "4300") return "No Trouble Codes"
        if (res.isEmpty()) return ""

        val lines = rawResult.split("\r", "\n").filter { it.isNotBlank() }
        codes.clear()
        for (line in lines) {
            val clean = line.replace(" ", "").trim()
            if (clean.startsWith("43")) {
                // Parse Mode 03 response hex pairs
                var idx = 2
                while (idx + 4 <= clean.length) {
                    val hex = clean.substring(idx, idx + 4)
                    if (hex == "0000") break
                    try {
                        val valInt = hex.toInt(16)
                        val letterIdx = (valInt and 0xC000) shr 14
                        val letter = DTC_LETTERS.getOrElse(letterIdx) { 'P' }
                        val digit1 = (valInt and 0x3000) shr 12
                        val rest = String.format(java.util.Locale.US, "%03X", valInt and 0x0FFF)
                        val code = "$letter$digit1$rest"
                        parsedCodeList.add(code)
                        codes.append("$code\n")
                    } catch (_: Exception) {
                    }
                    idx += 4
                }
            } else if (!clean.startsWith("AT") && !clean.startsWith("SEARCHING") && !clean.startsWith("OK")) {
                codes.append(clean).append("\n")
            }
        }
        val output = codes.toString().trim()
        return if (output.isEmpty()) "No Trouble Codes" else output
    }
}
