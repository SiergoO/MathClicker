package com.sdamashchuk.matharcade.feature.game

import org.junit.Assert.assertEquals
import org.junit.Test

private const val FLOAT_DELTA = 0.001f

class LaneConvergenceOffsetXTest {
    @Test
    fun `laneOffsetX returns full spacing at the bottom of the fall`() {
        // Column 0's centre (laneIndexFraction 0.5) on a 100dp column, 4 columns wide: distance
        // from the row's centre is 100 * (0.5 - 2) = -150, unscaled at fallFraction 1.
        assertEquals(-150f, laneOffsetX(laneIndexFraction = 0.5f, columnWidth = 100, fallFraction = 1f), FLOAT_DELTA)
    }

    @Test
    fun `laneOffsetX shrinks the distance from centre to 80 percent at the top of the fall`() {
        assertEquals(-120f, laneOffsetX(laneIndexFraction = 0.5f, columnWidth = 100, fallFraction = 0f), FLOAT_DELTA)
    }

    @Test
    fun `laneOffsetX is symmetric for the mirrored column on the other side`() {
        assertEquals(120f, laneOffsetX(laneIndexFraction = 3.5f, columnWidth = 100, fallFraction = 0f), FLOAT_DELTA)
    }

    @Test
    fun `laneOffsetX leaves the row's own centre boundary untouched by convergence`() {
        // boundaryIndex 2 is the shared boundary between columns 1 and 2, i.e. dead centre - it
        // has nothing to converge toward, at any fall fraction.
        assertEquals(0f, laneOffsetX(laneIndexFraction = 2f, columnWidth = 100, fallFraction = 0f), FLOAT_DELTA)
        assertEquals(0f, laneOffsetX(laneIndexFraction = 2f, columnWidth = 100, fallFraction = 1f), FLOAT_DELTA)
    }
}
