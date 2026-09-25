package com.sdamashchuk.matharcade.core.database.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdamashchuk.matharcade.core.database.dao.FieldDao
import com.sdamashchuk.matharcade.core.database.dao.TargetsDao
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MathArcadeDatabaseMigrationTest {
    // Hand-written rather than Schema.create(), because Schema.create() already targets the
    // current (post-migration) version. This is the exact CREATE TABLE text Room and the
    // pre-migration SQLDelight schema both shipped at user_version 1 - what every existing
    // install is actually running today. 1.sqm is a no-op, so this is also the version 2 shape.
    private val preMigrationFieldTable =
        "CREATE TABLE `field` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `level` INTEGER NOT NULL, " +
            "`score` INTEGER NOT NULL, `lifeCount` INTEGER NOT NULL, `bonusMultiplier` INTEGER NOT NULL, " +
            "`currentOperationSign` TEXT NOT NULL, `currentOperationDigit` INTEGER NOT NULL, " +
            "`nextOperationSign` TEXT NOT NULL, `nextOperationDigit` INTEGER NOT NULL, " +
            "`gameColumnWidthPx` INTEGER NOT NULL, `gameColumnHeightPx` INTEGER NOT NULL, `isClosed` INTEGER NOT NULL)"

    private val preMigrationTargetsTable =
        "CREATE TABLE `targets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `relatedFieldId` INTEGER NOT NULL, " +
            "`columnId` INTEGER NOT NULL, `value` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
            "`appearanceDelayMs` INTEGER NOT NULL, `lifetimeMs` INTEGER NOT NULL, `isProfitable` INTEGER NOT NULL, " +
            "`isVisible` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)"

    private fun insertField(
        driver: JdbcSqliteDriver,
        id: Int,
        gameColumnHeightPx: Int,
        isClosed: Int = 0,
    ) = driver.execute(
        null,
        "INSERT INTO field (id, level, score, lifeCount, bonusMultiplier, currentOperationSign, " +
            "currentOperationDigit, nextOperationSign, nextOperationDigit, gameColumnWidthPx, " +
            "gameColumnHeightPx, isClosed) VALUES ($id, 1, 0, 3, 0, '${OperationSign.DIVISION.sign}', 0, " +
            "'${OperationSign.DIVISION.sign}', 0, 0, $gameColumnHeightPx, $isClosed)",
        0,
    )

    private fun insertTarget(
        driver: JdbcSqliteDriver,
        id: Int,
        position: Int,
        lifetimeMs: Int,
    ) = driver.execute(
        null,
        "INSERT INTO targets (id, relatedFieldId, columnId, value, position, appearanceDelayMs, lifetimeMs, " +
            "isProfitable, isVisible, isActive) VALUES ($id, 1, 0, 5, $position, 0, $lifetimeMs, 1, 1, 1)",
        0,
    )

    // The 1->3 chain: what a Room-era install actually runs through on upgrade. 1.sqm is a no-op,
    // so the only structural change either table sees is 2.sqm.
    @Test
    fun `migrating from version 1 to 3 preserves the field and converts the target's fall position`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 1", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            insertField(driver, id = 1, gameColumnHeightPx = 480, isClosed = 1)
            insertTarget(driver, id = 1, position = 240, lifetimeMs = 1000)

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = MathArcadeDatabase.Schema.version)

            assertEquals(6L, MathArcadeDatabase.Schema.version)
            val database = MathArcadeDatabase(driver)
            val restoredField = FieldDao(database.fieldQueries, Dispatchers.Unconfined).getFieldById(1)
            assertEquals(
                Field(
                    id = 1,
                    level = 1,
                    score = 0,
                    lifeCount = 3,
                    bonusMultiplier = 0,
                    currentOperationSign = OperationSign.DIVISION,
                    currentOperationDigit = 0,
                    nextOperationSign = OperationSign.DIVISION,
                    nextOperationDigit = 0,
                    isClosed = true,
                ),
                restoredField,
            )
            val restoredTarget =
                TargetsDao(database.targetsQueries, Dispatchers.Unconfined).getTargets().single()
            // 240 of a 480px column, halfway, against a 1000ms lifetime: 2.sqm converts that to a
            // fallenMs of 500, and 5.sqm's backfill (gameTimeMs 0 + delay 0 - fallenMs) then reads
            // the same halfway point back as position(0) against the restored gameTimeMs of 0.
            assertEquals(0.5f, restoredTarget.position(0L))
        }

    // The pinned table from the design doc: an exact stored position, a corrupt negative one and an
    // overshoot past the column all land on their documented millisecond value against a real height.
    @Test
    fun `migrating from version 2 to 3 converts each stored pixel position into elapsed fall time`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 2", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            insertField(driver, id = 1, gameColumnHeightPx = 700)
            val positions = listOf(350, 0, 700, -5, 9999)
            positions.forEachIndexed { index, position ->
                insertTarget(driver, id = index + 1, position = position, lifetimeMs = 30000)
            }

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = MathArcadeDatabase.Schema.version)

            val database = MathArcadeDatabase(driver)
            val restored =
                TargetsDao(database.targetsQueries, Dispatchers.Unconfined)
                    .getTargets()
                    .sortedBy { it.id }
            // 5.sqm's backfill is gameTimeMs(0) + delay(0) - fallenMs, so negating appearsAtMs
            // recovers exactly the fallenMs 2.sqm computed; finishesAtMs - appearsAtMs staying at
            // the original 30000ms lifetime for every row proves the backfill never touched it.
            assertEquals(listOf(15000L, 0L, 30000L, 0L, 30000L), restored.map { -it.appearsAtMs })
            assertTrue(restored.all { it.finishesAtMs - it.appearsAtMs == 30000L })
        }

    // field empty while targets is not is reachable - nothing enforces referential integrity between
    // them. The original migration SQL (MAX(h.px, 1) on the outer expression) crashed here with a
    // NOT NULL constraint failure; this is the case that catches a regression back to that form.
    @Test
    fun `migrating from version 2 to 3 with no field rows converts every position to zero`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 2", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            listOf(100, 700, -5, 9999).forEachIndexed { index, position ->
                insertTarget(driver, id = index + 1, position = position, lifetimeMs = 5000)
            }

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = MathArcadeDatabase.Schema.version)

            val database = MathArcadeDatabase(driver)
            val restored = TargetsDao(database.targetsQueries, Dispatchers.Unconfined).getTargets()
            assertTrue(restored.all { it.appearsAtMs == 0L })
        }

    // MC-53: finishedAt did not exist before this version, so an install upgrading straight from
    // 1 to the current schema has nothing to recover it from - null, not a fabricated migration-time
    // stamp, is the only honest value. score and level (what MC-59 needs untouched) are asserted
    // alongside it to prove the new column's ALTER TABLE didn't disturb them.
    @Test
    fun `migrating from version 1 adds a null finishedAt without disturbing existing columns`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 1", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            insertField(driver, id = 1, gameColumnHeightPx = 480, isClosed = 1)

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = MathArcadeDatabase.Schema.version)

            val restoredField =
                FieldDao(
                    MathArcadeDatabase(driver).fieldQueries,
                    Dispatchers.Unconfined,
                ).getFieldById(1)
            assertEquals(1, restoredField.level)
            assertEquals(0, restoredField.score)
            assertTrue(restoredField.isClosed)
            assertNull(restoredField.finishedAt)
        }

    // MC-71: gameTimeMs did not exist before this version, so an install upgrading straight from 1
    // has nothing to recover it from - 0 is the only honest default (see 4.sqm), the same reasoning
    // MC-53 already applied to finishedAt, just non-null here instead of left null. score and level
    // are asserted alongside it to prove the ALTER TABLE didn't disturb the columns already there.
    @Test
    fun `migrating from version 1 adds a zero gameTimeMs without disturbing existing columns`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 1", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            insertField(driver, id = 1, gameColumnHeightPx = 480, isClosed = 1)

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = MathArcadeDatabase.Schema.version)

            val restoredField =
                FieldDao(
                    MathArcadeDatabase(driver).fieldQueries,
                    Dispatchers.Unconfined,
                ).getFieldById(1)
            assertEquals(1, restoredField.level)
            assertEquals(0, restoredField.score)
            assertTrue(restoredField.isClosed)
            assertEquals(0L, restoredField.gameTimeMs)
        }

    // relatedFieldId is never queried, so several field rows with differing geometry all feed the
    // same MAX(gameColumnHeightPx) into every target's conversion regardless of which field it
    // belongs to. The exact number that produces depends on which row's height wins - only the
    // invariant (the result can never exceed the target's own lifetime) is asserted here.
    @Test
    fun `migrating from version 2 to 3 with several field rows keeps the conversion within the target's lifetime`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 2", 0)
            driver.execute(null, preMigrationFieldTable, 0)
            driver.execute(null, preMigrationTargetsTable, 0)
            insertField(driver, id = 1, gameColumnHeightPx = 500)
            insertField(driver, id = 2, gameColumnHeightPx = 900)
            insertField(driver, id = 3, gameColumnHeightPx = 1200)
            insertTarget(driver, id = 1, position = 600, lifetimeMs = 20000)

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 2, newVersion = MathArcadeDatabase.Schema.version)

            val database = MathArcadeDatabase(driver)
            val restored = TargetsDao(database.targetsQueries, Dispatchers.Unconfined).getTargets().single()
            assertTrue(restored.position(0L) in 0f..1f)
        }
}
