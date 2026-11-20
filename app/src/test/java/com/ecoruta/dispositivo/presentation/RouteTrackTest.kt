package com.ecoruta.dispositivo.presentation

import com.ecoruta.dispositivo.presentation.onboarding.LatLon
import com.ecoruta.dispositivo.presentation.onboarding.RouteTrack
import com.ecoruta.dispositivo.presentation.onboarding.SanJuanDemoRoute
import com.ecoruta.dispositivo.presentation.onboarding.approachDegrees
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTrackTest {

    // An L: 1 km due north, then 3 km due east (near the equator, so degrees are ~111 km).
    private val start = LatLon(0.0, 0.0)
    private val corner = LatLon(0.009, 0.0)
    private val end = LatLon(0.009, 0.027)
    private val track = RouteTrack(listOf(start, corner, end))

    @Test
    fun `walks at constant speed, not one segment per share of time`() {
        // The first leg is a quarter of the length, so a quarter of the loop ends at the corner.
        val atQuarter = track.poseAt(0.25f).position
        assertEquals(corner.lat, atQuarter.lat, 1e-4)
        assertEquals(corner.lon, atQuarter.lon, 1e-4)
    }

    @Test
    fun `heading follows the current street`() {
        assertEquals(0f, track.poseAt(0.1f).heading, 0.5f)
        assertEquals(90f, track.poseAt(0.8f).heading, 0.5f)
    }

    @Test
    fun `travelled line ends at the bus`() {
        val pose = track.poseAt(0.6f)
        val line = track.travelled(pose)
        assertEquals(listOf(start, corner, pose.position), line)
    }

    @Test
    fun `ends are clamped`() {
        assertEquals(end.lon, track.poseAt(1.5f).position.lon, 1e-9)
        assertEquals(start.lat, track.poseAt(-1f).position.lat, 1e-9)
    }

    @Test
    fun `turns the short way round across north`() {
        // From 350° to 10° is a 20° turn clockwise, not 340° back.
        assertEquals(0f, approachDegrees(350f, 10f, 0.5f), 1e-3f)
        assertEquals(350f, approachDegrees(10f, 350f, 1f), 1e-3f)
    }

    @Test
    fun `demo route is the length its loop time is tuned for`() {
        assertEquals(1_700.0, RouteTrack(SanJuanDemoRoute).lengthMeters, 200.0)
    }
}
