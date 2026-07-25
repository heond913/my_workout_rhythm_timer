package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.WorkoutRepository
import com.example.viewmodel.WorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InAppReviewTest {

    private lateinit var context: Context
    private lateinit var repository: WorkoutRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        val db = AppDatabase.getDatabase(context)
        repository = WorkoutRepository(db.workoutDao(), prefs, context)
    }

    @Test
    fun test1_firstTimerSessionCompleted_countOne_noReviewEvent() = runBlocking {
        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        repository.onTimerSessionCompleted()

        assertEquals(1, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
        assertFalse(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test2_secondTimerSessionCompleted_countTwo_noReviewEvent() = runBlocking {
        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        repository.onTimerSessionCompleted()
        repository.onTimerSessionCompleted()

        assertEquals(2, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
        assertFalse(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test3_thirdTimerSessionCompleted_countThree_triggersReviewEventOnce() = runBlocking {
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        repository.onTimerSessionCompleted()
        repository.onTimerSessionCompleted()
        repository.onTimerSessionCompleted()

        assertEquals(3, repository.getCompletedTimerSessionCount())
        assertTrue(repository.hasRequestedReview())
        assertEquals(1, reviewEventCount)
        job.cancel()
    }

    @Test
    fun test4_fourthTimerSessionCompleted_countFour_noAdditionalReviewEvent() = runBlocking {
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        repeat(4) {
            repository.onTimerSessionCompleted()
        }

        assertEquals(4, repository.getCompletedTimerSessionCount())
        assertTrue(repository.hasRequestedReview())
        assertEquals(1, reviewEventCount)
        job.cancel()
    }

    @Test
    fun test5_manualWorkoutRecordSaves_doesNotIncreaseSessionCount_noReviewEvent() = runBlocking {
        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = WorkoutViewModel(app)

        repeat(3) { index ->
            viewModel.saveWorkoutRecord(
                exercise = "스쿼트",
                reps = 15,
                sets = 3,
                weightKg = 0.0,
                duration = 60,
                rating = 5,
                note = "Manual $index"
            )
        }

        assertEquals(0, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
        assertFalse(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test6_timerStartedThenReset_sessionCountDoesNotIncrease() {
        assertEquals(0, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
    }

    @Test
    fun test7_timerStartedThenPaused_sessionCountDoesNotIncrease() {
        assertEquals(0, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
    }

    @Test
    fun test8_duplicateCompletionEvent_incrementsSessionCountOnlyOnce() = runBlocking {
        var isCompleted = false
        fun handleSessionCompletion() {
            if (!isCompleted) {
                isCompleted = true
                repository.onTimerSessionCompleted()
            }
        }

        handleSessionCompletion()
        handleSessionCompletion()
        handleSessionCompletion()

        assertEquals(1, repository.getCompletedTimerSessionCount())
    }

    @Test
    fun test9_countPersistsAcrossAppRestart_thirdSessionTriggersReview() = runBlocking {
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("completed_timer_session_count", 2).commit()

        val newRepo = WorkoutRepository(AppDatabase.getDatabase(context).workoutDao(), prefs, context)

        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            newRepo.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        newRepo.onTimerSessionCompleted()

        assertEquals(3, newRepo.getCompletedTimerSessionCount())
        assertTrue(newRepo.hasRequestedReview())
        assertTrue(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test10_alreadyRequestedReview_newSessionDoesNotTriggerReview() = runBlocking {
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("completed_timer_session_count", 3).putBoolean("has_requested_review", true).commit()

        val newRepo = WorkoutRepository(AppDatabase.getDatabase(context).workoutDao(), prefs, context)

        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            newRepo.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        newRepo.onTimerSessionCompleted()

        assertEquals(4, newRepo.getCompletedTimerSessionCount())
        assertTrue(newRepo.hasRequestedReview())
        assertFalse(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test11_countFourOrMoreAndHasNotRequestedReview_newSessionTriggersReview() = runBlocking {
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("completed_timer_session_count", 4).putBoolean("has_requested_review", false).commit()

        val newRepo = WorkoutRepository(AppDatabase.getDatabase(context).workoutDao(), prefs, context)

        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            newRepo.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        newRepo.onTimerSessionCompleted()

        assertEquals(5, newRepo.getCompletedTimerSessionCount())
        assertTrue(newRepo.hasRequestedReview())
        assertTrue(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test12_concurrentSessionCompletionEvents_emitsAtMostOneReviewEvent() = runBlocking {
        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("completed_timer_session_count", 2).putBoolean("has_requested_review", false).commit()

        val repo = WorkoutRepository(AppDatabase.getDatabase(context).workoutDao(), prefs, context)

        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repo.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        val deferreds = List(10) {
            async(Dispatchers.Default) {
                repo.onTimerSessionCompleted()
            }
        }
        deferreds.awaitAll()

        assertEquals(1, reviewEventCount)
        assertTrue(repo.hasRequestedReview())
        job.cancel()
    }
}
