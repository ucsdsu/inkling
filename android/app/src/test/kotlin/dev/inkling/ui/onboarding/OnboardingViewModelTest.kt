package dev.inkling.ui.onboarding

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo
import dev.inkling.speech.FakeRecognizer
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
class OnboardingViewModelTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo
    private lateinit var vm: OnboardingViewModel
    private lateinit var recognizer: FakeRecognizer

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, InklingDb::class.java).allowMainThreadQueries().build()
        repo = Repo(db)
        recognizer = FakeRecognizer()
        vm = OnboardingViewModel(repo, ctx, recognizer)
    }
    @After fun tearDown() { db.close(); Dispatchers.resetMain() }

    @Test fun repeatedFinishCreatesOneChildAndNavigatesOnce() = runBlocking {
        val original = repo.addChild("Existing", 6, "chess", 3)
        val settings = repo.settings(original.id)
        vm.setName("Test")
        repeat(6) { vm.skip() }
        var navigations = 0
        val first = vm.finish { navigations++ }
        val second = vm.finish { navigations++ }
        first.join(); second.join()
        assertEquals(2, repo.children().size)
        assertEquals(1, navigations)
        assertEquals(original, repo.children().first())
        assertEquals(settings, repo.settings(original.id))
        val created = repo.activeChild()!!
        vm.finish { navigations++ }.join()
        assertEquals(created.id, repo.activeChild()!!.id)
        assertEquals(2, repo.children().size)
        repo.setActiveChild(original.id)
        assertEquals(original, repo.activeChild())
    }

    @Test fun unfinishedPlacementCannotCreateChild() = runBlocking {
        vm.setName("Test")
        vm.finish {}.join()
        assertEquals(0, repo.children().size)
    }

    @Test fun skipInvalidatesLateResultForPreviousWord() {
        vm.listen()
        val stale = recognizer.attempts.single()
        vm.skip()
        stale.onResult("cat", 1f)

        assertEquals(1, vm.state.value.wordIndex)
        assertEquals(listOf(dev.inkling.core.WordOutcome.SKIP), vm.state.value.outcomes)
    }

    @Test fun oldAttemptCannotConsumeRetryResult() {
        vm.listen()
        val first = recognizer.attempts[0]
        first.onError("unclear")
        vm.listen()
        val retry = recognizer.attempts[1]

        first.onResult("cat", 1f)
        retry.onResult("cat", 1f)

        assertEquals(1, vm.state.value.wordIndex)
        assertEquals(listOf(dev.inkling.core.WordOutcome.OK), vm.state.value.outcomes)
    }
}
