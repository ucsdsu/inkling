package dev.inkling.books

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookStoreTest {
    private val store = BookStore(ApplicationProvider.getApplicationContext<Context>().assets)
    private val sight = setOf("the","a","is","in","on","to","and","i","has","can","not","it","up")

    @Test fun loadsSixBooksInStageOrder() {
        assertEquals(listOf("cvc-a","cvc-e","cvc-i","cvc-o","cvc-u","digraph-sh"), store.all().map { it.stage })
        assertTrue(store.all().all { it.pages.size == 10 })
    }

    @Test fun everyWordIsDecodableOrSight() {
        for (b in store.all()) for (p in b.pages) for (w in p.lowercase().split(" ")) {
            val word = w.filter { it.isLetter() }
            if (word in sight || word.isEmpty()) continue
            val vowels = word.count { it in "aeiou" }
            assertTrue("$word in ${b.id} has $vowels vowels", vowels == 1)
            if (b.stage.startsWith("cvc")) assertTrue("$word in ${b.id} wrong vowel", word.contains(b.target))
        }
    }

    @Test fun byIdFindsAndMisses() {
        assertEquals("The Big Red Hen", store.byId("short-e-hen")?.title)
        assertEquals(null, store.byId("nope"))
    }
}
