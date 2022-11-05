package com.sdomashchuk.mathclicker.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sdomashchuk.mathclicker.R
import com.sdomashchuk.mathclicker.component.MathClickerDialog
import com.sdomashchuk.mathclicker.navigation.Screen
import com.sdomashchuk.mathclicker.ui.theme.MathClickerTheme
import com.sdomashchuk.mathclicker.ui.theme.Red500
import com.sdomashchuk.mathclicker.ui.theme.Shapes

@Composable
fun MenuScreen(
    navController: NavController
) {
    val shouldShowHelpDialog: MutableState<Boolean> = remember {
        mutableStateOf(false)
    }

    if (shouldShowHelpDialog.value) {
        MathClickerDialog(
            shouldShowHelpDialog,
            stringResource(id = R.string.how_to_play_dialog_header),
            stringResource(id = R.string.how_to_play_dialog_body),
            stringResource(id = R.string.how_to_play_dialog_button_positive)
        )
    }

    MathClickerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                IconButton(
                    content = {
                        Image(
                            painter = painterResource(id = R.drawable.ic_help),
                            contentDescription = "Help",
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    onClick = {
                        shouldShowHelpDialog.value = true
                    }
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxSize()
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.padding(40.dp)
                )
                Button(
                    onClick = { navController.navigate(Screen.Main.route) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Red500),
                    modifier = Modifier
                        .width(200.dp)
                        .height(60.dp)
                        .clip(Shapes.large)
                ) {
                    Text(
                        text = "PLAY",
                        fontSize = 28.sp
                    )
                }
            }
        }
    }
}