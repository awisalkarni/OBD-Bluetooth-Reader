package awis.obd.command

import java.io.InputStream
import java.io.OutputStream

class ObdMultiCommand(
    val commands: List<String>,
    desc: String = "Multi Command",
    resType: String = "string",
    impType: String = "string"
) : ObdCommand(null, desc, resType, impType) {

    constructor(cmds: Array<String>, desc: String = "Multi Command", resType: String = "string", impType: String = "string") :
            this(cmds.toList(), desc, resType, impType)

    override fun copyCommand(): ObdCommand {
        return ObdMultiCommand(commands, desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    override fun execute(inputStream: InputStream, outputStream: OutputStream): String {
        buff.clear()
        error = null
        for (c in commands) {
            sendCmd(c, outputStream)
            readResult(inputStream)
            buff.add('\n'.code.toByte())
        }
        rawResult = String(buff.toByteArray())
        return formatResult()
    }

    override fun formatResult(): String {
        return rawResult.trim()
    }
}
