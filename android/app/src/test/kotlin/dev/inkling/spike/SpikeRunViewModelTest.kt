package dev.inkling.spike

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SpikeRunViewModelTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, InklingDb::class.java).allowMainThreadQueries().build()
        repo = Repo(db)
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test fun inFlightSaveSurvivesUiDisposalAndAdvancesExactlyOnce() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val vm = SpikeRunViewModel(repo) { row ->
            started.complete(Unit)
            release.await()
            repo.addSpike(row)
        }
        val token = vm.beginListen()!!
        vm.onResult(token, SpikeLines.lines[0], 1f)
        val save = vm.record("correct")!!
        started.await()

        vm.cancelLiveAttempt() // The composition is disposed during rotation; this ViewModel remains.
        assertTrue(vm.state.value.saving)
        assertNull(vm.record("correct"))
        release.complete(Unit)
        save.join()

        assertFalse(vm.state.value.saving)
        assertEquals(1, vm.state.value.savedRows.size)
        assertEquals(1, vm.state.value.lineIndex)
        assertEquals(1, repo.spikes().size)
    }

    @Test fun twentySavedRowsStopAtTerminalSummary() = runBlocking {
        val vm = SpikeRunViewModel(repo)
        repeat(SpikeLines.lines.size) { index ->
            val token = vm.beginListen()!!
            vm.onResult(token, SpikeLines.lines[index], 1f)
            vm.record("correct")!!.join()
        }

        assertTrue(vm.state.value.complete)
        assertEquals(20, vm.state.value.savedRows.size)
        assertEquals(20, repo.spikes().size)
        assertNull(vm.beginListen())
    }
}
