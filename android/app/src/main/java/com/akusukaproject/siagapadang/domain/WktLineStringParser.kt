package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate

object WktLineStringParser {
    private val lineStringPattern = Regex(
        pattern = """^\s*LINESTRING(?:\s+Z)?\s*\((.+)\)\s*$""",
        option = RegexOption.IGNORE_CASE,
    )

    fun parse(wkt: String): List<GeoCoordinate> {
        val body = lineStringPattern.matchEntire(wkt)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Geometri ruas bukan LINESTRING yang valid")

        return body.split(',').map { coordinateText ->
            val values = coordinateText.trim().split(Regex("\\s+"))
            require(values.size >= 2) { "Koordinat WKT tidak lengkap" }
            val longitude = values[0].toDoubleOrNull()
                ?: throw IllegalArgumentException("Longitude WKT tidak valid")
            val latitude = values[1].toDoubleOrNull()
                ?: throw IllegalArgumentException("Latitude WKT tidak valid")
            GeoCoordinate(latitude = latitude, longitude = longitude)
        }.also { coordinates ->
            require(coordinates.size >= 2) { "LINESTRING harus memiliki sedikitnya dua titik" }
        }
    }
}

