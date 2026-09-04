package dev.inkling.books

import android.content.res.AssetManager
import kotlinx.serialization.json.Json

class BookStore(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val books: List<Book> by lazy {
        assets.list("books").orEmpty().filter { it.endsWith(".json") }
            .map { name -> assets.open("books/$name").bufferedReader().use { json.decodeFromString(Book.serializer(), it.readText()) } }
            .sortedBy { STAGES.indexOf(it.stage) }
    }
    fun all(): List<Book> = books
    fun byId(id: String): Book? = books.firstOrNull { it.id == id }
}
