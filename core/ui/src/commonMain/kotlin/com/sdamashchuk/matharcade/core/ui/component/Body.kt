package com.sdamashchuk.matharcade.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun Body(
    modifier: Modifier = Modifier,
    bodyText: String,
) {
    Column(
        modifier =
            modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = bodyText,
            style = MaterialTheme.typography.body1,
        )
    }
}
