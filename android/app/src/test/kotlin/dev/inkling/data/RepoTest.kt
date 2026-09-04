package dev.inkling.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.core.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

    @Test fun activeChildIsNullOnFreshDb() = runBlocking {
        assertNull(repo.activeChild())
        assertEquals(emptyList<Child>(), repo.children())
    }

    @Test fun addChildSetsActiveAndCreatesSettings() = runBlocking {
        val c = repo.addChild("Cove", 4, "dinosaurs,chess", 2)
        assertEquals("Cove", c.name)
        assertEquals(4, c.ageYears)
        assertEquals("dinosaurs,chess", c.interests)
        assertEquals(2, c.startStage)
        assertEquals(c.id, repo.activeChild()?.id)
        assertEquals(false, repo.settings(c.id).kidsModeOn)
    }

    @Test fun setActiveChildSwitches() = runBlocking {
        val a = repo.addChild("Cove", 4, "", 0)
        val b = repo.addChild("Wren", 6, "", 3)
        assertEquals(b.id, repo.activeChild()?.id)
        repo.setActiveChild(a.id)
        assertEquals(a.id, repo.activeChild()?.id)
        assertEquals("Cove", repo.activeChild()?.name)
    }

    @Test fun childrenListsInCreationOrder() = runBlocking {
        repo.addChild("Cove", 4, "", 0)
        repo.addChild("Wren", 6, "", 0)
        repo.addChild("Ash", 5, "", 0)
        assertEquals(listOf("Cove", "Wren", "Ash"), repo.children().map { it.name })
    }

    @Test fun setStartStageUpdatesTheChild() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        repo.setStartStage(c.id, 4)
        assertEquals(4, repo.activeChild()?.startStage)
        assertEquals(4, repo.children().single().startStage)
    }

    @Test fun openSpanClosesPrevious() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        repo.openSpan(c.id, "com.chess", 1_000)
        repo.openSpan(c.id, "com.hangman", 5_000)
        val spans = repo.spansToday(c.id, 0)
        assertEquals(2, spans.size)
        assertEquals(5_000L, spans.first { it.packageName == "com.chess" }.endedAt)
        assertNull(spans.first { it.packageName == "com.hangman" }.endedAt)
    }

    @Test fun openSpanIfChangedKeepsTheSameSessionOpen() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        repo.openSpanIfChanged(c.id, "com.chess", 1_000)
        repo.openSpanIfChanged(c.id, "com.chess", 61_000)
        val spans = repo.spansToday(c.id, 0)
        assertEquals(1, spans.size)
        assertEquals(1_000L, spans[0].startedAt)
        assertNull(spans[0].endedAt)
        Unit
    }

    @Test fun openSpanIfChangedStartsANewSpanForANewApp() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        repo.openSpanIfChanged(c.id, "com.chess", 1_000)
        repo.openSpanIfChanged(c.id, "com.hangman", 5_000)
        val spans = repo.spansToday(c.id, 0)
        assertEquals(2, spans.size)
        assertEquals(5_000L, spans.first { it.packageName == "com.chess" }.endedAt)
        assertNull(spans.first { it.packageName == "com.hangman" }.endedAt)
        Unit
    }

    @Test fun closeStaleSpansCapsAtMaxSpan() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        val now = 100L * 60 * 60 * 1000
        repo.openSpan(c.id, "com.chess", now - 10 * 60 * 60 * 1000)   // 10 hours ago
        repo.closeStaleSpans(now)
        val stale = repo.spansToday(c.id, 0).first { it.packageName == "com.chess" }
        assertEquals(now - 10 * 60 * 60 * 1000 + Budget.MAX_SPAN_MS, stale.endedAt)
        Unit
    }

    @Test fun closeStaleSpansUsesNowForRecentSpans() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        val now = 100L * 60 * 60 * 1000
        repo.openSpan(c.id, "com.chess", now - 60_000)
        repo.closeStaleSpans(now)
        assertEquals(now, repo.spansToday(c.id, 0).first { it.packageName == "com.chess" }.endedAt)
        Unit
    }

    @Test fun concurrentOpenSpansLeaveExactlyOneOpen() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
        val pkgs = listOf("com.chess", "com.hangman")
        coroutineScope {
            repeat(20) { i ->
                launch(Dispatchers.IO) { repo.openSpan(c.id, pkgs[i % 2], 1_000L + i) }
            }
        }
        assertEquals(1, repo.spansToday(c.id, 0).count { it.endedAt == null })
    }

    @Test fun rulesUpsertByPackage() = runBlocking {
        val c = repo.addChild("Cove", 4, "", 0)
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
