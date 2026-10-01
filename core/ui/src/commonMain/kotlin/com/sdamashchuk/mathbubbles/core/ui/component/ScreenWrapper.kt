package com.sdamashchuk.mathbubbles.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdamashchuk.mathbubbles.core.ui.theme.MathBubblesTheme

private val ScreenHorizontalPadding = 20.dp

@Composable
fun ScreenWrapper(
    modifier: Modifier = Modifier,
    edgeToEdgeContent: Boolean = false,
    topBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    MathBubblesTheme {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding)) {
                topBar()
            }
            Column(
                modifier =
                    if (edgeToEdgeContent) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.fillMaxWidth().padding(horizontal = ScreenHorizontalPadding)
                    },
                content = content,
            )
        }
    }
}
