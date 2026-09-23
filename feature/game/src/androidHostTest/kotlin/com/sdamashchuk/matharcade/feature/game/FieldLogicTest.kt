package com.sdamashchuk.matharcade.feature.game

import com.sdamashchuk.matharcade.core.model.GAME_COLUMN_COUNT
import org.junit.Assert.assertEquals
import org.junit.Test

class FieldLogicTest {
    @Test
    fun `calculateGameColumnWidth splits the measured width across four columns`() {
        // Both expectations are literals on purpose. GAME_COLUMN_COUNT is a const val, so deriving
        // the expected width from it compiles to the same bytecode as the production expression -
        // changing the constant moves both sides together and the test can never catch the drift it
        // exists to catch. Pinning the count makes whoever changes it come here; pinning the width
        // then fails if this call site did not move with it.
        assertEquals(4, GAME_COLUMN_COUNT)
        assertEquals(100, calculateGameColumnWidth(403))
    }
}
