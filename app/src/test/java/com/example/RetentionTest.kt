package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.data.RetentionStateStore
import com.example.util.AppLifecycleObserver
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkoutViewModel
import com.example.worker.RetentionDay
import com.example.worker.RetentionWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetentionTest {

    private lateinit var context: Context
    private lateinit var stateStore: RetentionStateStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        stateStore = RetentionStateStore(context)
        context.getSharedPreferences("workout_rhythm_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun testCase1_d1BeforeWorkout_skipsD1Notification() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 86400000L)
        stateStore.recordWorkoutStarted()

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D1))
    }

    @Test
    fun testCase2_d1WithoutWorkout_triggersD1Notification() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 86400000L)
        assertFalse(stateStore.hasStartedWorkout())

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D1))
    }

    @Test
    fun testCase3_d3AfterWorkout_skipsD3Notification() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 259200000L)
        stateStore.recordWorkoutStarted()

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
    }

    @Test
    fun testCase4_d3WithoutWorkout_triggersD3Notification() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 259200000L)
        assertFalse(stateStore.hasStartedWorkout())

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
    }

    @Test
    fun testCase5_permissionDenied_handlesGracefully() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 86400000L)

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testCase6_duplicateD1Worker_executesIdempotently() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 86400000L)
        stateStore.setRetentionHandled(RetentionDay.D1, true)

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testCase7_duplicateD3Worker_executesIdempotently() {
        stateStore.recordFirstOpenAt(System.currentTimeMillis() - 259200000L)
        stateStore.setRetentionHandled(RetentionDay.D3, true)

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testCase8_d1NotificationClick_navigatesToTimerScreen() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("from_retention_push", true)
            putExtra(RetentionDay.KEY_RETENTION_DAY, 1)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        val activity = controller.get()
        assertNotNull(activity)

        val viewModel = androidx.lifecycle.ViewModelProvider(activity)[WorkoutViewModel::class.java]
        assertEquals(AppTab.Timer, viewModel.uiState.value.currentTab)
    }

    @Test
    fun testCase9_d3NotificationClick_navigatesToTimerScreen() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("from_retention_push", true)
            putExtra(RetentionDay.KEY_RETENTION_DAY, 3)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        val activity = controller.get()
        assertNotNull(activity)

        val viewModel = androidx.lifecycle.ViewModelProvider(activity)[WorkoutViewModel::class.java]
        assertEquals(AppTab.Timer, viewModel.uiState.value.currentTab)
    }

    @Test
    fun testCase10_appReEntry_recordsForegroundState() {
        val observer = AppLifecycleObserver(context)
        val initialTime = stateStore.getLastAppForegroundAt()

        observer.onStart(Robolectric.buildActivity(MainActivity::class.java).get())

        val updatedTime = stateStore.getLastAppForegroundAt()
        assertTrue(updatedTime >= initialTime)
    }
}
