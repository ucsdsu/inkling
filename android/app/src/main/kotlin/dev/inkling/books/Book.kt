package dev.inkling.books

import kotlinx.serialization.Serializable

/** One decodable book. [target] is the grapheme the coach highlights when a word is missed. */
@Serializable
data class Book(val id: String, val title: String, val stage: String, val target: String, val pages: List<String>)

/** Stage order for the shelf. */
val STAGES = listOf("cvc-a", "cvc-e", "cvc-i", "cvc-o", "cvc-u", "digraph-sh")
