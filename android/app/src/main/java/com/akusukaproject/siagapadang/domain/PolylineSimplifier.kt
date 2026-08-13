package com.akusukaproject.siagapadang.domain

import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import java.util.ArrayDeque
import kotlin.math.cos
import kotlin.math.sqrt

/** Menyederhanakan garis untuk render saja; geometri sumber dan kalkulasi arahan tetap utuh. */
object PolylineSimplifier {
    fun simplify(
        coordinates: List<GeoCoordinate>,
        toleranceMeters: Double = DEFAULT_TOLERANCE_METERS,
    ): List<GeoCoordinate> {
        if (coordinates.size <= 2 || toleranceMeters <= 0.0) return coordinates

        val keep = BooleanArray(coordinates.size)
        keep[0] = true
        keep[coordinates.lastIndex] = true
        val ranges = ArrayDeque<Pair<Int, Int>>()
        ranges.add(0 to coordinates.lastIndex)

        while (ranges.isNotEmpty()) {
            val (start, end) = ranges.removeLast()
            var farthestIndex = -1
            var farthestDistance = 0.0
            for (index in start + 1 until end) {
                val distance = distanceToSegmentMeters(
                    point = coordinates[index],
                    start = coordinates[start],
                    end = coordinates[end],
                )
                if (distance > farthestDistance) {
                    farthestDistance = distance
                    farthestIndex = index
                }
            }
            if (farthestIndex >= 0 && farthestDistance > toleranceMeters) {
                keep[farthestIndex] = true
                ranges.add(start to farthestIndex)
                ranges.add(farthestIndex to end)
            }
        }

        return coordinates.filterIndexed { index, _ -> keep[index] }
    }

    private fun distanceToSegmentMeters(
        point: GeoCoordinate,
        start: GeoCoordinate,
        end: GeoCoordinate,
    ): Double {
        val referenceLatitudeRadians = Math.toRadians(
            (point.latitude + start.latitude + end.latitude) / 3.0,
        )
        fun GeoCoordinate.toMeters(): Pair<Double, Double> =
            longitude * METERS_PER_LONGITUDE_DEGREE * cos(referenceLatitudeRadians) to
                latitude * METERS_PER_LATITUDE_DEGREE

        val (px, py) = point.toMeters()
        val (sx, sy) = start.toMeters()
        val (ex, ey) = end.toMeters()
        val dx = ex - sx
        val dy = ey - sy
        val segmentLengthSquared = dx * dx + dy * dy
        if (segmentLengthSquared == 0.0) {
            return sqrt((px - sx) * (px - sx) + (py - sy) * (py - sy))
        }

        val projection = (((px - sx) * dx + (py - sy) * dy) / segmentLengthSquared)
            .coerceIn(0.0, 1.0)
        val closestX = sx + projection * dx
        val closestY = sy + projection * dy
        return sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY))
    }

    private const val DEFAULT_TOLERANCE_METERS = 1.5
    private const val METERS_PER_LATITUDE_DEGREE = 110_540.0
    private const val METERS_PER_LONGITUDE_DEGREE = 111_320.0
}
