package com.sdamashchuk.mathbubbles.di

import android.content.Context
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sdamashchuk.mathbubbles.core.database.local.MathBubblesDatabase
import com.sdamashchuk.mathbubbles.core.model.logging.Logger

// The file name Room used; keeping it lets an existing install open in place.
internal const val DATABASE_NAME = "Database"

private const val INTEGRITY_OK = "ok"
private const val DOWNGRADE_MESSAGE = "Can't downgrade database"

internal fun createRecoveringSqlDriver(
    context: Context,
    logger: Logger,
    name: String = DATABASE_NAME,
    open: (Context, String) -> SqlDriver = ::openDriver,
): SqlDriver =
    try {
        open(context, name)
    } catch (failure: SQLiteException) {
        if (!failure.isRecoverableByRecreation()) throw failure
        logger.error("Could not open the database, recreating it", failure)
        context.deleteDatabase(name)
        open(context, name)
    }

// A full disk, a lock or a failed open can pass on their own, so only damage and a downgrade justify deleting.
private fun SQLiteException.isRecoverableByRecreation(): Boolean =
    this is SQLiteDatabaseCorruptException || message.orEmpty().contains(DOWNGRADE_MESSAGE)

private fun openDriver(
    context: Context,
    name: String,
): SqlDriver {
    val driver = AndroidSqliteDriver(MathBubblesDatabase.Schema, context, name)
    try {
        driver.verifyIntegrity()
    } catch (failure: SQLiteException) {
        driver.close()
        throw failure
    }
    return driver
}

private fun SqlDriver.verifyIntegrity() {
    val verdict =
        executeQuery(
            null,
            "PRAGMA quick_check(1)",
            { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getString(0) else null) },
            0,
        ).value
    if (verdict != INTEGRITY_OK) throw SQLiteDatabaseCorruptException("Integrity check failed: $verdict")
}
