package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldDaoDecodingTest {
    private val fixture = DaoFixture()

    private suspend fun stored(
        column: String,
        sqlLiteral: String,
        field: Field = Field(id = 1),
    ): Field? {
        fixture.repository.insertField(field)
        fixture.setFieldColumn(column, sqlLiteral)
        return fixture.fieldDao.getUnfinishedField()
    }

    @Test
    fun `an unknown current operation sign makes the open field unreadable and logs a warning`() =
        runTest {
            assertNull(stored("currentOperationSign", "'?'"))
            assertEquals(1, fixture.logger.warnings.size)
        }

    @Test
    fun `an unreadable open field is closed so an older open field can be restored`() =
        runTest {
            fixture.repository.insertField(Field(id = 1, score = 10))
            fixture.repository.insertField(Field(id = 2, score = 20))
            fixture.setFieldColumn("currentOperationSign", "'?'", id = 2)

            assertEquals(1, fixture.fieldDao.getUnfinishedField()?.id)
        }

    @Test
    fun `an unreadable open field is closed and nothing resurfaces afterwards`() =
        runTest {
            fixture.repository.insertField(Field(id = 1))
            fixture.setFieldColumn("currentOperationSign", "'?'")

            assertNull(fixture.fieldDao.getUnfinishedField())
            fixture.setFieldColumn("currentOperationSign", "'÷'")
            assertNull(fixture.fieldDao.getUnfinishedField())
        }

    @Test
    fun `an unknown next operation sign makes the open field unreadable and logs a warning`() =
        runTest {
            assertNull(stored("nextOperationSign", "'?'"))
            assertEquals(1, fixture.logger.warnings.size)
        }

    @Test
    fun `an unknown current booster reads as no booster`() =
        runTest {
            val field = stored("currentBooster", "'BOGUS'", Field(id = 1, currentBooster = Booster.FREEZE))
            assertNull(requireNotNull(field).currentBooster)
        }

    @Test
    fun `an unknown next booster reads as no booster`() =
        runTest {
            val field = stored("nextBooster", "'BOGUS'", Field(id = 1, nextBooster = Booster.FREEZE))
            assertNull(requireNotNull(field).nextBooster)
        }

    @Test
    fun `an unknown timed effect booster reads as no effect`() =
        runTest {
            val field = stored("timedEffectBooster", "'BOGUS'", Field(id = 1, timedEffectBooster = Booster.FREEZE))
            assertNull(requireNotNull(field).timedEffectBooster)
        }

    @Test
    fun `unknown names are dropped from the booster stash and the rest is kept`() =
        runTest {
            val field = stored("boosterStash", "'FREEZE,BOGUS,SHIELD'")
            assertEquals(listOf(Booster.FREEZE, Booster.SHIELD), requireNotNull(field).boosterStash)
        }

    @Test
    fun `a level of zero is raised to the first level`() =
        runTest {
            assertEquals(1, requireNotNull(stored("level", "0")).level)
        }

    @Test
    fun `a level past the Int range is capped at the last level instead of wrapping`() =
        runTest {
            assertEquals(999, requireNotNull(stored("level", "5000000000")).level)
        }

    @Test
    fun `a score past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MAX_VALUE, requireNotNull(stored("score", "5000000000")).score)
        }

    @Test
    fun `a negative score is clamped to zero`() =
        runTest {
            assertEquals(0, requireNotNull(stored("score", "-5")).score)
        }

    @Test
    fun `a life count above the starting lives is clamped down`() =
        runTest {
            assertEquals(3, requireNotNull(stored("lifeCount", "99")).lifeCount)
        }

    @Test
    fun `a negative life count is clamped to zero`() =
        runTest {
            assertEquals(0, requireNotNull(stored("lifeCount", "-1")).lifeCount)
        }

    @Test
    fun `a negative booster drop counter is clamped to zero`() =
        runTest {
            assertEquals(0, requireNotNull(stored("boosterDropCounter", "-3")).boosterDropCounter)
        }

    @Test
    fun `a booster drop counter past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MAX_VALUE, requireNotNull(stored("boosterDropCounter", "5000000000")).boosterDropCounter)
        }

    @Test
    fun `a negative timed effect remaining time is clamped to zero`() =
        runTest {
            assertEquals(0, requireNotNull(stored("timedEffectRemainingMs", "-1")).timedEffectRemainingMs)
        }

    @Test
    fun `a timed effect remaining time past the Int range is clamped instead of wrapping`() =
        runTest {
            val field = stored("timedEffectRemainingMs", "5000000000")
            assertEquals(Int.MAX_VALUE, requireNotNull(field).timedEffectRemainingMs)
        }

    @Test
    fun `a timed effect rate above neutral is clamped to neutral`() =
        runTest {
            assertEquals(1.0, requireNotNull(stored("timedEffectRate", "5.0")).timedEffectRate, 0.0)
        }

    @Test
    fun `a timed effect rate below the rewind floor is clamped to it`() =
        runTest {
            assertEquals(-1.0, requireNotNull(stored("timedEffectRate", "-9.0")).timedEffectRate, 0.0)
        }

    @Test
    fun `an infinite timed effect rate is clamped to neutral`() =
        runTest {
            assertEquals(1.0, requireNotNull(stored("timedEffectRate", "1e999")).timedEffectRate, 0.0)
        }

    @Test
    fun `a freeze tint envelope above one is clamped to one`() =
        runTest {
            assertEquals(1.0, requireNotNull(stored("freezeTintEnvelope", "7.5")).freezeTintEnvelope, 0.0)
        }

    @Test
    fun `a negative freeze tint envelope is clamped to zero`() =
        runTest {
            assertEquals(0.0, requireNotNull(stored("freezeTintEnvelope", "-2.0")).freezeTintEnvelope, 0.0)
        }

    @Test
    fun `an ice pick stash index past the stash reads as not armed from a slot`() =
        runTest {
            val field = stored("icePickArmedStashIndex", "5", Field(id = 1, boosterStash = listOf(Booster.FREEZE)))
            assertNull(requireNotNull(field).icePickArmedStashIndex)
        }

    @Test
    fun `a negative ice pick stash index reads as not armed from a slot`() =
        runTest {
            val field = stored("icePickArmedStashIndex", "-1", Field(id = 1, boosterStash = listOf(Booster.FREEZE)))
            assertNull(requireNotNull(field).icePickArmedStashIndex)
        }

    @Test
    fun `an ice pick stash index inside the stash is kept`() =
        runTest {
            val field = stored("icePickArmedStashIndex", "0", Field(id = 1, boosterStash = listOf(Booster.FREEZE)))
            assertEquals(0, requireNotNull(field).icePickArmedStashIndex)
        }

    @Test
    fun `an operation digit past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(
                Int.MAX_VALUE,
                requireNotNull(stored("currentOperationDigit", "5000000000")).currentOperationDigit,
            )
        }

    @Test
    fun `an unreadable closed row is skipped in recent results and the rest is returned`() =
        runTest {
            fixture.repository.insertField(Field(id = 1, score = 10, isClosed = true))
            fixture.repository.insertField(Field(id = 2, score = 20, isClosed = true))
            fixture.setFieldColumn("currentOperationSign", "'?'", id = 2)

            assertEquals(listOf(1), fixture.fieldDao.getRecentClosedFields().map { it.id })
            assertTrue(fixture.logger.warnings.isNotEmpty())
        }

    @Test
    fun `an unreadable best row falls through to the next best`() =
        runTest {
            fixture.repository.insertField(Field(id = 1, score = 10, isClosed = true))
            fixture.repository.insertField(Field(id = 2, score = 20, isClosed = true))
            fixture.setFieldColumn("nextOperationSign", "'?'", id = 2)

            assertEquals(1, requireNotNull(fixture.fieldDao.getBestClosedField()).id)
        }
}
