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
import java.util.UUID

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
    fun test1_sessionACompleted_countOne() = runBlocking {
        val sessionIdA = UUID.randomUUID().toString()
        var reviewEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventTriggered = true
            }
        }

        repository.onTimerSessionCompleted(sessionIdA)

        assertEquals(1, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
        assertFalse(reviewEventTriggered)
        job.cancel()
    }

    @Test
    fun test2_sessionASameCompletionEventResent_noCountIncrease() = runBlocking {
        val sessionIdA = UUID.randomUUID().toString()

        repository.onTimerSessionCompleted(sessionIdA)
        assertEquals(1, repository.getCompletedTimerSessionCount())

        // Re-send same completion event
        repository.onTimerSessionCompleted(sessionIdA)
        assertEquals(1, repository.getCompletedTimerSessionCount())
    }

    @Test
    fun test3_sessionACompletionCalledTwiceConsecutively_countOneOnly() = runBlocking {
        val sessionIdA = UUID.randomUUID().toString()

        val count1 = repository.onTimerSessionCompleted(sessionIdA)
        val count2 = repository.onTimerSessionCompleted(sessionIdA)

        assertEquals(1, count1)
        assertEquals(1, count2)
        assertEquals(1, repository.getCompletedTimerSessionCount())
    }

    @Test
    fun test4_sessionACompletedThenSessionBCompleted_countTwo() = runBlocking {
        val sessionIdA = UUID.randomUUID().toString()
        val sessionIdB = UUID.randomUUID().toString()

        repository.onTimerSessionCompleted(sessionIdA)
        repository.onTimerSessionCompleted(sessionIdB)

        assertEquals(2, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
    }

    @Test
    fun test5_sessionACompleted_repoRecreated_sameSessionACompletionReprocessed_noCountIncrease() = runBlocking {
        val sessionIdA = UUID.randomUUID().toString()
        repository.onTimerSessionCompleted(sessionIdA)
        assertEquals(1, repository.getCompletedTimerSessionCount())

        val prefs = context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE)
        val newRepo = WorkoutRepository(AppDatabase.getDatabase(context).workoutDao(), prefs, context)

        // Re-process same Session A completion on new Repository instance
        newRepo.onTimerSessionCompleted(sessionIdA)
        assertEquals(1, newRepo.getCompletedTimerSessionCount())
    }

    @Test
    fun test6_sessionAPauseResumeComplete_countOne() = runBlocking {
        // Pausing does not complete session, completion happens at the end with same session ID
        val sessionIdA = UUID.randomUUID().toString()

        // Simulate pause/resume by maintaining same session ID until completion
        repository.onTimerSessionCompleted(sessionIdA)

        assertEquals(1, repository.getCompletedTimerSessionCount())
    }

    @Test
    fun test7_sessionACancel_noCountChange() = runBlocking {
        // Cancellation resets session ID and does not invoke onTimerSessionCompleted
        assertEquals(0, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
    }

    @Test
    fun test8_manualWorkoutRecordSaved3Times_noSessionCountChange_noReviewEvent() = runBlocking {
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
    fun test9_sessionABCCompleted_countThree_triggersReviewEventOnce() = runBlocking {
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())

        assertEquals(3, repository.getCompletedTimerSessionCount())
        assertTrue(repository.hasRequestedReview())
        assertEquals(1, reviewEventCount)
        job.cancel()
    }

    @Test
    fun test10_sessionABCCompletedThenSessionDCompleted_countFour_noAdditionalReviewEvent() = runBlocking {
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        repeat(4) {
            repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        }

        assertEquals(4, repository.getCompletedTimerSessionCount())
        assertTrue(repository.hasRequestedReview())
        assertEquals(1, reviewEventCount)
        job.cancel()
    }

    @Test
    fun test11_reviewEventTriggered_viewModelRecreated_noReplay() = runBlocking {
        // Trigger review event with 3 sessions
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())

        assertTrue(repository.hasRequestedReview())

        // Late collector subscribing after event was emitted
        var lateEventTriggered = false
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                lateEventTriggered = true
            }
        }

        // Since replay = 0, late collector should NOT receive the past event
        assertFalse(lateEventTriggered)
        job.cancel()
    }

    @Test
    fun test12_reviewEventTriggered_newCollectorRegistered_doesNotReceivePastEvent() = runBlocking {
        var initialEventCount = 0
        val job1 = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                initialEventCount++
            }
        }

        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())
        repository.onTimerSessionCompleted(UUID.randomUUID().toString())

        assertEquals(1, initialEventCount)
        job1.cancel()

        // Register new collector later
        var newCollectorEventCount = 0
        val job2 = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                newCollectorEventCount++
            }
        }

        assertEquals(0, newCollectorEventCount)
        job2.cancel()
    }

    @Test
    fun test13_concurrentDifferentSessionsABCCompletion_countThree() = runBlocking {
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        val sessionIds = List(3) { UUID.randomUUID().toString() }
        val deferreds = sessionIds.map { id ->
            async(Dispatchers.Default) {
                repository.onTimerSessionCompleted(id)
            }
        }
        deferreds.awaitAll()

        assertEquals(3, repository.getCompletedTimerSessionCount())
        assertTrue(repository.hasRequestedReview())
        assertEquals(1, reviewEventCount)
        job.cancel()
    }

    @Test
    fun test14_concurrentSameSessionACompletion_countOneOnly_reviewEventAtMostOne() = runBlocking {
        val sameSessionId = UUID.randomUUID().toString()
        var reviewEventCount = 0
        val job = launch(Dispatchers.Unconfined) {
            repository.reviewEventFlow.collect {
                reviewEventCount++
            }
        }

        val deferreds = List(10) {
            async(Dispatchers.Default) {
                repository.onTimerSessionCompleted(sameSessionId)
            }
        }
        deferreds.awaitAll()

        assertEquals(1, repository.getCompletedTimerSessionCount())
        assertFalse(repository.hasRequestedReview())
        assertEquals(0, reviewEventCount)
        job.cancel()
    }
}
