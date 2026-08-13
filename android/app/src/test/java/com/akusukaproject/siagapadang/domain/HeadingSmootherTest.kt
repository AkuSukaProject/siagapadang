package com.akusukaproject.siagapadang.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeadingSmootherTest {
    @Test
    fun `smooths across north using shortest rotation`() {
        val smoother = HeadingSmoother(smoothingFactor = 0.5f)

        assertEquals(350f, smoother.update(350f), 0.001f)
        val smoothed = smoother.update(10f)

        assertTrue(smoothed < 1f || smoothed > 359f)
    }

    @Test
    fun `normalizes negative headings`() {
        val smoother = HeadingSmoother(smoothingFactor = 1f)

        assertEquals(350f, smoother.update(-10f), 0.001f)
    }
}
