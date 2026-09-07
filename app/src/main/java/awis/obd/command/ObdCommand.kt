package awis.obd.command

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Base class for all OBD commands sent to the ELM327 interface.
 */
open class ObdCommand(
    open var cmd: String? = null,
    val desc: String = "",
    val resType: String = "",
    val impType: String = ""
) {
    var rawResult: String = ""
    var rawValue: Any? = null
    var error: Exception? = null
    var isImperial: Boolean = false

    protected val buff = mutableListOf<Byte>()

    open fun copyCommand(): ObdCommand {
        return ObdCommand(cmd, desc, resType, impType).also {
            it.isImperial = isImperial
        }
    }

    open fun execute(inputStream: InputStream, outputStream: OutputStream): String {
        buff.clear()
        error = null
        cmd?.let { sendCmd(it, outputStream) }
        readResult(inputStream)
        rawResult = String(buff.toByteArray())
        return formatResult()
    }

    protected open fun sendCmd(command: String, out: OutputStream) {
        try {
            val cmdWithCr = if (command.endsWith("\r")) command else "$command\r\n"
            out.write(cmdWithCr.toByteArray())
            out.flush()
        } catch (e: Exception) {
            error = e
        }
    }

    protected open fun readResult(input: InputStream) {
        try {
            var c: Byte
            while (true) {
                val b = input.read()
                if (b == -1) break
                c = b.toByte()
                if (c.toInt().toChar() == '>') break
                buff.add(c)
            }
        } catch (e: IOException) {
            error = e
        }
    }

    /**
     * Parse raw response string directly, useful for testing and offline parsing.
     */
    open fun parseResult(raw: String): String {
        rawResult = raw
        buff.clear()
        raw.toByteArray().forEach { buff.add(it) }
        return formatResult()
    }

    open fun formatResult(): String {
        val res = cleanRawResult()
        return res
    }

    protected fun cleanRawResult(): String {
        val lines = rawResult.split("\r", "\n").filter { it.isNotBlank() }
        if (lines.isEmpty()) return ""
        // Take first relevant line that contains hex or response
        val target = lines.firstOrNull { !it.startsWith("AT") && !it.startsWith("SEARCHING") } ?: lines[0]
        return target.replace(" ", "").trim()
    }
}
