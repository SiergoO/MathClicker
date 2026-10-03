package com.sdamashchuk.mathbubbles.di

import android.util.Log
import com.sdamashchuk.mathbubbles.core.model.logging.Logger

private const val TAG = "MathBubbles"

class AndroidLogger : Logger {
    override fun warn(
        message: String,
        throwable: Throwable?,
    ) {
        Log.w(TAG, message, throwable)
    }

    override fun error(
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(TAG, message, throwable)
    }
}
