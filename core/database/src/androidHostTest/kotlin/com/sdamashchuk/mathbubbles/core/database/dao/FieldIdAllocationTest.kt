package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.model.Field
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FieldIdAllocationTest {
    private val fixture = DaoFixture()

    @Test
    fun `the next id is one past the highest stored id when ids have gaps`() =
        runTest {
            listOf(1, 2, 4).forEach { fixture.repository.insertField(Field(id = it, score = it * 10)) }

            assertEquals(5, fixture.repository.getNextFieldId())
        }

    @Test
    fun `the next id on an empty table is one`() =
        runTest {
            assertEquals(1, fixture.repository.getNextFieldId())
        }

    @Test
    fun `a new run with ids one two and four stored persists without touching run four`() =
        runTest {
            listOf(1, 2, 4).forEach { fixture.repository.insertField(Field(id = it, score = it * 10)) }

            val newId = fixture.repository.getNextFieldId()
            fixture.repository.insertField(Field(id = newId, score = 999))

            assertEquals(40, fixture.fieldDao.getFieldById(4).score)
            assertEquals(999, fixture.fieldDao.getFieldById(5).score)
        }

    @Test
    fun `inserting a field under an id that is already stored fails instead of being ignored`() =
        runTest {
            fixture.repository.insertField(Field(id = 4, score = 40))

            val failure = runCatching { fixture.repository.insertField(Field(id = 4, score = 1)) }.exceptionOrNull()

            assertNotNull(failure)
            assertEquals(40, fixture.fieldDao.getFieldById(4).score)
        }
}
