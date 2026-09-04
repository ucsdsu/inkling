package dev.inkling.core

/** One word of the placement read, and the starter stage it belongs to. */
data class PlacementWord(val word: String, val stageIndex: Int)

/** How one placement word went. SKIP is the parent or the child tapping past it, never a failure. */
enum class WordOutcome { OK, MISS, SKIP, UNCLEAR }

/**
 * Six words, one per starter stage, that decide where a new child begins.
 *
 * The read stops being useful at the first word he does not have, so the stage he starts at is the
 * stage of that word. Nothing here is scored or shown to him as right or wrong.
 */
object Placement {
    val WORDS = listOf(
        PlacementWord("cat", 0),
        PlacementWord("pen", 1),
        PlacementWord("pig", 2),
        PlacementWord("hop", 3),
        PlacementWord("bug", 4),
        PlacementWord("ship", 5),
    )

    /**
     * Stage index to start at: the stage of the first word not read OK; all OK means the last
     * stage. UNCLEAR counts as not OK only if it happens twice for the same word, so the screen
     * retries once and only then appends UNCLEAR here.
     *
     * @param outcomes one entry per word attempted, in [WORDS] order
     * @return an index into [WORDS], 0 when nothing was attempted
     */
    fun stageFor(outcomes: List<WordOutcome>): Int {
        if (outcomes.isEmpty()) return WORDS.first().stageIndex
        val i = outcomes.indexOfFirst { it != WordOutcome.OK }
        if (i < 0) return WORDS.last().stageIndex
        return WORDS[i.coerceAtMost(WORDS.lastIndex)].stageIndex
    }

    /**
     * Scores one spoken word against the word on screen, using the same diff the reader uses so a
     * three-year-old's "wed" for "red" is not held against him.
     *
     * @param word the word shown
     * @param transcript what the recognizer heard
     * @param confidence the engine's score
     * @return OK, MISS, or UNCLEAR when the recognizer heard nothing it trusts. Never SKIP.
     */
    fun outcome(word: String, transcript: String, confidence: Float): WordOutcome {
        val r = ReadingDiff.score(word, transcript, confidence)
        return when {
            r.lowConfidence -> WordOutcome.UNCLEAR
            r.missed.isEmpty() -> WordOutcome.OK
            else -> WordOutcome.MISS
        }
    }
}
