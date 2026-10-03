package com.sdamashchuk.mathbubbles.core.model.logging

object NoOpLogger : Logger {
    override fun warn(
        message: String,
        throwable: Throwable?,
    ) = Unit

    override fun error(
        message: String,
        throwable: Throwable?,
    ) = Unit
}
