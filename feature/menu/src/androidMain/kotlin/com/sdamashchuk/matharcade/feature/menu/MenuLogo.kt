package com.sdamashchuk.matharcade.feature.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdamashchuk.matharcade.core.ui.theme.Accent

private val LOGO_MARK_SIZE = 96.dp
private val LOGO_MARK_GUTTER = 16.dp

// The wordmark is laid out to the width it is actually given rather than to a pinned type size: at
// h1's own 48sp "ARCADE" overflows a 393dp screen once the mark and the 40dp screen padding are
// subtracted, and a plain Text answers that by wrapping - which is how this shipped once already,
// reading MATH / ARCAD / E on a device while every build stayed green. Shrinking to fit is the one
// behaviour that cannot wrap or clip on a narrower phone or at a larger system font scale.
private val WORDMARK_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 20.sp, maxFontSize = 48.sp)

@Composable
fun MenuLogo(modifier: Modifier = Modifier) {
    val description = stringResource(id = R.string.logo_content_description)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_mark),
            contentDescription = null,
            modifier = Modifier.size(LOGO_MARK_SIZE),
        )
        Spacer(modifier = Modifier.width(LOGO_MARK_GUTTER))
        // One BasicText carrying both lines rather than one per line: autoSize resolves a size per
        // text, so two of them would render MATH visibly larger than ARCADE - four characters fit at
        // a size six do not.
        BasicText(
            text =
                stringResource(id = R.string.logo_wordmark_line_1) + "\n" +
                    stringResource(id = R.string.logo_wordmark_line_2),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.h1.copy(color = Accent),
            maxLines = 2,
            autoSize = WORDMARK_AUTO_SIZE,
        )
    }
}
