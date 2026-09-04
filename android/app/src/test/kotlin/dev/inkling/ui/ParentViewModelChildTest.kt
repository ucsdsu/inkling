package dev.inkling.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.books.BookStore
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ParentViewModelChildTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo
    private lateinit var vm: ParentViewModel

    @Before fun setUp() {
        // viewModelScope runs on Main; Robolectric's paused main looper would deadlock runBlocking + join().
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, InklingDb::class.java).allowMainThreadQueries().build()
        repo = Repo(db)
        vm = ParentViewModel(repo, ctx.packageManager, "dev.inkling", BookStore(ctx.assets))
    }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    @Test fun noChildLeavesReadingAndNextUpEmpty() = runBlocking {
        vm.refresh().join()
        // Whole-state equality on purpose: the no-child branch has to reset, not copy. A copy would
        // leave the previous child's book list and recommendations on the parent's screen.
        assertEquals(ParentState(loaded = true, noChild = true), vm.state.value)
    }

    @Test fun switchChildMakesThatChildActive() = runBlocking {
        val cove = repo.addChild("Cove", 4, "", 0)
        val wren = repo.addChild("Wren", 6, "", 0)
        assertEquals(wren.id, repo.activeChild()!!.id)
        vm.switchChild(cove.id).join()
        assertEquals(cove.id, repo.activeChild()!!.id)
    }
}
