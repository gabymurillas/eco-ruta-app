package com.ecoruta.dispositivo.presentation

import com.ecoruta.dispositivo.domain.model.GpsStatus
import com.ecoruta.dispositivo.domain.model.TripStatus
import com.ecoruta.dispositivo.domain.model.isNightAt
import com.ecoruta.dispositivo.fakes.T0
import com.ecoruta.dispositivo.presentation.common.Ago
import com.ecoruta.dispositivo.presentation.common.agoOf
import com.ecoruta.dispositivo.presentation.trip.TripHeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class PresentationRulesTest {

    private val healthy = TripStatus(gps = GpsStatus.GOOD, isOnline = true, isLocationEnabled = true)

    @Test
    fun `a healthy unit reads as transmitting`() {
        assertEquals(TripHeadline.TRANSMITTING, TripHeadline.of(healthy))
    }

    @Test
    fun `the headline shows the worst thing that is true`() {
        val everythingWrong = healthy.copy(gps = GpsStatus.NO_SIGNAL, isOnline = false, isLocationEnabled = false)

        assertEquals(TripHeadline.LOCATION_OFF, TripHeadline.of(everythingWrong))
        assertEquals(TripHeadline.SEARCHING_GPS, TripHeadline.of(everythingWrong.copy(isLocationEnabled = true)))
        assertEquals(TripHeadline.OFFLINE, TripHeadline.of(healthy.copy(isOnline = false, gps = GpsStatus.WEAK)))
        assertEquals(TripHeadline.WEAK_GPS, TripHeadline.of(healthy.copy(gps = GpsStatus.WEAK)))
    }

    @Test
    fun `losing the gps is an error, losing the network only a warning`() {
        // Offline the positions are still stored and sent later; with no GPS nothing is captured.
        assertEquals(TripHeadline.Severity.ERROR, TripHeadline.SEARCHING_GPS.severity)
        assertEquals(TripHeadline.Severity.WARNING, TripHeadline.OFFLINE.severity)
    }

    @Test
    fun `elapsed time uses the largest unit that fits`() {
        assertEquals(Ago(5, Ago.Unit.SECONDS), agoOf(T0, T0.plusSeconds(5)))
        assertEquals(Ago(59, Ago.Unit.SECONDS), agoOf(T0, T0.plusSeconds(59)))
        assertEquals(Ago(1, Ago.Unit.MINUTES), agoOf(T0, T0.plusSeconds(60)))
        assertEquals(Ago(2, Ago.Unit.HOURS), agoOf(T0, T0.plusSeconds(7_300)))
    }

    @Test
    fun `a send stamped slightly in the future reads as just now, not negative`() {
        assertEquals(Ago(0, Ago.Unit.SECONDS), agoOf(T0.plusSeconds(3), T0))
    }

    @Test
    fun `automatic theme is dark from 18 30 to 6 00`() {
        assertFalse(isNightAt(LocalTime.of(12, 0)))
        assertFalse(isNightAt(LocalTime.of(18, 29)))
        assertTrue(isNightAt(LocalTime.of(18, 30)))
        assertTrue(isNightAt(LocalTime.of(0, 0)))
        assertTrue(isNightAt(LocalTime.of(5, 59)))
        assertFalse(isNightAt(LocalTime.of(6, 0)))
    }
}
