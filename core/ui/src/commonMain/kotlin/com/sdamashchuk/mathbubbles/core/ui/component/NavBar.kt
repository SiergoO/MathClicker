package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.resources.Res
import com.sdamashchuk.mathbubbles.core.ui.resources.ic_back
import com.sdamashchuk.mathbubbles.core.ui.resources.nav_bar_back
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val NavBarHeight = 56.dp

@Composable
fun NavBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    require(title == null || titleContent == null) {
        "NavBar takes either title or titleContent, not both"
    }
    val resolvedTitleContent =
        titleContent ?: title?.let { text ->
            @Composable {
                Text(
                    text = text,
                    style = MaterialTheme.typography.h2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

    Box(modifier = modifier.fillMaxWidth().height(NavBarHeight)) {
        if (onBack != null) {
            NavBarIconButton(
                painter = painterResource(Res.drawable.ic_back),
                contentDescription = stringResource(Res.string.nav_bar_back),
                onClick = onBack,
                iconAlignment = Alignment.CenterStart,
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }
        if (resolvedTitleContent != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = NavBarIconButtonTouchTarget),
            ) {
                resolvedTitleContent()
            }
        }
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            content = actions,
        )
    }
}
