package com.sdamashchuk.mathbubbles.presentation.splash

import com.arkivanov.decompose.ComponentContext

/**
 * No state of its own — SplashScreen calls [onFinished] once its animation completes.
 */
class SplashComponent(
    componentContext: ComponentContext,
    val onFinished: () -> Unit,
) : ComponentContext by componentContext
