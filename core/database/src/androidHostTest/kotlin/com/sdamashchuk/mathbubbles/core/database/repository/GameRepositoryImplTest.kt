package com.sdamashchuk.mathbubbles.core.database.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdamashchuk.mathbubbles.core.database.dao.FieldDao
import com.sdamashchuk.mathbubbles.core.database.dao.TargetsDao
import com.sdamashchuk.mathbubbles.core.database.local.MathBubblesDatabase
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameRepositoryImplTest {
    // GameRepository dropped getFieldById (MC-37, no production caller); read straight from the
    // DAO for the round-trip assertions below, the same way MathBubblesDatabaseMigrationTest does.
    private lateinit var fieldDao: FieldDao
    private lateinit var repository: GameRepositoryImpl

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MathBubblesDatabase.Schema.create(driver)
        val database = MathBubblesDatabase(driver)
        fieldDao = FieldDao(database.fieldQueries, Dispatchers.Unconfined)
        repository =
            GameRepositoryImpl(
                fieldDao,
                TargetsDao(database.targetsQueries, Dispatchers.Unconfined),
                database.fieldQueries,
                database.targetsQueries,
                Dispatchers.Unconfined,
            )
    }

    // applyCombo never writes past MAX_COMBO_MULTIPLIER - 1 itself, so this only exercises the DAO's
    // own defensive clamp against a row a migration or an external edit left out of range.
    @Test
    fun `a bonusMultiplier past the combo cap is clamped on read`() =
        runTest {
            repository.insertField(Field(bonusMultiplier = 99))

            assertEquals(4, fieldDao.getFieldById(1).bonusMultiplier)
        }

    @Test
    fun `a negative bonusMultiplier is clamped to zero on read`() =
        runTest {
            repository.insertField(Field(bonusMultiplier = -5))

            assertEquals(0, fieldDao.getFieldById(1).bonusMultiplier)
        }

    @Test
    fun `field round trip preserves every column including the boolean flags`() =
        runTest {
            val field =
                Field(
                    level = 4,
                    score = 120,
                    lifeCount = 2,
                    bonusMultiplier = 3,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 7,
                    nextOperationSign = OperationSign.DIVISION,
                    nextOperationDigit = 9,
                    isClosed = true,
                    finishedAt = 1_726_000_000_000L,
                    gameTimeMs = 45_000L,
                )

            repository.insertField(field)

            assertEquals(field.copy(id = 1), fieldDao.getFieldById(1))
        }

    @Test
    fun `field round trip preserves a full stash a next booster action and the drop counter`() =
        runTest {
            val field =
                Field(
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 2,
                    currentBooster = Booster.SHIELD,
                    nextBooster = Booster.FREEZE,
                    boosterStash = listOf(Booster.REWIND, Booster.ICE_PICK, Booster.SHIELD),
                    boosterDropCounter = 11,
                    hasDroppedBoosterThisSession = true,
                )

            repository.insertField(field)

            assertEquals(field.copy(id = 1), fieldDao.getFieldById(1))
        }

    @Test
    fun `getUnfinishedField returns null when every field is closed`() =
        runTest {
            repository.insertField(Field(isClosed = true))

            assertNull(repository.getUnfinishedField())
        }

    @Test
    fun `getUnfinishedField returns the open field among closed ones`() =
        runTest {
            repository.insertField(Field(isClosed = true))
            repository.insertField(Field(isClosed = false, score = 55))

            assertEquals(55, repository.getUnfinishedField()?.score)
        }

    // Two open fields must never throw (executeAsOneOrNull dies on a second row); the newest by
    // id wins because it is the session the player was actually in. M1: ORDER BY id DESC removed -
    // a bare LIMIT 1 on this table scan returns id 1's row instead, failing this assertion. M2:
    // LIMIT 1 removed - executeAsOneOrNull sees two rows and throws before this assertion runs.
    @Test
    fun `getUnfinishedField resolves two open fields to the newest by id`() =
        runTest {
            // The older row carries the HIGHER score deliberately: with both ordered the same way,
            // an ORDER BY score would satisfy this assertion just as well as ORDER BY id, and the
            // test could not tell "newest" from "best".
            repository.insertField(Field(isClosed = false, score = 30))
            repository.insertField(Field(isClosed = false, score = 20))

            assertEquals(20, repository.getUnfinishedField()?.score)
        }

    // The losing field is deliberately left alone rather than closed: folding it into history
    // would fabricate a finish the player never reached. It just sits open, and the test above
    // already proves getUnfinishedField never surfaces it again.
    @Test
    fun `getUnfinishedField leaves the older open field open and untouched`() =
        runTest {
            repository.insertField(Field(isClosed = false, score = 30))
            repository.insertField(Field(isClosed = false, score = 20))

            repository.getUnfinishedField()

            val olderField = fieldDao.getFieldById(1)
            assertEquals(30, olderField.score)
            assertEquals(false, olderField.isClosed)
        }

    // updateField is the only write the game performs during play: GameViewModel persists the
    // field on every change. A crossed pair here is invisible until the next launch restores a
    // session with the wrong numbers in it, which no insert-path test can see.
    @Test
    fun `updateField rewrites every column of an existing row`() =
        runTest {
            repository.insertField(Field(level = 1, score = 0, lifeCount = 3))
            val stored = fieldDao.getFieldById(1)

            val advanced =
                stored.copy(
                    level = 6,
                    score = 410,
                    lifeCount = 1,
                    bonusMultiplier = 4,
                    currentOperationSign = OperationSign.DIVISION,
                    currentOperationDigit = 8,
                    nextOperationSign = OperationSign.SUBTRACTION,
                    nextOperationDigit = 2,
                    isClosed = true,
                    gameTimeMs = 8_500L,
                )
            repository.updateField(advanced)

            assertEquals(advanced, fieldDao.getFieldById(1))
        }

    @Test
    fun `updateField does not cross the current and next booster columns`() =
        runTest {
            repository.insertField(Field())
            val stored = fieldDao.getFieldById(1)

            val advanced =
                stored.copy(
                    currentBooster = Booster.SHIELD,
                    nextBooster = Booster.FREEZE,
                    boosterStash = listOf(Booster.REWIND, Booster.ICE_PICK),
                    boosterDropCounter = 7,
                    hasDroppedBoosterThisSession = true,
                )
            repository.updateField(advanced)

            assertEquals(advanced, fieldDao.getFieldById(1))
        }

    // M1: best computed over the returned page instead of all history. The highest score here is
    // the oldest run, id 1 - it is pushed out of getRecentClosedFields' window by ids 2..8, so a
    // best derived from that window alone would report a lower score than the true best.
    @Test
    fun `getBestClosedField is taken over all history, not just the recent window`() =
        runTest {
            repository.insertField(Field(score = 999, level = 1, isClosed = true))
            (2..8).forEach { repository.insertField(Field(score = it, level = 1, isClosed = true)) }

            assertEquals(999, repository.getBestClosedField()?.score)
            assertEquals(7, repository.getRecentClosedFields().size)
            assertTrue(repository.getRecentClosedFields().none { it.score == 999 })
        }

    // M2: LIMIT 7 dropped. M1 (best over the window) is also re-proven here from the list's own
    // side: 8 closed rows must still cap at exactly 7.
    @Test
    fun `getRecentClosedFields excludes unfinished runs and caps at seven`() =
        runTest {
            repository.insertField(Field(isClosed = false))
            repeat(8) { repository.insertField(Field(isClosed = true)) }

            val recent = repository.getRecentClosedFields()

            assertEquals(7, recent.size)
            assertTrue(recent.all { it.isClosed })
        }

    // M3: ordering reversed (oldest first). Score climbs with id below, so id DESC and score DESC
    // agree here - a wrong direction is caught by the first element's score, not just its id.
    @Test
    fun `getRecentClosedFields orders the most recent run first`() =
        runTest {
            (1..3).forEach { repository.insertField(Field(score = it * 10, isClosed = true)) }

            val recent = repository.getRecentClosedFields()

            assertEquals(listOf(30, 20, 10), recent.map { it.score })
        }

    // Tied scores break on level, pinned rather than left to whatever SQLite's default tie order
    // happens to be.
    @Test
    fun `getBestClosedField breaks a tied score by the higher level`() =
        runTest {
            repository.insertField(Field(score = 100, level = 3, isClosed = true))
            repository.insertField(Field(score = 100, level = 8, isClosed = true))

            assertEquals(8, repository.getBestClosedField()?.level)
        }

    @Test
    fun `getRecentClosedFields and getBestClosedField report nothing for empty history`() =
        runTest {
            assertTrue(repository.getRecentClosedFields().isEmpty())
            assertNull(repository.getBestClosedField())
        }

    // M4: a null date treated as zero rather than unknown. Nothing coerces finishedAt away from
    // null here - default insertField never sets it - and both queries must read it back that way
    // without throwing.
    @Test
    fun `a null finishedAt survives both queries without throwing`() =
        runTest {
            repository.insertField(Field(score = 50, isClosed = true))

            assertNull(repository.getRecentClosedFields().single().finishedAt)
            assertNull(repository.getBestClosedField()?.finishedAt)
        }

    @Test
    fun `updateTargets rewrites each target without crossing columns`() =
        runTest {
            val first = target(id = 1, value = 11, appearsAtMs = 22)
            val second = target(id = 2, value = 33, appearsAtMs = 44)
            repository.refreshTargets(listOf(first, second))

            val moved =
                listOf(
                    first.copy(value = 99, appearsAtMs = 7, isActive = false),
                    second.copy(value = 5, appearsAtMs = 100, isProfitable = true),
                )
            repository.updateTargets(moved)

            assertEquals(moved, repository.getTargets().sortedBy { it.id })
        }

    // Every later level calls Game.createTargets() and hands the collector a brand new id set;
    // refreshTargets (not updateTargets) is what has to carry that set into the DB intact.
    @Test
    fun `refreshTargets replaces a smaller target set with a larger one exactly`() =
        runTest {
            val levelOne = (1..6).map { target(id = it, value = it, appearsAtMs = it.toLong()) }
            repository.refreshTargets(levelOne)

            val levelTwo = (1..9).map { target(id = it, value = it * 10, appearsAtMs = it * 10L) }
            repository.refreshTargets(levelTwo)

            assertEquals(levelTwo.sortedBy { it.id }, repository.getTargets().sortedBy { it.id })
        }

    @Test
    fun `refreshTargets replaces a larger target set with a smaller one, leaving no ghost rows`() =
        runTest {
            val levelOne = (1..10).map { target(id = it, value = it, appearsAtMs = it.toLong()) }
            repository.refreshTargets(levelOne)

            val levelTwo = (1..6).map { target(id = it, value = it * 10, appearsAtMs = it * 10L) }
            repository.refreshTargets(levelTwo)

            assertEquals(levelTwo.sortedBy { it.id }, repository.getTargets().sortedBy { it.id })
        }

    // Proves delete-then-insert is a single transaction: a failure partway through the insert must
    // roll back the delete too, or an interrupted refresh could commit an empty table.
    @Test
    fun `refreshTargets rolls back the delete when the insert fails partway through`() =
        runTest {
            val original =
                listOf(target(id = 1, value = 1, appearsAtMs = 1), target(id = 2, value = 2, appearsAtMs = 2))
            repository.refreshTargets(original)

            val poisoned =
                object : AbstractList<Target>() {
                    override val size = 2

                    override fun get(index: Int): Target =
                        if (index == 1) {
                            throw IllegalStateException("boom")
                        } else {
                            target(id = 3, value = 3, appearsAtMs = 3)
                        }
                }

            var thrown: IllegalStateException? = null
            try {
                repository.refreshTargets(poisoned)
            } catch (e: IllegalStateException) {
                thrown = e
            }

            assertEquals("boom", thrown?.message)
            assertEquals(original, repository.getTargets().sortedBy { it.id })
        }

    // The field and its targets must read back as a consistent pair: nothing short of a single
    // transaction proves that, since two separate calls could always be interrupted between them.
    @Test
    fun `saveFieldAndTargets writes the field and its targets together`() =
        runTest {
            repository.insertField(Field(score = 0, level = 1))
            val stored = fieldDao.getFieldById(1)
            val advanced = stored.copy(score = 77, level = 2)
            val targets = listOf(target(id = 1, value = 5, appearsAtMs = 1), target(id = 2, value = 6, appearsAtMs = 2))

            repository.saveFieldAndTargets(advanced, targets, replaceTargets = true)

            assertEquals(advanced, fieldDao.getFieldById(1))
            assertEquals(targets, repository.getTargets().sortedBy { it.id })
        }

    @Test
    fun `saveFieldAndTargets updates targets in place when replaceTargets is false`() =
        runTest {
            repository.insertField(Field())
            repository.refreshTargets(listOf(target(id = 1, value = 9, appearsAtMs = 1)))
            val stored = fieldDao.getFieldById(1)
            val moved = listOf(target(id = 1, value = 2, appearsAtMs = 1))

            repository.saveFieldAndTargets(stored.copy(score = 5), moved, replaceTargets = false)

            assertEquals(5, fieldDao.getFieldById(1).score)
            assertEquals(moved, repository.getTargets())
        }

    // Proves the field write and the target write share one transaction: a failure partway
    // through the targets half must roll the field update back too, not leave it half-applied.
    @Test
    fun `saveFieldAndTargets rolls back the field update when the target write fails partway through`() =
        runTest {
            repository.insertField(Field(score = 0, level = 1))
            val stored = fieldDao.getFieldById(1)
            val advanced = stored.copy(score = 77, level = 2)

            val poisoned =
                object : AbstractList<Target>() {
                    override val size = 2

                    override fun get(index: Int): Target =
                        if (index == 1) {
                            throw IllegalStateException("boom")
                        } else {
                            target(id = 1, value = 5, appearsAtMs = 1)
                        }
                }

            var thrown: IllegalStateException? = null
            try {
                repository.saveFieldAndTargets(advanced, poisoned, replaceTargets = true)
            } catch (e: IllegalStateException) {
                thrown = e
            }

            assertEquals("boom", thrown?.message)
            assertEquals(stored, fieldDao.getFieldById(1))
            assertTrue(repository.getTargets().isEmpty())
        }

    @Test
    fun `target value survives the value_ rename without swapping columns`() =
        runTest {
            val target =
                Target(
                    id = 1,
                    relatedFieldId = 1,
                    columnId = 2,
                    value = 987,
                    appearsAtMs = 100,
                    finishesAtMs = 2100,
                    isProfitable = false,
                    isActive = true,
                )

            repository.refreshTargets(listOf(target))

            assertEquals(target, repository.getTargets().single())
        }
}

private fun target(
    id: Int,
    value: Int,
    appearsAtMs: Long,
) = Target(
    id = id,
    relatedFieldId = 1,
    columnId = 2,
    value = value,
    appearsAtMs = appearsAtMs,
    finishesAtMs = appearsAtMs + 2000,
    isProfitable = false,
    isActive = true,
)
