package com.sdamashchuk.mathbubbles.di

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement

internal class LazySqlDriver(
    open: () -> SqlDriver,
) : SqlDriver {
    private val delegate = lazy(open)

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = delegate.value.executeQuery(identifier, sql, mapper, parameters, binders)

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = delegate.value.execute(identifier, sql, parameters, binders)

    override fun newTransaction(): QueryResult<Transacter.Transaction> = delegate.value.newTransaction()

    override fun currentTransaction(): Transacter.Transaction? = delegate.value.currentTransaction()

    override fun addListener(
        vararg queryKeys: String,
        listener: Query.Listener,
    ) = delegate.value.addListener(*queryKeys, listener = listener)

    override fun removeListener(
        vararg queryKeys: String,
        listener: Query.Listener,
    ) = delegate.value.removeListener(*queryKeys, listener = listener)

    override fun notifyListeners(vararg queryKeys: String) = delegate.value.notifyListeners(*queryKeys)

    override fun close() {
        if (delegate.isInitialized()) delegate.value.close()
    }
}
