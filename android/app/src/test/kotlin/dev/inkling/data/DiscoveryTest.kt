package dev.inkling.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiscoveryTest {
    @Test fun datesAndReplayNeverClaimMasteryOrHideReading() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), InklingDb::class.java).build()
        try {
            val repo = Repo(db)
            val a = repo.addChild("A", 4, "", 0)
            val b = repo.addChild("B", 5, "", 1)
            val oldSettings = repo.settings(a.id)
            repo.finishDiscovery(a.id, "wheels", 100, false)
            repo.finishDiscovery(a.id, "wheels", 101, false)
            var p = repo.discovery(a.id, "wheels")!!
            assertEquals(100L, p.firstCompletedDay)
            assertFalse(p.recallDue(99)); assertFalse(p.recallDue(100)); assertTrue(p.recallDue(101))
            repo.finishDiscovery(a.id, "wheels", 101, true)
            repo.finishDiscovery(a.id, "wheels", 101, true)
            repo.finishDiscovery(a.id, "wheels", 99, true)
            p = repo.discovery(a.id, "wheels")!!
            assertEquals(101L, p.lastRecallDay)
            assertFalse(p.recallDue(101)); assertTrue(p.recallDue(102))
            assertNull(repo.discovery(b.id, "wheels"))
            assertEquals(oldSettings, repo.settings(a.id))
            assertEquals(b, repo.activeChild())
            assertTrue(repo.recentAttempts(a.id, 10).isEmpty())
            assertEquals(0, repo.booksFinishedToday(a.id, 0))
        } finally { db.close() }
    }

    @Test fun realV3MigrationAndReopenPreserveEveryOldRow() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("inkling.db")
        val old = context.openOrCreateDatabase("inkling.db", Context.MODE_PRIVATE, null)
        val schema = javaClass.classLoader!!.getResource("inkling-v3.sql")!!.readText()
        schema.split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
        old.execSQL("INSERT INTO Child VALUES (7,'Fixture',4,100,'wheels',2)")
        old.execSQL("INSERT INTO DeviceState VALUES (1,7)")
        old.execSQL("INSERT INTO Settings VALUES (7,0,45,2,1170,420,'fixture-pin',1,500)")
        old.execSQL("INSERT INTO AppRule VALUES (8,7,'example.app','Example',1,15)")
        old.execSQL("INSERT INTO UsageEvent VALUES (9,7,'example.app',100,200)")
        old.execSQL("INSERT INTO ReadEvent VALUES (10,7,'short-a-cat',1,100,200,'look')")
        old.execSQL("INSERT INTO TutorAttempt VALUES (11,7,'short-a-cat',1,'cat',1.0,'',1,0,200)")
        old.execSQL("INSERT INTO SpikeRow VALUES (12,'cat','cat',1.0,'','right',200)")
        val tables = listOf("Child","DeviceState","Settings","AppRule","UsageEvent","ReadEvent","TutorAttempt","SpikeRow","sqlite_sequence")
        fun snapshot(query: (String) -> android.database.Cursor): Map<String,List<List<String?>>> = tables.associateWith { table ->
            query("SELECT * FROM $table").use { cursor ->
                buildList { while(cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) }) }
            }
        }
        val expected = snapshot { old.rawQuery(it,null) }
        old.version = 3
        old.close()
        var db = InklingDb.open(context)
        try {
            val migrated = db.openHelper.writableDatabase
            assertEquals(4, migrated.version)
            assertEquals(expected, snapshot { migrated.query(it) })
            val repo = Repo(db)
            repo.finishDiscovery(7, "wheels", 100, false)
            db.close()
            db = InklingDb.open(context)
            assertEquals(100L, Repo(db).discovery(7, "wheels")!!.firstCompletedDay)
            assertEquals(expected, snapshot { db.openHelper.writableDatabase.query(it) })
        } finally { db.close(); context.deleteDatabase("inkling.db") }
    }
}
