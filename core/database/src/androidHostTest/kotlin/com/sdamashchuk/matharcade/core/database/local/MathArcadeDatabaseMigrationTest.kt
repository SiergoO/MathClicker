package com.sdamashchuk.matharcade.core.database.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdamashchuk.matharcade.core.database.dao.FieldDao
import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.OperationSign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MathArcadeDatabaseMigrationTest {
    // Hand-written rather than Schema.create(), because Schema.create() already targets the
    // current (post-migration) version. This is the exact CREATE TABLE text Room and the
    // pre-migration SQLDelight schema both shipped at user_version 1 — what every existing
    // install is actually running today.
    private val version1FieldTable =
        "CREATE TABLE `field` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `level` INTEGER NOT NULL, " +
            "`score` INTEGER NOT NULL, `lifeCount` INTEGER NOT NULL, `bonusMultiplier` INTEGER NOT NULL, " +
            "`currentOperationSign` TEXT NOT NULL, `currentOperationDigit` INTEGER NOT NULL, " +
            "`nextOperationSign` TEXT NOT NULL, `nextOperationDigit` INTEGER NOT NULL, " +
            "`gameColumnWidthPx` INTEGER NOT NULL, `gameColumnHeightPx` INTEGER NOT NULL, `isClosed` INTEGER NOT NULL)"

    @Test
    fun `migrating from version 1 to 2 leaves an existing row untouched`() =
        runTest {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            driver.execute(null, "PRAGMA user_version = 1", 0)
            driver.execute(null, version1FieldTable, 0)
            driver.execute(
                null,
                "INSERT INTO field (id, level, score, lifeCount, bonusMultiplier, currentOperationSign, " +
                    "currentOperationDigit, nextOperationSign, nextOperationDigit, gameColumnWidthPx, " +
                    "gameColumnHeightPx, isClosed) VALUES (1, 4, 120, 2, 3, '${OperationSign.SUBTRACTION.sign}', 7, " +
                    "'${OperationSign.DIVISION.sign}', 9, 640, 480, 1)",
                0,
            )

            MathArcadeDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = 2)

            assertEquals(2L, MathArcadeDatabase.Schema.version)
            val database = MathArcadeDatabase(driver)
            val restored = FieldDao(database.fieldQueries, Dispatchers.Unconfined).getFieldById(1)
            assertEquals(
                Field(
                    id = 1,
                    level = 4,
                    score = 120,
                    lifeCount = 2,
                    bonusMultiplier = 3,
                    currentOperationSign = OperationSign.SUBTRACTION,
                    currentOperationDigit = 7,
                    nextOperationSign = OperationSign.DIVISION,
                    nextOperationDigit = 9,
                    gameColumnWidthPx = 640,
                    gameColumnHeightPx = 480,
                    isClosed = true,
                ),
                restored,
            )
        }
}
