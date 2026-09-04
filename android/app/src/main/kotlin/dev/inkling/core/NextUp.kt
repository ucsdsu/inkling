package dev.inkling.core

import kotlin.math.roundToInt

/** One thing the parent could do next, and the reason in plain words. */
data class Recommendation(val title: String, val why: String, val bookId: String?)

/**
 * One shelf row, flattened for [NextUp] so `core/` never sees a Book or the UI's row types.
 * [lastAccuracy] is 0..1 over recent tutored attempts, null when he has never read it aloud.
 */
data class ShelfEntry(
    val bookId: String,
    val title: String,
    val stageLabel: String,
    val tag: Tag,
    val lastAccuracy: Float?,
)

/**
 * Turns the shelf into at most three suggestions for the parent, each with a reason.
 *
 * The order is fixed because it is the order that helps: one new pattern to try, one book that is
 * close to solid, one book to read to him. A shelf with none of those gets nothing, which is the
 * honest answer rather than filler.
 */
object NextUp {
    /** Above this, a second pass buys little, so the book stops being a suggestion. */
    const val SECOND_PASS_CEILING = 0.90f

    fun recommend(rows: List<ShelfEntry>, missedTwice: List<String>): List<Recommendation> {
        val out = mutableListOf<Recommendation>()

        rows.firstOrNull { it.tag == Tag.TRY_IT }?.let {
            out += Recommendation(
                title = it.title,
                why = "Introduces ${it.stageLabel}. One new pattern is the right step.",
                bookId = it.bookId,
            )
        }

        rows.firstOrNull { it.tag == Tag.JUST_RIGHT && (it.lastAccuracy ?: 1f) < SECOND_PASS_CEILING }?.let {
            val percent = ((it.lastAccuracy ?: 0f) * 100).roundToInt()
            val watch = if (missedTwice.isEmpty()) "" else " Words to watch: ${missedTwice.joinToString(", ")}."
            out += Recommendation(
                title = it.title,
                why = "${it.title} was $percent% last time. A second pass usually pushes it past 90.$watch",
                bookId = it.bookId,
            )
        }

        rows.firstOrNull { it.tag == Tag.STRETCH }?.let {
            out += Recommendation(
                title = it.title,
                why = "Too many new patterns for now. Keep it as a 'read to me' book.",
                bookId = it.bookId,
            )
        }

        return out
    }
}
