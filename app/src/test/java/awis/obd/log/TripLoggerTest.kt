package awis.obd.log

import org.junit.Assert.assertEquals
import org.junit.Test

class TripLoggerTest {

    @Test
    fun testTripDataPointCreation() {
        val point = TripDataPoint(
            timestamp = 1600000000000L,
            rpm = 2500,
            speedKmh = 80,
            coolantTempC = 90,
            fuelEconomy = "7.5 l/100km",
            latitude = 37.7749,
            longitude = -122.4194,
            altitude = 15.0
        )

        assertEquals(2500, point.rpm)
        assertEquals(80, point.speedKmh)
        assertEquals(90, point.coolantTempC)
        assertEquals(37.7749, point.latitude ?: 0.0, 0.0001)
    }

    @Test
    fun testTripSummaryCalculation() {
        val summary = TripSummary(
            sessionId = "trip_test",
            startTime = 1000L,
            endTime = 61000L,
            pointCount = 10,
            maxSpeedKmh = 120,
            avgSpeedKmh = 65.0,
            maxRpm = 4500,
            avgRpm = 2300.0,
            maxCoolantTempC = 92,
            distanceKm = 1.08
        )

        assertEquals("trip_test", summary.sessionId)
        assertEquals(120, summary.maxSpeedKmh)
        assertEquals(65.0, summary.avgSpeedKmh, 0.01)
        assertEquals(4500, summary.maxRpm)
        assertEquals(1.08, summary.distanceKm, 0.01)
    }
}
