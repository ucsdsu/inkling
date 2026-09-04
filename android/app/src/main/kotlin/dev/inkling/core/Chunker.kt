package dev.inkling.core

/** One piece of a sounded-out word. [highlight] marks the grapheme the book is teaching. */
data class Chunk(val text: String, val highlight: Boolean)

/** Splits a decodable word into the pieces a teacher would point at: letters, with common digraphs kept together. */
object Chunker {
    private val DIGRAPHS = listOf("sh", "ch", "th", "ck", "ng", "wh", "ph", "qu")

    /**
     * @param word the word as it appears on the page; case and punctuation are stripped
     * @param target the grapheme to highlight, for example "e" or "sh"
     * @return the chunks in order, left to right
     */
    fun chunk(word: String, target: String): List<Chunk> {
        val w = word.lowercase().filter { it.isLetter() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < w.length) {
            val two = if (i + 1 < w.length) w.substring(i, i + 2) else ""
            if (two in DIGRAPHS) {
                out += two
                i += 2
            } else {
                out += w[i].toString()
                i += 1
            }
        }
        return out.map { Chunk(it, it == target.lowercase()) }
    }
}
