package com.sdamashchuk.matharcade.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun MathArcadeDialog(
    headerText: String,
    bodyText: String,
    onDismiss: () -> Unit,
    positiveButtonText: String,
    onPositive: () -> Unit,
    negativeButtonText: String? = null,
    onNegative: () -> Unit = onDismiss,
) {
    Dialog(
        onDismissRequest = onDismiss,
        content = {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Header(headerText)
                    Body(
                        Modifier.weight(1f, fill = false),
                        bodyText,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (negativeButtonText != null) {
                            Button(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(20.dp, 0.dp, 10.dp, 20.dp),
                                onClick = onPositive,
                            ) {
                                Text(text = positiveButtonText)
                            }
                            Button(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(10.dp, 0.dp, 20.dp, 20.dp),
                                onClick = onNegative,
                            ) {
                                Text(text = negativeButtonText)
                            }
                        } else {
                            Button(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(20.dp, 0.dp, 20.dp, 20.dp),
                                onClick = onPositive,
                            ) {
                                Text(text = positiveButtonText)
                            }
                        }
                    }
                }
            }
        },
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    )
}
