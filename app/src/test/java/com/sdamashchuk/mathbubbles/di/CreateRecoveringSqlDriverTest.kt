package com.sdamashchuk.mathbubbles.di

import android.content.Context
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import app.cash.sqldelight.db.SqlDriver
import com.sdamashchuk.mathbubbles.core.database.local.MathBubblesDatabase
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.RandomAccessFile

private const val TEST_DATABASE = "recovery-test"
private const val GARBAGE_BYTE_COUNT = 4096
private const val PAGE_SIZE_BYTES = 4096

@RunWith(RobolectricTestRunner::class)
class CreateRecoveringSqlDriverTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val errors = mutableListOf<String>()
    private val logger =
        object : Logger {
            override fun warn(
                message: String,
                throwable: Throwable?,
            ) = Unit

            override fun error(
                message: String,
                throwable: Throwable?,
            ) {
                errors += message
            }
        }
    private var driver: SqlDriver? = null

    @Before
    fun setUp() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        driver?.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    private fun open(): MathBubblesDatabase {
        driver?.close()
        return MathBubblesDatabase(createRecoveringSqlDriver(context, logger, TEST_DATABASE).also { driver = it })
    }

    private fun databaseFile() = context.getDatabasePath(TEST_DATABASE)

    @Test
    fun `a garbage file at the database path opens as an empty usable database`() {
        databaseFile().parentFile?.mkdirs()
        databaseFile().writeBytes(ByteArray(GARBAGE_BYTE_COUNT) { 'x'.code.toByte() })

        val database = open()

        assertEquals(0L, database.fieldQueries.getMaxFieldId().executeAsOne())
    }

    @Test
    fun `a database with a damaged table page opens as an empty usable database`() {
        open().fieldQueries.getMaxFieldId().executeAsOne()
        driver?.close()
        RandomAccessFile(databaseFile(), "rw").use {
            it.seek(PAGE_SIZE_BYTES.toLong())
            it.write(ByteArray(PAGE_SIZE_BYTES) { 0xFF.toByte() })
        }

        val database = open()

        assertEquals(0L, database.fieldQueries.getMaxFieldId().executeAsOne())
        assertEquals(1, errors.size)
    }

    @Test
    fun `a database written by a newer schema version opens as an empty usable database`() {
        databaseFile().parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(databaseFile(), null).use {
            it.version = MathBubblesDatabase.Schema.version.toInt() + 1
        }

        val database = open()

        assertEquals(0L, database.fieldQueries.getMaxFieldId().executeAsOne())
        assertEquals(1, errors.size)
    }

    @Test
    fun `a healthy database keeps its rows and logs nothing`() {
        open().fieldQueries.insertField(
            id = 5,
            level = 1,
            score = 0,
            lifeCount = 3,
            bonusMultiplier = 0,
            currentOperationSign = "÷",
            currentOperationDigit = 2,
            nextOperationSign = "÷",
            nextOperationDigit = 2,
            isClosed = false,
            finishedAt = null,
            gameTimeMs = 0,
            currentBooster = null,
            nextBooster = null,
            boosterStash = "",
            boosterDropCounter = 0,
            hasDroppedBoosterThisSession = false,
            timedEffectBooster = null,
            timedEffectRemainingMs = 0,
            timedEffectRate = 1.0,
            freezeTintEnvelope = 0.0,
            icePickArmedFireButton = false,
            icePickArmedStashIndex = null,
            shieldActive = false,
        )

        val reopened = open()

        assertEquals(5L, reopened.fieldQueries.getMaxFieldId().executeAsOne())
        assertTrue(errors.isEmpty())
    }

    private fun assertOpenFailureLeavesFileUntouched(failure: SQLiteException) {
        databaseFile().parentFile?.mkdirs()
        val original = ByteArray(GARBAGE_BYTE_COUNT) { 'y'.code.toByte() }
        databaseFile().writeBytes(original)
        var attempts = 0

        val thrown =
            runCatching {
                createRecoveringSqlDriver(context, logger, TEST_DATABASE) { _, _ ->
                    attempts++
                    throw failure
                }
            }.exceptionOrNull()

        assertSame(failure, thrown)
        assertEquals(1, attempts)
        assertTrue(original.contentEquals(databaseFile().readBytes()))
    }

    @Test
    fun `a full disk at open is rethrown and the database file is left alone`() =
        assertOpenFailureLeavesFileUntouched(SQLiteFullException("disk full"))

    @Test
    fun `a locked database at open is rethrown and the database file is left alone`() =
        assertOpenFailureLeavesFileUntouched(SQLiteDatabaseLockedException("locked"))

    @Test
    fun `a database that cannot be opened is rethrown and the database file is left alone`() =
        assertOpenFailureLeavesFileUntouched(SQLiteCantOpenDatabaseException("cannot open"))

    @Test
    fun `the lazy driver opens the database only on first use`() {
        var opens = 0
        val lazyDriver = LazySqlDriver { opens++.let { createRecoveringSqlDriver(context, logger, TEST_DATABASE) } }
        driver = lazyDriver
        assertEquals(0, opens)

        MathBubblesDatabase(lazyDriver).fieldQueries.getMaxFieldId().executeAsOne()

        assertEquals(1, opens)
    }
}
