package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WktLineStringParserTest {
    @Test
    fun `parser membaca urutan longitude lalu latitude`() {
        val coordinates = WktLineStringParser.parse(
            "LINESTRING (100.4146528 -0.9539962, 100.4147655 -0.9538743)",
        )

        assertEquals(-0.9539962, coordinates.first().latitude, 0.0)
        assertEquals(100.4146528, coordinates.first().longitude, 0.0)
        assertEquals(2, coordinates.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parser menolak geometri selain linestring`() {
        WktLineStringParser.parse("POINT (100.4 -0.9)")
    }
}

