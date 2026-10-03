package com.sdamashchuk.mathbubbles.core.database.dao

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sdamashchuk.mathbubbles.core.database.local.MathBubblesDatabase
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepositoryImpl
import kotlinx.coroutines.Dispatchers

class DaoFixture {
    val logger = RecordingLogger()
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    private val database: MathBubblesDatabase

    val fieldDao: FieldDao
    val targetsDao: TargetsDao
    val repository: GameRepositoryImpl

    init {
        MathBubblesDatabase.Schema.create(driver)
        database = MathBubblesDatabase(driver)
        fieldDao = FieldDao(database.fieldQueries, Dispatchers.Unconfined, logger)
        targetsDao = TargetsDao(database.targetsQueries, Dispatchers.Unconfined)
        repository =
            GameRepositoryImpl(
                fieldDao,
                targetsDao,
                database.fieldQueries,
                database.targetsQueries,
                Dispatchers.Unconfined,
            )
    }

    fun setFieldColumn(
        column: String,
        sqlLiteral: String,
        id: Int = 1,
    ) = driver.execute(null, "UPDATE field SET $column = $sqlLiteral WHERE id = $id", 0)

    fun setTargetColumn(
        column: String,
        sqlLiteral: String,
        id: Int = 1,
    ) = driver.execute(null, "UPDATE targets SET $column = $sqlLiteral WHERE id = $id", 0)
}
