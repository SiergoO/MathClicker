package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.model.logging.Logger

class RecordingLogger : Logger {
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()

    override fun warn(
        message: String,
        throwable: Throwable?,
    ) {
        warnings += message
    }

    override fun error(
        message: String,
        throwable: Throwable?,
    ) {
        errors += message
    }
}
