package com.sdomashchuk.mathclicker.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.OperationSign
import com.sdomashchuk.mathclicker.core.model.Target
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.database.local.MathClickerDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GameRepositoryImplTest {
    private lateinit var repository: GameRepositoryImpl

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MathClickerDatabase.Schema.create(driver)
        val database = MathClickerDatabase(driver)
        repository = GameRepositoryImpl(FieldDao(database.fieldQueries), TargetsDao(database.targetsQueries))
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
                    gameColumnWidthPx = 640,
                    gameColumnHeightPx = 480,
                    isClosed = true,
                )

            repository.insertField(field)

            assertEquals(field.copy(id = 1), repository.getFieldById(1))
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

    // updateField is the only write the game performs during play: GameViewModel persists the
    // field on every change. A crossed pair here is invisible until the next launch restores a
    // session with the wrong numbers in it, which no insert-path test can see.
    @Test
    fun `updateField rewrites every column of an existing row`() =
        runTest {
            repository.insertField(Field(level = 1, score = 0, lifeCount = 3))
            val stored = repository.getFieldById(1)

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
                    gameColumnWidthPx = 1080,
                    gameColumnHeightPx = 1920,
                    isClosed = true,
                )
            repository.updateField(advanced)

            assertEquals(advanced, repository.getFieldById(1))
        }

    @Test
    fun `updateTargets rewrites each target without crossing columns`() =
        runTest {
            val first = target(id = 1, value = 11, position = 22)
            val second = target(id = 2, value = 33, position = 44)
            repository.insertTargets(listOf(first, second))

            val moved =
                listOf(
                    first.copy(value = 99, position = 7, isVisible = false, isActive = false),
                    second.copy(value = 5, position = 100, isProfitable = true),
                )
            repository.updateTargets(moved)

            assertEquals(moved, repository.getTargets().sortedBy { it.id })
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
                    position = 3,
                    appearanceDelayMs = 100,
                    lifetimeMs = 2000,
                    isProfitable = false,
                    isVisible = true,
                    isActive = true,
                )

            repository.insertTarget(target)

            assertEquals(target, repository.getTargets().single())
        }
}

private fun target(
    id: Int,
    value: Int,
    position: Int,
) = Target(
    id = id,
    relatedFieldId = 1,
    columnId = 2,
    value = value,
    position = position,
    appearanceDelayMs = 100,
    lifetimeMs = 2000,
    isProfitable = false,
    isVisible = true,
    isActive = true,
)
