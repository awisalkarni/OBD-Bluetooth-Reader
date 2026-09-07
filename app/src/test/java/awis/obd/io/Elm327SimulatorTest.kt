package awis.obd.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Elm327SimulatorTest {

    @Test
    fun testSimulatorAtCommands() {
        assertEquals("ELM327 v1.5", Elm327Simulator.respondToCommand("ATZ"))
        assertEquals("ELM327 v1.5", Elm327Simulator.respondToCommand("atz"))
        assertEquals("OK", Elm327Simulator.respondToCommand("ATE0"))
        assertEquals("OK", Elm327Simulator.respondToCommand("ATSP0"))
        assertEquals("14.2V", Elm327Simulator.respondToCommand("ATRV"))
        assertTrue(Elm327Simulator.respondToCommand("ATDP").contains("ISO 15765-4"))
    }

    @Test
    fun testSimulatorObdPids() {
        val dtcStatus = Elm327Simulator.respondToCommand("0101")
        assertTrue(dtcStatus.startsWith("41 01"))

        val dtcCodes = Elm327Simulator.respondToCommand("03")
        assertTrue(dtcCodes.startsWith("43"))
        assertTrue(dtcCodes.contains("01 71")) // P0171

        val clearResult = Elm327Simulator.respondToCommand("04")
        assertEquals("44", clearResult)
    }
}
