package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TargetsDaoDecodingTest {
    private val fixture = DaoFixture()

    private suspend fun stored(
        column: String,
        sqlLiteral: String,
    ): Target {
        fixture.repository.insertField(Field(id = 1))
        fixture.targetsDao.refreshTargets(
            listOf(Target(id = 1, relatedFieldId = 1, columnId = 2, value = 3, appearsAtMs = 0, finishesAtMs = 10)),
        )
        fixture.setTargetColumn(column, sqlLiteral)
        return fixture.targetsDao.getTargets().single()
    }

    @Test
    fun `a target value past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MAX_VALUE, stored("value", "5000000000").value)
        }

    @Test
    fun `a target value below the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MIN_VALUE, stored("value", "-5000000000").value)
        }

    @Test
    fun `a column id past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MAX_VALUE, stored("columnId", "5000000000").columnId)
        }

    @Test
    fun `a related field id past the Int range is clamped instead of wrapping`() =
        runTest {
            assertEquals(Int.MAX_VALUE, stored("relatedFieldId", "5000000000").relatedFieldId)
        }
}
