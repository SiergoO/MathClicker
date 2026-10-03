package com.sdamashchuk.mathbubbles.core.database.repository

/**
 * Thrown by [GameRepository] when the underlying store fails, whatever driver sits below it.
 */
class PersistenceException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
