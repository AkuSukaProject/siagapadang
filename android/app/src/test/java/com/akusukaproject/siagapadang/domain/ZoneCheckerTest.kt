package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneCheckerTest {
    private val outerRing = listOf(
        GeoCoordinate(latitude = -1.0, longitude = 100.0),
        GeoCoordinate(latitude = -1.0, longitude = 101.0),
        GeoCoordinate(latitude = 0.0, longitude = 101.0),
        GeoCoordinate(latitude = 0.0, longitude = 100.0),
        GeoCoordinate(latitude = -1.0, longitude = 100.0),
    )

    @Test
    fun `titik di dalam ring terdeteksi`() {
        assertTrue(
            ZoneChecker.contains(
                point = GeoCoordinate(latitude = -0.5, longitude = 100.5),
                rings = listOf(outerRing),
            ),
        )
    }

    @Test
    fun `titik di luar ring tidak terdeteksi`() {
        assertFalse(
            ZoneChecker.contains(
                point = GeoCoordinate(latitude = -1.5, longitude = 100.5),
                rings = listOf(outerRing),
            ),
        )
    }

    @Test
    fun `titik pada lubang polygon tidak dianggap berada di zona`() {
        val hole = listOf(
            GeoCoordinate(latitude = -0.7, longitude = 100.3),
            GeoCoordinate(latitude = -0.7, longitude = 100.7),
            GeoCoordinate(latitude = -0.3, longitude = 100.7),
            GeoCoordinate(latitude = -0.3, longitude = 100.3),
            GeoCoordinate(latitude = -0.7, longitude = 100.3),
        )

        assertFalse(
            ZoneChecker.contains(
                point = GeoCoordinate(latitude = -0.5, longitude = 100.5),
                rings = listOf(outerRing, hole),
            ),
        )
    }

    @Test
    fun `titik pada batas luar tetap dianggap berada di zona`() {
        assertTrue(
            ZoneChecker.contains(
                point = GeoCoordinate(latitude = -1.0, longitude = 100.5),
                rings = listOf(outerRing),
            ),
        )
    }
}
