package com.akusukaproject.siagapadang.data.repository

import com.akusukaproject.siagapadang.data.local.ZoneDao
import com.akusukaproject.siagapadang.data.local.ZoneGeometryRow
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.InundationZoneStatus
import com.akusukaproject.siagapadang.data.model.TsunamiZoneOverlay
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import com.akusukaproject.siagapadang.domain.WktPolygonParser
import com.akusukaproject.siagapadang.domain.ZoneChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point

class ZoneRepository(
    private val dao: ZoneDao,
) {
    @Volatile
    private var cachedMapOverlay: TsunamiZoneOverlay? = null

    @Volatile
    private var cachedRiskZones: List<ParsedRiskZone>? = null

    suspend fun findStatus(location: GeoCoordinate): InundationZoneStatus =
        withContext(Dispatchers.Default) {
            val nearestNode = SEARCH_WINDOWS.firstNotNullOfOrNull { window ->
                val candidates = dao.findNodesInBounds(
                    minLat = location.latitude - window,
                    maxLat = location.latitude + window,
                    minLon = location.longitude - window,
                    maxLon = location.longitude + window,
                )
                NearestNodeFinder.findNearest(location, candidates)
            }
            if (nearestNode != null) {
                if (nearestNode.isSafe == SAFE_NODE_FLAG) {
                    return@withContext InundationZoneStatus.OutsideRecordedZone
                }
                findRiskZoneStatus(location)?.let { return@withContext it }
                return@withContext InundationZoneStatus.InsideRecordedZone(
                    zoneName = RISK_ZONE_NAME,
                    dangerLevel = RISK_LEVEL,
                )
            }

            // Fallback untuk posisi di luar jangkauan node jalan. Poligon juga
            // tetap menjadi sumber data layer visual peta.
            val rows = dao.findAllInundationZones()
            if (rows.isEmpty()) return@withContext InundationZoneStatus.DataUnavailable

            var validGeometryCount = 0
            rows.forEach { zone ->
                val polygons = runCatching { WktPolygonParser.parsePolygons(zone.geometryWkt) }
                    .getOrNull()
                    ?: return@forEach
                validGeometryCount += 1
                if (polygons.any { rings -> ZoneChecker.contains(location, rings) }) {
                    return@withContext InundationZoneStatus.InsideRecordedZone(
                        zoneName = zone.name,
                        dangerLevel = zone.dangerLevel,
                    )
                }
            }

            if (validGeometryCount == 0) {
                InundationZoneStatus.DataUnavailable
            } else {
                InundationZoneStatus.OutsideRecordedZone
            }
        }

    private suspend fun findRiskZoneStatus(
        location: GeoCoordinate,
    ): InundationZoneStatus.InsideRecordedZone? {
        val parsedZones = cachedRiskZones ?: dao.findAllInundationZones()
            .mapNotNull { zone ->
                runCatching { WktPolygonParser.parsePolygons(zone.geometryWkt) }
                    .getOrNull()
                    ?.let { polygons ->
                        ParsedRiskZone(
                            name = zone.name,
                            dangerLevel = zone.dangerLevel,
                            polygons = polygons,
                        )
                    }
            }
            .sortedByDescending { zone -> dangerPriority(zone.dangerLevel) }
            .also { zones -> cachedRiskZones = zones }

        val zone = parsedZones.firstOrNull { candidate ->
            candidate.polygons.any { rings -> ZoneChecker.contains(location, rings) }
        } ?: return null
        return InundationZoneStatus.InsideRecordedZone(
            zoneName = zone.name,
            dangerLevel = zone.dangerLevel,
        )
    }

    suspend fun loadMapOverlay(): TsunamiZoneOverlay {
        cachedMapOverlay?.let { return it }
        val safeRows = dao.findSafeZoneGeometries()
        val riskRows = dao.findInundationZoneGeometries()
        return withContext(Dispatchers.Default) {
            TsunamiZoneOverlay(
                safeGeoJson = safeRows.toFeatureCollectionJson(),
                lowRiskGeoJson = riskRows
                    .filter { it.level.equals(LOW_RISK_LEVEL, ignoreCase = true) }
                    .toFeatureCollectionJson(),
                mediumRiskGeoJson = riskRows
                    .filter { it.level.equals(MEDIUM_RISK_LEVEL, ignoreCase = true) }
                    .toFeatureCollectionJson(),
                highRiskGeoJson = riskRows
                    .filter { it.level.equals(HIGH_RISK_LEVEL, ignoreCase = true) }
                    .toFeatureCollectionJson(),
            )
        }.also { overlay -> cachedMapOverlay = overlay }
    }

    private fun List<ZoneGeometryRow>.toFeatureCollectionJson(): String {
        val features = mapNotNull { row ->
            val polygons = runCatching { WktPolygonParser.parsePolygons(row.geometryWkt) }
                .getOrNull()
                ?: return@mapNotNull null
            val points = polygons.map { rings ->
                rings.map { ring ->
                    ring.map { coordinate ->
                        Point.fromLngLat(coordinate.longitude, coordinate.latitude)
                    }
                }
            }
            Feature.fromGeometry(MultiPolygon.fromLngLats(points)).apply {
                addStringProperty(ZONE_NAME_PROPERTY, row.name)
                addStringProperty(ZONE_LEVEL_PROPERTY, row.level)
            }
        }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    private companion object {
        val SEARCH_WINDOWS = listOf(0.005, 0.02)
        const val SAFE_NODE_FLAG = 1
        const val RISK_ZONE_NAME = "Zona risiko tsunami"
        const val RISK_LEVEL = "Berisiko"
        const val LOW_RISK_LEVEL = "Rendah"
        const val MEDIUM_RISK_LEVEL = "Sedang"
        const val HIGH_RISK_LEVEL = "Tinggi"
        const val ZONE_NAME_PROPERTY = "zone_name"
        const val ZONE_LEVEL_PROPERTY = "zone_level"
    }

    private data class ParsedRiskZone(
        val name: String,
        val dangerLevel: String,
        val polygons: List<List<List<GeoCoordinate>>>,
    )

    private fun dangerPriority(level: String): Int = when {
        level.equals(HIGH_RISK_LEVEL, ignoreCase = true) -> 3
        level.equals(MEDIUM_RISK_LEVEL, ignoreCase = true) -> 2
        level.equals(LOW_RISK_LEVEL, ignoreCase = true) -> 1
        else -> 0
    }
}
