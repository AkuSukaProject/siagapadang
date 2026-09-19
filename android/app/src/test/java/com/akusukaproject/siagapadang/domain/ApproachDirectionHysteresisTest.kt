package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ApproachDirectionHysteresisTest {
    private fun classify(relative: Double, previous: ManeuverType? = null) =
        RouteGuidanceCalculator.classifyApproachDirection(relative, previous)

    @Test
    fun `without history the plain boundaries apply`() {
        assertEquals(ManeuverType.LEFT, classify(-145.0))
        assertEquals(ManeuverType.U_TURN, classify(-155.0))
    }

    @Test
    fun `jitter around the u-turn boundary keeps the previous instruction`() {
        assertEquals(ManeuverType.LEFT, classify(-155.0, previous = ManeuverType.LEFT))
        assertEquals(ManeuverType.U_TURN, classify(-140.0, previous = ManeuverType.U_TURN))
    }

    @Test
    fun `a clear change beyond the margin still switches the instruction`() {
        assertEquals(ManeuverType.U_TURN, classify(-170.0, previous = ManeuverType.LEFT))
        assertEquals(ManeuverType.LEFT, classify(-120.0, previous = ManeuverType.U_TURN))
    }

    @Test
    fun `turning to the other side is never held by hysteresis`() {
        assertEquals(ManeuverType.RIGHT, classify(100.0, previous = ManeuverType.LEFT))
    }

    @Test
    fun `straight is held through small wobble but not a real turn`() {
        assertEquals(ManeuverType.STRAIGHT, classify(30.0, previous = ManeuverType.STRAIGHT))
        assertEquals(ManeuverType.SLIGHT_RIGHT, classify(45.0, previous = ManeuverType.STRAIGHT))
    }
}
