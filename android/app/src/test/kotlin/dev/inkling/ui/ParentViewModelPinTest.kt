package dev.inkling.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.data.InklingDb
import dev.inkling.data.Pin
import dev.inkling.data.Repo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParentViewModelPinTest {
    private lateinit var db: InklingDb
    private lateinit var vm: ParentViewModel

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, InklingDb::class.java).allowMainThreadQueries().build()
        vm = ParentViewModel(Repo(db), ctx.packageManager, "dev.inkling")
    }
    @After fun tearDown() { db.close() }

    @Test fun noPinSetMeansNothingUnlocks() = runBlocking {
        assertFalse(vm.tryPin("1234", 0))
        assertFalse(vm.tryPin("", 0))
    }

    @Test fun rightPinUnlocksWrongPinDoesNot() = runBlocking {
        val repo = Repo(db)
        val child = repo.ensureChild()
        repo.saveSettings(repo.settings(child.id).copy(pinHash = Pin.hash("1234")))
        assertTrue(vm.tryPin("1234", 0))
        assertFalse(vm.tryPin("9999", 0))
    }
}
