package com.sdomashchuk.mathclicker.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sdomashchuk.mathclicker.ui.theme.Typography

@Composable
fun MathClickerDialog(
    dialogState: MutableState<Boolean>,
    headerText: String,
    bodyText: String,
    positiveButtonText: String,
    negativeButtonText: String? = null
) {

    if (dialogState.value) {
        Dialog(
            onDismissRequest = { dialogState.value = false },
            content = {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Header(headerText)
                        Body(
                            Modifier.weight(1f),
                            bodyText
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (negativeButtonText != null) {
                                Button(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(20.dp, 0.dp, 10.dp, 20.dp),
                                    onClick = { dialogState.value = false },
                                ) {
                                    Text(text = positiveButtonText)
                                }
                                Button(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(10.dp, 0.dp, 20.dp, 20.dp),
                                    onClick = { dialogState.value = false },
                                ) {
                                    Text(text = negativeButtonText)
                                }
                            } else {
                                Button(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(20.dp, 0.dp, 20.dp, 20.dp),
                                    onClick = { dialogState.value = false },
                                ) {
                                    Text(text = positiveButtonText)
                                }
                            }
                        }
                    }
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}

@Composable
private fun Header(title: String) {
    Column {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(text = title, style = Typography.h1)
        }
        Divider(color = Color.DarkGray, thickness = 1.dp)
    }
}

@Composable
private fun Body(modifier: Modifier = Modifier, bodyText: String) {
    Column(
        modifier = modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = bodyText,
            style = Typography.body1
        )
    }
}