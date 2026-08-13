package com.akusukaproject.siagapadang.domain

class HeadingSmoother(
    private val smoothingFactor: Float = DEFAULT_SMOOTHING_FACTOR,
) {
    private var smoothedHeading: Float? = null

    init {
        require(smoothingFactor in 0f..1f) { "Faktor penghalusan harus antara 0 dan 1" }
    }

    fun update(rawHeadingDegrees: Float): Float {
        val normalizedRaw = normalize(rawHeadingDegrees)
        val previous = smoothedHeading ?: return normalizedRaw.also { smoothedHeading = it }
        val shortestDelta = (normalizedRaw - previous + 540f) % 360f - 180f
        return normalize(previous + shortestDelta * smoothingFactor).also { smoothedHeading = it }
    }

    private fun normalize(degrees: Float): Float = (degrees % 360f + 360f) % 360f

    private companion object {
        const val DEFAULT_SMOOTHING_FACTOR = 0.22f
    }
}
