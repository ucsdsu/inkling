package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.core.Tag

/** One cover color per stage, in shelf order. Muted, because color e-ink is dim. */
private val CoverColors = listOf(
    InklingColors.Rust, InklingColors.Teal, InklingColors.Ochre,
    InklingColors.Moss, InklingColors.Paper2, InklingColors.Teal,
)

/** The one word the shelf gives a book, and the color it wears. */
private fun tagText(t: Tag): String = when (t) {
    Tag.TRY_IT -> "Try it"
    Tag.JUST_RIGHT -> "Just right"
    Tag.EASY -> "Easy"
    Tag.STRETCH -> "Stretch"
}

private fun tagColor(t: Tag): Color = when (t) {
    Tag.TRY_IT -> InklingColors.Teal
    Tag.JUST_RIGHT -> InklingColors.Moss
    Tag.EASY -> InklingColors.Paper2
    Tag.STRETCH -> InklingColors.Ochre
}

@Composable
fun ShelfScreen(rows: List<ShelfRow>, onOpen: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.heightIn(min = TapTarget).clickable(onClick = onBack).padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text("‹ Home", fontFamily = Andika, fontSize = 15.sp, color = InklingColors.Ink2) }
        }
        Spacer(Modifier.height(6.dp))
        Text("Your books", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = InklingColors.Ink)
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(rows, key = { _, r -> r.book.id }) { i, r ->
                BookRow(r, CoverColors[i % CoverColors.size], onOpen)
            }
        }
    }
}

/** The letter on the cover. Every starter title opens with "The", so skip the article. */
private fun coverLetter(title: String): String =
    (title.split(" ").firstOrNull { it.lowercase() !in setOf("the", "a", "an") } ?: title)
        .take(1).uppercase()

@Composable
private fun BookRow(row: ShelfRow, cover: Color, onOpen: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(80.dp)
            .border(1.5.dp, InklingColors.Ink2, RoundedCornerShape(8.dp))
            .clickable { onOpen(row.book.id) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 46.dp, height = 60.dp).background(cover, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) {
            Text(coverLetter(row.book.title), fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = InklingColors.Ink)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.book.title, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InklingColors.Ink)
            Text(row.subtitle, fontFamily = Andika, fontSize = 12.sp, color = InklingColors.Ink2)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.background(tagColor(row.tag), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                tagText(row.tag), fontSize = 11.sp,
                color = if (row.tag == Tag.EASY) InklingColors.Ink2 else InklingColors.Ink,
            )
        }
    }
}
