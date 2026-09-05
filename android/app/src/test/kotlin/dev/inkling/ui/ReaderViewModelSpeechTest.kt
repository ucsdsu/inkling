package dev.inkling.ui

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import dev.inkling.books.BookStore
import dev.inkling.data.InklingDb
import dev.inkling.data.Repo
import dev.inkling.speech.FakeRecognizer
import dev.inkling.speech.FakeSpeaker
import dev.inkling.speech.OFFLINE_TTS_UNAVAILABLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReaderViewModelSpeechTest {
    private lateinit var db: InklingDb
    private lateinit var repo: Repo
    private lateinit var recognizer: FakeRecognizer
    private lateinit var speaker: FakeSpeaker
    private lateinit var vm: ReaderViewModel
    private lateinit var context: Context
    private var childId = 0L
    private var otherChildId = 0L

    @Before fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, InklingDb::class.java).allowMainThreadQueries().build()
        repo = Repo(db)
        val child = repo.addChild("Reader", 4, "", 0)
        val other = repo.addChild("Other", 5, "", 0)
        childId = child.id
        otherChildId = other.id
        repo.setActiveChild(childId)
        recognizer = FakeRecognizer()
        speaker = FakeSpeaker()
        vm = ReaderViewModel(repo, BookStore(context.assets), context, speaker, recognizer)
        vm.open("short-a-cat").join()
    }

    @After fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test fun resultFromPageLeftBehindCannotCoachOrLogEitherChild() = runBlocking {
        vm.listen()
        val stale = recognizer.attempts.single()
        vm.turn(1)
        stale.onResult("wrong words", 1f)

        assertEquals(1, vm.state.value.page)
        assertEquals(TutorPhase.IDLE, vm.state.value.phase)
        assertTrue(repo.recentAttempts(childId, 10).isEmpty())
        assertTrue(repo.recentAttempts(otherChildId, 10).isEmpty())
    }

    @Test fun oldRetryCallbacksCannotConsumeNewAttempt() = runBlocking {
        vm.listen()
        val first = recognizer.attempts[0]
        vm.stopListening()
        vm.listen()
        val second = recognizer.attempts[1]

        first.onError("late error")
        first.onResult("late result", 1f)
        second.onResult(vm.state.value.book!!.pages[0], 1f)
        second.onResult("duplicate", 1f)

        assertEquals(TutorPhase.GOOD, vm.state.value.phase)
        assertEquals(1, repo.recentAttempts(childId, 10).size)
        assertTrue(repo.recentAttempts(otherChildId, 10).isEmpty())
    }

    @Test fun readToMeCancelsMicAndStalePlaybackCannotMarkLaterPage() = runBlocking {
        vm.listen()
        val mic = recognizer.attempts.single()
        vm.speakLine()
        val playback = speaker.lines.single()
        mic.onResult("wrong words", 1f)
        vm.turn(1)
        playback.onWord(3)

        assertEquals(1, vm.state.value.page)
        assertEquals(-1, vm.state.value.speakingWord)
        assertTrue(repo.recentAttempts(childId, 10).isEmpty())
    }

    @Test fun unavailableTtsShowsNoticeAndDoesNotMarkPageTts() = runBlocking {
        speaker.queuesLine = false
        speaker.unavailableMessage = OFFLINE_TTS_UNAVAILABLE
        vm.speakLine()
        assertEquals(TutorPhase.UNCLEAR, vm.state.value.phase)
        assertEquals(OFFLINE_TTS_UNAVAILABLE, vm.state.value.notice)

        vm.turn(1)
        val cursor = db.openHelper.readableDatabase.query("SELECT mode FROM ReadEvent WHERE childId = $childId")
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals(ReadMode.LOOK, it.getString(0))
            assertEquals(1, it.count)
        }
    }

    @Test fun asynchronousTtsFailureRollsBackProvisionalTtsMode() = runBlocking {
        vm.speakLine()
        speaker.lines.single().onUnavailable(OFFLINE_TTS_UNAVAILABLE)
        vm.turn(1)

        assertEquals(ReadMode.LOOK, savedModes().single())
    }

    @Test fun laterTtsFailureDoesNotEraseEarlierSuccessfulReadAloud() = runBlocking {
        vm.speakLine()
        speaker.lines[0].onDone()
        vm.speakLine()
        speaker.lines[1].onUnavailable(OFFLINE_TTS_UNAVAILABLE)
        vm.turn(1)

        assertEquals(ReadMode.TTS, savedModes().single())
    }

    private fun savedModes(): List<String> {
        val cursor = db.openHelper.readableDatabase.query("SELECT mode FROM ReadEvent WHERE childId = $childId")
        return cursor.use {
            buildList {
                while (it.moveToNext()) add(it.getString(0))
            }
        }
    }
}
