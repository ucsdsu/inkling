package dev.inkling.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RepoTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InklingDb::class.java)
            .allowMainThreadQueries().build()
        repo = Repo(db)
    }
    @After fun tearDown() { db.close() }

    @Test fun ensureChildCreatesOnce() = runBlocking {
        val a = repo.ensureChild()
        val b = repo.ensureChild()
        assertEquals(a.id, b.id)
        assertEquals("Cove", a.name)
        assertEquals(false, repo.settings(a.id).kidsModeOn)
    }

    @Test fun openSpanClosesPrevious() = runBlocking {
        val c = repo.ensureChild()
        repo.openSpan(c.id, "com.chess", 1_000)
        repo.openSpan(c.id, "com.hangman", 5_000)
        val spans = repo.spansToday(c.id, 0)
        assertEquals(2, spans.size)
        assertEquals(5_000L, spans.first { it.packageName == "com.chess" }.endedAt)
        assertNull(spans.first { it.packageName == "com.hangman" }.endedAt)
    }

    @Test fun rulesUpsertByPackage() = runBlocking {
        val c = repo.ensureChild()
        repo.upsertRule(AppRule(childId = c.id, packageName = "com.chess", label = "Chess", enabled = true, dailyCapMinutes = 30))
        repo.upsertRule(AppRule(childId = c.id, packageName = "com.chess", label = "Chess", enabled = false, dailyCapMinutes = 10))
        val rules = repo.rules(c.id)
        assertEquals(1, rules.size)
        assertEquals(10, rules[0].dailyCapMinutes)
    }

    @Test fun pinHashIsStable() {
        assertEquals(Pin.hash("1234"), Pin.hash("1234"))
        assertEquals(64, Pin.hash("1234").length)
    }
}
