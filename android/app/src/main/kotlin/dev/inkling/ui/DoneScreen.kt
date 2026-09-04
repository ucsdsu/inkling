package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DoneScreen(appLabel: String, onPickBook: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(InklingColors.Paper).padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$appLabel is done\nfor today.", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 28.sp, textAlign = TextAlign.Center, color = InklingColors.Ink)
        Text("It comes back tomorrow. Want to read one more book?", fontFamily = Andika, fontSize = 17.sp, textAlign = TextAlign.Center, color = InklingColors.Ink2, modifier = Modifier.padding(top = 14.dp, bottom = 24.dp))
        BigButton("Pick a book", filled = true, onClick = onPickBook)
        BigButton("Back", filled = false, onClick = onBack)
    }
}

@Composable
fun BigButton(label: String, filled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth(0.7f).padding(vertical = 5.dp)
            .background(if (filled) InklingColors.Ink else InklingColors.Paper, RoundedCornerShape(10.dp))
            .border(2.dp, InklingColors.Ink, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (filled) InklingColors.Paper else InklingColors.Ink) }
}
