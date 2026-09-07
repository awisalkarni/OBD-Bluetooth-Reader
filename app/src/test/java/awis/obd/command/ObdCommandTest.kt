package awis.obd.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdCommandTest {

    @Test
    fun testEngineRpmCommand() {
        val rpmCmd = EngineRPMObdCommand()
        // Response for 010C: 41 0C 1A F8
        // 0x1A = 26, 0xF8 = 248 -> (26 * 256 + 248) / 4 = 1726
        val result = rpmCmd.parseResult("41 0C 1A F8\r\n>")
        assertEquals("1726 RPM", result)
        assertEquals(1726, rpmCmd.intValue)
    }

    @Test
    fun testSpeedCommandMetricAndImperial() {
        val speedCmd = SpeedObdCommand()
        // Response for 010D: 41 0D 3C -> 0x3C = 60 km/h
        val metricResult = speedCmd.parseResult("41 0D 3C\r\n>")
        assertEquals("60 km/h", metricResult)
        assertEquals(60, speedCmd.intValue)

        speedCmd.isImperial = true
        val imperialResult = speedCmd.formatResult()
        assertEquals("37 mph", imperialResult) // 60 * 0.625 = 37.5 -> 37
    }

    @Test
    fun testCoolantTempCommand() {
        val coolantCmd = TempObdCommand()
        // Response for 0105: 41 05 7B -> 0x7B = 123 -> 123 - 40 = 83 C
        val metricResult = coolantCmd.parseResult("41 05 7B\r\n>")
        assertEquals("83 C", metricResult)
        assertEquals(83, coolantCmd.intValue)

        coolantCmd.isImperial = true
        val imperialResult = coolantCmd.formatResult()
        assertEquals("181 F", imperialResult) // 83 * 9 / 5 + 32 = 149.4 + 32 = 181
    }

    @Test
    fun testMassAirFlowCommand() {
        val mafCmd = MassAirFlowObdCommand()
        // Response for 0110: 41 10 09 60 -> (9 * 256 + 96) / 100.0 = 24.00 g/s
        val result = mafCmd.parseResult("41 10 09 60\r\n>")
        assertEquals("24.00 g/s", result)
        assertEquals(24.0, mafCmd.maf, 0.01)
    }

    @Test
    fun testThrottleCommand() {
        val throttleCmd = ThrottleObdCommand()
        // Response for 0111: 41 11 80 -> (128 * 100) / 255 = 50%
        val result = throttleCmd.parseResult("41 11 80\r\n>")
        assertEquals("50 %", result)
        assertEquals(50, throttleCmd.intValue)
    }

    @Test
    fun testEngineRunTimeCommand() {
        val runtimeCmd = EngineRunTimeObdCommand()
        // Response for 011F: 41 1F 0E 10 -> 0x0E * 256 + 0x10 = 3600 sec = 01:00:00
        val result = runtimeCmd.parseResult("41 1F 0E 10\r\n>")
        assertEquals("01:00:00", result)
    }

    @Test
    fun testDtcNumberCommand() {
        val dtcCmd = DtcNumberObdCommand()
        // Response for 0101: 41 01 83 -> MIL is on (0x80), 3 codes (0x03)
        val result = dtcCmd.parseResult("41 01 83 00 00\r\n>")
        assertEquals("MIL is on, 3 codes", result)
        assertTrue(dtcCmd.milOn)
        assertEquals(3, dtcCmd.codeCount)
    }

    @Test
    fun testTroubleCodesParsing() {
        val tcCmd = TroubleCodesObdCommand()
        // Mode 03 response with P0133 (0x0133)
        val result = tcCmd.parseResult("43 01 33 00 00 00 00\r\n>")
        assertTrue("Expected P0133 in result", result.contains("P0133"))
    }

    @Test
    fun testFuelEconomyCalculation() {
        val feCmd = FuelEconomyObdCommand()
        val mpg = feCmd.calculateFromMafAndSpeed(mafGramsPerSec = 15.0, speedKmH = 90.0)
        assertTrue("MPG should be positive", mpg > 0)
        feCmd.isImperial = true
        val imperialResult = feCmd.formatResult()
        assertTrue("Result should contain mpg", imperialResult.contains("mpg"))

        feCmd.isImperial = false
        val metricResult = feCmd.formatResult()
        assertTrue("Result should contain kml", metricResult.contains("kml"))
    }

    @Test
    fun testAtCommandsParsing() {
        val atz = ObdCommand("ATZ", "Reset")
        val atzRes = atz.parseResult("ELM327 v1.5\r\n>")
        assertEquals("ELM327 v1.5", atzRes)

        val atrv = ObdCommand("ATRV", "Voltage")
        val atrvRes = atrv.parseResult("12.6V\r\n>")
        assertEquals("12.6V", atrvRes)

        val atdp = ObdCommand("ATDP", "Protocol")
        val atdpRes = atdp.parseResult("ISO 15765-4 (CAN 11/500)\r\n>")
        assertEquals("ISO 15765-4 (CAN 11/500)", atdpRes)
    }

    @Test
    fun testVehiclePresetsIntegrity() {
        val presets = awis.obd.ui.VEHICLE_PRESETS
        assertTrue("Presets should not be empty", presets.isNotEmpty())
        for (p in presets) {
            assertTrue("Displacement should be positive", p.displacement > 0.0)
            assertTrue("VE should be in reasonable range 0.5-1.2", p.ve in 0.5..1.2)
            assertTrue("Title should be non-blank", p.title.isNotBlank())
        }
    }
}
