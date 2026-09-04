package dev.inkling.core

enum class WordResult { OK, MISS, UNSURE }

/**
 * One scored read-aloud attempt.
 * @property words the expected words in order (original spelling) with their result
 * @property lowConfidence true when the recognizer was not sure enough to flag anything
 */
data class DiffResult(val words: List<Pair<String, WordResult>>, val lowConfidence: Boolean) {
    /** The missed words, punctuation stripped, so callers can look them up or say them aloud. */
    val missed: List<String>
        get() = words.filter { it.second == WordResult.MISS }
            .map { (word, _) -> word.filter { c -> c.isLetter() || c == '\'' } }
}

/**
 * Compares what a child said with the line on the page.
 *
 * Rules, in order:
 * 1. Empty transcript or confidence under [CONFIDENCE_FLOOR] means every word is UNSURE.
 * 2. Words are aligned by edit distance so a skipped or repeated word does not shift the rest.
 * 3. A word matches if the normalized forms are equal, or differ only by a typical
 *    3-to-5-year-old articulation pattern (r/l to w, th to f or d, initial cluster reduction).
 * 4. Everything else is MISS.
 */
object ReadingDiff {
    const val CONFIDENCE_FLOOR = 0.5f

    fun score(expected: String, transcript: String, confidence: Float): DiffResult {
        val expectedWords = expected.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val heard = transcript.trim().split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
        if (heard.isEmpty() || confidence < CONFIDENCE_FLOOR) {
            return DiffResult(expectedWords.map { it to WordResult.UNSURE }, lowConfidence = true)
        }
        val exp = expectedWords.map(::normalize)
        val matched = align(exp, heard)
        val results = expectedWords.mapIndexed { i, original ->
            original to if (matched[i]) WordResult.OK else WordResult.MISS
        }
        return DiffResult(results, lowConfidence = false)
    }

    private fun normalize(w: String): String = w.lowercase().filter { it.isLetter() || it == '\'' }

    /** Standard edit-distance alignment. Returns, per expected word, whether it was matched. */
    private fun align(exp: List<String>, heard: List<String>): BooleanArray {
        val n = exp.size
        val m = heard.size
        val cost = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) cost[i][0] = i
        for (j in 0..m) cost[0][j] = j
        for (i in 1..n) for (j in 1..m) {
            val sub = if (sameWord(exp[i - 1], heard[j - 1])) 0 else 1
            cost[i][j] = minOf(cost[i - 1][j] + 1, cost[i][j - 1] + 1, cost[i - 1][j - 1] + sub)
        }
        val matched = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val same = sameWord(exp[i - 1], heard[j - 1])
            when {
                same && cost[i][j] == cost[i - 1][j - 1] -> { matched[i - 1] = true; i--; j-- }
                cost[i][j] == cost[i][j - 1] + 1 -> j--          // extra heard word (self-correction)
                cost[i][j] == cost[i - 1][j] + 1 -> i--          // skipped expected word
                else -> { i--; j-- }                             // substitution
            }
        }
        return matched
    }

    private fun sameWord(expected: String, heard: String): Boolean {
        if (expected == heard) return true
        return articulationVariants(expected).contains(heard)
    }

    /** Forms a typical 3-to-5-year-old might produce for a correctly decoded word. */
    private fun articulationVariants(w: String): Set<String> {
        val out = mutableSetOf<String>()
        val subs = listOf("r" to "w", "l" to "w", "th" to "f", "th" to "d")
        var forms = setOf(w)
        for ((from, to) in subs) {
            forms = forms + forms.map { it.replace(from, to) }
        }
        out += forms
        // Initial cluster reduction: "stop" -> "top", "truck" -> "tuck", "blue" -> "bue"
        val clusters = listOf("st", "sp", "sk", "tr", "dr", "br", "bl", "cl", "fl", "gl", "pl", "sl", "cr", "fr", "gr", "pr", "sn", "sm", "sw")
        for (c in clusters) {
            if (w.startsWith(c)) {
                out += w.drop(1)          // drop first consonant: stop -> top
                out += c[0] + w.drop(2)   // drop second: truck -> tuck
            }
        }
        // Reduction combined with r/l/th substitution, e.g. "truck" -> "tuck" is already covered;
        // "thrill" -> "fwill" would need both, handled by applying subs to reduced forms.
        val reduced = out.toList()
        for (r in reduced) {
            var f = setOf(r)
            for ((from, to) in subs) f = f + f.map { it.replace(from, to) }
            out += f
        }
        return out
    }
}
