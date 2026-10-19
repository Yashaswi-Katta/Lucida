package com.lucida.studio.domain.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OneEuroFilterTest {
    @Test
    fun firstSamplePassesThrough() {
        val filter = OneEuroFilter1D()
        assertEquals(42f, filter.filter(42f, 0.0), 0f)
    }

    @Test
    fun outputIsSmoothedAndRespondsToMotion() {
        val filter = OneEuroFilter1D(minCutoff = 1.2f, beta = 0.05f, dCutoff = 1f)
        val start = filter.filter(10f, 0.0)
        val moving = filter.filter(110f, 0.02)
        assertTrue(moving > start)
        assertTrue(moving < 110f)
        val settled = filter.filter(110f, 0.04)
        assertTrue(settled > moving)
        assertTrue(settled <= 110f)
    }

    @Test
    fun duplicateOrReverseTimestampsDoNotDivideByZero() {
        val filter = OneEuroFilter1D()
        assertEquals(1f, filter.filter(1f, 1.0), 0f)
        assertEquals(1f, filter.filter(3f, 1.0), 0f)
        assertEquals(1f, filter.filter(4f, 0.5), 0f)
        assertEquals(1f, filter.filter(Float.NaN, Double.NaN), 0f)
    }

    @Test
    fun resetStartsANewTrack() {
        val filter = OneEuroFilter1D()
        filter.filter(50f, 1.0)
        filter.reset()
        assertEquals(-2f, filter.filter(-2f, 0.0), 0f)
    }

    @Test
    fun twoAxisFilterReturnsComposeFriendlyPair() {
        val filter = OneEuroFilter2D()
        val first = filter.filter(12f, 24f, 0.0)
        assertEquals(12f, first.first, 0f)
        assertEquals(24f, first.second, 0f)
    }
}
