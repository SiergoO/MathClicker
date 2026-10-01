package com.sdamashchuk.mathbubbles.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandler

/**
 * Single canonical way to render a Decompose [ChildStack] in Compose. [animation] is the ordinary
 * transition; when [backHandler] and [onBack] are both supplied it is wrapped in predictive back
 * here, so a caller never writes that wiring itself.
 */
@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun <C : Any, T : Any> ChildStackHost(
    stack: Value<ChildStack<C, T>>,
    modifier: Modifier = Modifier,
    backHandler: BackHandler? = null,
    onBack: (() -> Unit)? = null,
    animation: StackAnimation<C, T> = stackAnimation(),
    content: @Composable (T) -> Unit,
) {
    // Computed here rather than in a default argument for `animation`: a default is skipped the
    // moment a caller passes that parameter, which silently dropped backHandler and onBack at the
    // only call site and left it hand-wiring predictive back — the thing this wrapper exists to
    // absorb.
    val resolved =
        if (backHandler != null && onBack != null) {
            predictiveBackAnimation(
                backHandler = backHandler,
                fallbackAnimation = animation,
                onBack = onBack,
            )
        } else {
            animation
        }

    Children(
        stack = stack,
        modifier = modifier,
        animation = resolved,
    ) { child ->
        content(child.instance)
    }
}
