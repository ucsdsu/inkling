package dev.inkling.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.R

/** Icons stay square at this edge; a done tile fades its icon to match the greyed ink. */
private val IconSize = 40.dp
private const val DoneIconAlpha = 0.35f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KidHome(state: HomeState, onOpen: (Tile) -> Unit, onRead: () -> Unit, onGearLongPress: () -> Unit) {
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(1.dp))
            Box(
                Modifier.size(28.dp).border(1.5.dp, InklingColors.Ink3, CircleShape)
                    .combinedClickable(onClick = {}, onLongClick = onGearLongPress),
                contentAlignment = Alignment.Center,
            ) { Text("⚙", fontSize = 13.sp, color = InklingColors.Ink2) }
        }
        Spacer(Modifier.height(12.dp))
        Text("Hi, ${state.childName}.", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 32.sp, color = InklingColors.Ink)
        Text("${state.booksToday} books today.", fontFamily = Andika, fontSize = 16.sp, color = InklingColors.Ink2)
        Spacer(Modifier.height(18.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item(span = { GridItemSpan(2) }) {
                TileCard(label = "Read", fraction = null, done = false, icon = null, glyph = painterResource(R.drawable.ic_read), onClick = onRead)
            }
            items(state.tiles, key = { it.packageName }) { t ->
                TileCard(label = t.label, fraction = t.fraction, done = t.done, icon = t.icon, glyph = null, onClick = { onOpen(t) })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TileCard(
    label: String,
    fraction: Float?,
    done: Boolean,
    icon: ImageBitmap?,
    glyph: Painter?,
    onClick: () -> Unit,
) {
    val ink = if (done) InklingColors.Ink3 else InklingColors.Ink
    val iconAlpha = if (done) DoneIconAlpha else 1f
    Column(
        // The icon row added 46.dp of content, so every tile needs the taller box or
        // "Done for today" gets clipped off the bottom.
        Modifier.fillMaxWidth().height(132.dp)
            .border(2.dp, ink, RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            when {
                glyph != null -> Icon(glyph, contentDescription = null, tint = ink, modifier = Modifier.size(IconSize))
                icon != null -> Image(icon, contentDescription = null, alpha = iconAlpha, modifier = Modifier.size(IconSize))
                // No icon to show (app uninstalled since the last refresh). Hold the space so the
                // labels across the grid still line up.
                else -> Spacer(Modifier.size(IconSize))
            }
            Spacer(Modifier.height(6.dp))
            Text(label, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = ink)
            if (done) Text("Done for today", fontFamily = Andika, fontSize = 12.sp, color = ink)
        }
        if (fraction != null) {
            Box(Modifier.fillMaxWidth().height(7.dp).background(InklingColors.Paper2, RoundedCornerShape(4.dp))) {
                Box(Modifier.fillMaxWidth(if (done) 1f else fraction).height(7.dp).background(ink, RoundedCornerShape(4.dp)))
            }
        }
    }
}
