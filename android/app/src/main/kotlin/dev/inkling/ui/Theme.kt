package dev.inkling.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.inkling.R

/** Kaleido 3 palette from the prototype. Muted on purpose; color e-ink is dim. */
object InklingColors {
    val Paper = Color(0xFFEDEBE6)
    val Paper2 = Color(0xFFDCD9D2)
    val Ink = Color(0xFF17160F)
    val Ink2 = Color(0xFF5A574F)
    val Ink3 = Color(0xFF9A968C)
    val Teal = Color(0xFF7AA59F)
    val Ochre = Color(0xFFC9A66B)
    val Rust = Color(0xFFB8776A)
    val Moss = Color(0xFF8FA574)
}

val Andika = FontFamily(
    Font(R.font.andika_regular, FontWeight.Normal),
    Font(R.font.andika_bold, FontWeight.Bold),
)

@Composable
fun InklingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = InklingColors.Ink,
            onPrimary = InklingColors.Paper,
            background = InklingColors.Paper,
            onBackground = InklingColors.Ink,
            surface = InklingColors.Paper,
            onSurface = InklingColors.Ink,
        ),
        typography = Typography(
            bodyLarge = TextStyle(fontFamily = Andika, fontSize = 18.sp),
            titleLarge = TextStyle(fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 30.sp),
        ),
        content = content,
    )
}
