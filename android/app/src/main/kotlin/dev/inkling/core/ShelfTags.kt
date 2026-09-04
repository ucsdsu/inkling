package dev.inkling.core

/** How hard a book is for this child right now, shown on the shelf. */
enum class Tag { TRY_IT, JUST_RIGHT, EASY, STRETCH }

/**
 * What the child has done with one book.
 * @property finished how many times the last page was reached
 * @property lastAccuracy 0..1 over recent tutored attempts, null if never tutored
 */
data class BookHistory(val bookId: String, val finished: Int, val lastAccuracy: Float?)

/**
 * The five-finger rule, made visible. About 1 unknown word in 20 is "just right":
 * accuracy at or above 0.95 is EASY, below 0.75 is STRETCH, in between is JUST_RIGHT.
 * Books above the child's stage are STRETCH until proven otherwise; below it, EASY.
 */
object ShelfTags {
    const val EASY_FLOOR = 0.95f
    const val STRETCH_CEILING = 0.75f

    fun tag(history: BookHistory?, stageIndex: Int, currentStageIndex: Int): Tag {
        val acc = history?.lastAccuracy
        if (acc != null) return when {
            acc >= EASY_FLOOR -> Tag.EASY
            acc < STRETCH_CEILING -> Tag.STRETCH
            else -> Tag.JUST_RIGHT
        }
        return when {
            stageIndex > currentStageIndex -> Tag.STRETCH
            stageIndex < currentStageIndex -> Tag.EASY
            else -> Tag.TRY_IT
        }
    }
}
