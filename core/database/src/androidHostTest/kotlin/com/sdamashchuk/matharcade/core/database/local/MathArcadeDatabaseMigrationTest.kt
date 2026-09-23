package com.sdamashchuk.matharcade.core.database.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdamashchuk.matharcade.core.database.dao.FieldDao
import com.sdamashchuk.matharcade.core.database.dao.TargetsDao
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

            assertEquals(3L, MathArcadeDatabase.Schema.version)
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
            // 240 of a 480px column, halfway, against a 1000ms lifetime.
            assertEquals(500, restoredTarget.fallenMs)
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
            assertEquals(listOf(15000, 0, 30000, 0, 30000), restored.map { it.fallenMs })
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
            assertTrue(restored.all { it.fallenMs == 0 })
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
            assertTrue(restored.fallenMs in 0..20000)
        }
}
