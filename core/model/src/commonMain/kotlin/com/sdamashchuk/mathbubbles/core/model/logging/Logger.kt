package com.sdamashchuk.mathbubbles.core.model.logging

interface Logger {
    fun warn(
        message: String,
        throwable: Throwable? = null,
    )

    fun error(
        message: String,
        throwable: Throwable? = null,
    )
}
