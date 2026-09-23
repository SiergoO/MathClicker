package com.sdamashchuk.matharcade.core.ui.theme

import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.ui.resources.Res
import com.sdamashchuk.matharcade.core.ui.resources.quicksand_bold
import com.sdamashchuk.matharcade.core.ui.resources.quicksand_medium
import com.sdamashchuk.matharcade.core.ui.resources.quicksand_regular
import org.jetbrains.compose.resources.Font

// A function, not a val: CMP's Font(FontResource) loader is @Composable, unlike the Android-only
// Font(R.font.id) it replaces, so QuickSand can no longer be built as a top-level constant.
@Composable
fun Typography(): Typography {
    val quickSand =
        FontFamily(
            Font(Res.font.quicksand_regular, FontWeight.Normal),
            Font(Res.font.quicksand_medium, FontWeight.Medium),
            Font(Res.font.quicksand_bold, FontWeight.Bold),
        )

    return Typography(
        h1 =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
            ),
        h2 =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
        body1 =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
            ),
        body2 =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
            ),
        button =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        caption =
            TextStyle(
                fontFamily = quickSand,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
            ),
    )
}
