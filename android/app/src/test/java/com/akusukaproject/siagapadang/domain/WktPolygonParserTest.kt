package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WktPolygonParserTest {
    @Test
    fun `parse menjaga urutan longitude latitude dan membaca lubang`() {
        val rings = WktPolygonParser.parse(
            "POLYGON ((100.0 -1.0, 101.0 -1.0, 101.0 0.0, 100.0 -1.0), " +
                "(100.2 -0.8, 100.4 -0.8, 100.3 -0.6, 100.2 -0.8))",
        )

        assertEquals(2, rings.size)
        assertEquals(-1.0, rings.first().first().latitude, 0.0)
        assertEquals(100.0, rings.first().first().longitude, 0.0)
    }

    @Test
    fun `parse menerima awalan SRID`() {
        val rings = WktPolygonParser.parse(
            "SRID=4326;POLYGON ((100 -1, 101 -1, 100 0, 100 -1))",
        )

        assertEquals(1, rings.size)
        assertEquals(4, rings.first().size)
    }

    @Test
    fun `parsePolygons membaca multipolygon tiga dimensi dari database zona`() {
        val polygons = WktPolygonParser.parsePolygons(
            "MULTIPOLYGON Z (((100 -1 4, 101 -1 5, 100 0 6, 100 -1 4)), " +
                "((102 -2 7, 103 -2 8, 102 -1 9, 102 -2 7)))",
        )

        assertEquals(2, polygons.size)
        assertEquals(1, polygons.first().size)
        assertEquals(4, polygons.first().first().size)
        assertEquals(-2.0, polygons[1].first().first().latitude, 0.0)
        assertEquals(102.0, polygons[1].first().first().longitude, 0.0)
    }
}
