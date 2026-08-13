package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate

object WktPolygonParser {
    fun parse(value: String): List<List<GeoCoordinate>> {
        val polygons = parsePolygons(value)
        require(polygons.size == 1) {
            "Gunakan parsePolygons untuk geometri MULTIPOLYGON"
        }
        return polygons.first()
    }

    fun parsePolygons(value: String): List<List<List<GeoCoordinate>>> {
        val geometry = value.substringAfterLast(';').trim()
        val bodyStart = geometry.indexOf('(')
        require(bodyStart > 0) { "Geometri zona tidak memiliki koordinat" }
        val geometryType = geometry.substring(0, bodyStart).trim()
        val body = geometry.substring(bodyStart).trim()

        return when {
            geometryType.startsWith(MULTIPOLYGON_PREFIX, ignoreCase = true) -> {
                require(body.startsWith('(') && body.endsWith(')')) {
                    "Kurung MULTIPOLYGON tidak lengkap"
                }
                splitTopLevel(body.substring(1, body.length - 1)).map(::parsePolygonBody)
            }

            geometryType.startsWith(POLYGON_PREFIX, ignoreCase = true) ->
                listOf(parsePolygonBody(body))

            else -> error("Geometri zona harus berupa POLYGON atau MULTIPOLYGON")
        }
    }

    private fun parsePolygonBody(body: String): List<List<GeoCoordinate>> {
        val normalizedBody = body.trim()
        require(normalizedBody.startsWith('(') && normalizedBody.endsWith(')')) {
            "Kurung POLYGON tidak lengkap"
        }

        val rings = splitTopLevel(normalizedBody.substring(1, normalizedBody.length - 1))
            .map { ringText ->
                val coordinatesText = ringText.trim().removeSurrounding("(", ")")
                coordinatesText.split(',').map { coordinateText ->
                    val values = coordinateText.trim().split(WHITESPACE)
                    require(values.size >= 2) { "Koordinat zona tidak lengkap" }
                    GeoCoordinate(
                        latitude = values[1].toDouble(),
                        longitude = values[0].toDouble(),
                    )
                }
            }

        require(rings.isNotEmpty() && rings.all { it.size >= 3 }) {
            "POLYGON tidak memiliki ring yang valid"
        }
        return rings
    }

    private fun splitTopLevel(value: String): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        value.forEachIndexed { index, character ->
            when (character) {
                '(' -> depth += 1
                ')' -> depth -= 1
                ',' -> if (depth == 0) {
                    parts += value.substring(start, index)
                    start = index + 1
                }
            }
            require(depth >= 0) { "Kurung POLYGON tidak seimbang" }
        }
        require(depth == 0) { "Kurung POLYGON tidak seimbang" }
        parts += value.substring(start)
        return parts.filter { it.isNotBlank() }
    }

    private const val POLYGON_PREFIX = "POLYGON"
    private const val MULTIPOLYGON_PREFIX = "MULTIPOLYGON"
    private val WHITESPACE = Regex("\\s+")
}
