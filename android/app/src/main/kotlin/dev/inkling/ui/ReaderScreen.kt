package dev.inkling.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.R

/** Kid text on the page. The spec floor is 30sp; the reader gets a little more. */
private val PageTextSize = 32.sp

/**
 * One page of a book, with the tutor underneath it.
 *
 * @param onDebugFake long-press on the mic, debug builds only, to fake a recognition result
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    state: ReaderState,
    onBack: () -> Unit,
    onTurn: (Int) -> Unit,
    onNext: () -> Unit,
    onSpeak: () -> Unit,
    onListen: () -> Unit,
    onStopListening: () -> Unit,
    onReplayChunks: () -> Unit,
    onDebugFake: (() -> Unit)?,
) {
    val book = state.book ?: return
    val line = book.pages[state.page]
    val listening = state.phase == TutorPhase.LISTENING
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.heightIn(min = TapTarget).clickable(onClick = onBack).padding(horizontal = 2.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text("‹ Books", fontFamily = Andika, fontSize = 15.sp, color = InklingColors.Ink2) }
            Text(book.title, fontFamily = Andika, fontSize = 15.sp, color = InklingColors.Ink2)
            Spacer(Modifier.width(1.dp))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f)
                .border(1.5.dp, InklingColors.Ink3, RoundedCornerShape(6.dp)),
        ) {
            Text(
                pageText(line, state),
                fontFamily = Andika, fontSize = PageTextSize, lineHeight = 44.sp, color = InklingColors.Ink,
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Center).padding(18.dp),
            )
            // Invisible page turns: the left and right edges of the page, as in a paper book.
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.38f).align(Alignment.CenterStart).clickable { onTurn(-1) })
            Box(Modifier.fillMaxHeight().fillMaxWidth(0.38f).align(Alignment.CenterEnd).clickable { onTurn(1) })
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.weight(1f).heightIn(min = TapTarget)
                    .background(if (listening) InklingColors.Rust else InklingColors.Ink, RoundedCornerShape(10.dp))
                    .border(2.dp, if (listening) InklingColors.Rust else InklingColors.Ink, RoundedCornerShape(10.dp))
                    .combinedClickable(
                        onClick = { if (listening) onStopListening() else onListen() },
                        onLongClick = onDebugFake,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val ink = if (listening) InklingColors.Ink else InklingColors.Paper
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
                    Text(
                        if (listening) "Listening…" else "I'll read",
                        fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = ink,
                    )
                }
            }
            Box(
                Modifier.weight(1f).heightIn(min = TapTarget)
                    .border(2.dp, InklingColors.Ink, RoundedCornerShape(10.dp))
                    .clickable(onClick = onSpeak),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_speaker), contentDescription = null, tint = InklingColors.Ink, modifier = Modifier.size(20.dp))
                    Text("Read to me", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InklingColors.Ink)
                }
            }
        }
        when (state.phase) {
            TutorPhase.COACH -> CoachCard(state, onReplayChunks)
            TutorPhase.UNCLEAR -> PlainCard("I didn't catch that. Try again.")
            TutorPhase.GOOD -> GoodCard(onNext)
            else -> Unit
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "page ${state.page + 1} of ${book.pages.size}" + if (state.speechMode == "online") " · online speech" else "",
            fontSize = 12.sp, color = InklingColors.Ink3,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
    }
}

/** The line, with the word being spoken bold and the coached word on ochre. */
private fun pageText(line: String, state: ReaderState) = buildAnnotatedString {
    val words = line.split(" ")
    words.forEachIndexed { i, w ->
        if (i > 0) append(" ")
        val missed = state.phase == TutorPhase.COACH &&
            state.missedWord != null &&
            w.filter { it.isLetter() }.lowercase() == state.missedWord.lowercase()
        when {
            missed -> withStyle(SpanStyle(background = InklingColors.Ochre)) { append(w) }
            i == state.speakingWord -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(w) }
            else -> append(w)
        }
    }
}

@Composable
private fun CoachCard(state: ReaderState, onReplay: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Column(
        Modifier.fillMaxWidth().border(2.dp, InklingColors.Ink, RoundedCornerShape(12.dp))
            .clickable(onClick = onReplay).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_speaker), contentDescription = null, tint = InklingColors.Ink, modifier = Modifier.size(22.dp))
                Text("Let's sound it out:", fontFamily = Andika, fontSize = 17.sp, color = InklingColors.Ink)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.chunks.forEach { c ->
                    Box(
                        Modifier.border(2.dp, InklingColors.Ink, RoundedCornerShape(8.dp))
                            .background(if (c.highlight) InklingColors.Teal else Color.Transparent, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text(c.text, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = InklingColors.Ink) }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Tap the mic and try the line again.", fontFamily = Andika, fontSize = 14.sp, color = InklingColors.Ink2)
    }
}

@Composable
private fun PlainCard(text: String) {
    Spacer(Modifier.height(10.dp))
    Box(
        Modifier.fillMaxWidth().heightIn(min = TapTarget)
            .border(2.dp, InklingColors.Ink, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) { Text(text, fontFamily = Andika, fontSize = 17.sp, color = InklingColors.Ink) }
}

@Composable
private fun GoodCard(onNext: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Row(
        Modifier.fillMaxWidth().heightIn(min = TapTarget)
            .background(InklingColors.Moss, RoundedCornerShape(12.dp))
            .clickable(onClick = onNext).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Nice reading!", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InklingColors.Ink)
        Text("Next page ›", fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = InklingColors.Ink)
    }
}
