package com.example

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.example.data.RetentionStateStore
import com.example.util.AppLifecycleObserver
import com.example.util.FakeClock
import com.example.util.NotificationHelper
import com.example.viewmodel.AppTab
import com.example.viewmodel.WorkoutViewModel
import com.example.worker.PushScheduler
import com.example.worker.RetentionConstants
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
import org.robolectric.Shadows.shadowOf
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
        NotificationHelper.clearPostedNotificationsForTest()

        // Grant POST_NOTIFICATIONS by default for test cases
        val shadowApp = shadowOf(context.applicationContext as Application)
        shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun testCase1_d1NoWorkout_triggersD1Push() {
        val now = System.currentTimeMillis()
        stateStore.recordFirstOpenAt(now - 86400000L) // 24 hours ago
        assertFalse(stateStore.hasStartedWorkout())

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D1))
        assertEquals(1, NotificationHelper.postedNotifications.size)
        assertEquals(1, NotificationHelper.postedNotifications[0].retentionDayNumber)
    }

    @Test
    fun testCase2_d1AfterFirstWorkout_skipsD1Push() {
        val now = System.currentTimeMillis()
        stateStore.recordFirstOpenAt(now - 86400000L)
        stateStore.recordWorkoutStarted(now - 80000000L)

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D1))
        assertEquals(0, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase3_d3FirstWorkoutOnly_triggersD3Push() {
        val now = System.currentTimeMillis()
        val installTime = now - (72 * 3600 * 1000L) // 72 hours ago
        stateStore.recordFirstOpenAt(installTime)

        // First workout on Day 0 (70 hours ago)
        val firstWorkoutTime = installTime + (2 * 3600 * 1000L)
        stateStore.recordWorkoutStarted(firstWorkoutTime)

        // D1 runs on Day 1 and skips
        stateStore.setRetentionHandled(RetentionDay.D1, true)

        // D3 runs on Day 3 (72 hours after install)
        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(1, NotificationHelper.postedNotifications.size)
        assertEquals(3, NotificationHelper.postedNotifications[0].retentionDayNumber)
    }

    @Test
    fun testCase4_d3RecentWorkout_skipsD3Push() {
        val now = System.currentTimeMillis()
        val installTime = now - (72 * 3600 * 1000L) // 72 hours ago
        stateStore.recordFirstOpenAt(installTime)

        // First workout on Day 0
        stateStore.recordWorkoutStarted(installTime + (2 * 3600 * 1000L))

        // Recent workout on Day 2 (20 hours ago)
        stateStore.recordWorkoutStarted(now - (20 * 3600 * 1000L))

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(0, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase5_d3NoWorkout_triggersD3Push() {
        val now = System.currentTimeMillis()
        val installTime = now - (72 * 3600 * 1000L)
        stateStore.recordFirstOpenAt(installTime)
        assertFalse(stateStore.hasStartedWorkout())

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(1, NotificationHelper.postedNotifications.size)
        assertEquals(3, NotificationHelper.postedNotifications[0].retentionDayNumber)
    }

    @Test
    fun testCase6_d1ClickImmediateWorkout_logsAttribution() {
        val now = System.currentTimeMillis()
        stateStore.setPendingRetentionClick(1, now)

        val clickDay = stateStore.getAndClearValidPendingRetentionClickDay(now + (10 * 60 * 1000L)) // 10 mins later
        assertEquals(1, clickDay)
    }

    @Test
    fun testCase7_d1ClickDelayedWorkout_noAttribution() {
        val now = System.currentTimeMillis()
        stateStore.setPendingRetentionClick(1, now)

        // 2 hours later (120 mins) > 30 mins attribution window
        val clickDay = stateStore.getAndClearValidPendingRetentionClickDay(now + (120 * 60 * 1000L))
        assertEquals(0, clickDay)
    }

    @Test
    fun testCase8_d3ClickWorkoutWithinWindow_logsAttribution() {
        val now = System.currentTimeMillis()
        stateStore.setPendingRetentionClick(3, now)

        val clickDay = stateStore.getAndClearValidPendingRetentionClickDay(now + (15 * 60 * 1000L)) // 15 mins later
        assertEquals(3, clickDay)
    }

    @Test
    fun testCase9_duplicateD1Worker_executesIdempotently() {
        val now = System.currentTimeMillis()
        stateStore.recordFirstOpenAt(now - 86400000L)

        val worker1 = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()
        worker1.startWork().get()

        assertEquals(1, NotificationHelper.postedNotifications.size)

        val worker2 = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()
        worker2.startWork().get()

        assertEquals(1, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase10_duplicateD3Worker_executesIdempotently() {
        val now = System.currentTimeMillis()
        stateStore.recordFirstOpenAt(now - (72 * 3600 * 1000L))

        val worker1 = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()
        worker1.startWork().get()

        assertEquals(1, NotificationHelper.postedNotifications.size)

        val worker2 = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()
        worker2.startWork().get()

        assertEquals(1, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase11_permissionDenied_handlesGracefully() {
        val shadowApp = shadowOf(context.applicationContext as Application)
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val now = System.currentTimeMillis()
        stateStore.recordFirstOpenAt(now - 86400000L)

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, NotificationHelper.postedNotifications.size)
        assertFalse(stateStore.isRetentionHandled(RetentionDay.D1))
        assertTrue(stateStore.isRetentionPermissionPending(RetentionDay.D1))
    }

    @Test
    fun testCase12_appReEntry_recordsForegroundState() {
        val observer = AppLifecycleObserver(context)
        val initialTime = stateStore.getLastAppForegroundAt()

        observer.onStart(Robolectric.buildActivity(MainActivity::class.java).get())

        val updatedTime = stateStore.getLastAppForegroundAt()
        assertTrue(updatedTime >= initialTime)
        assertEquals(0L, stateStore.getFirstWorkoutStartedAt())
    }

    @Test
    fun testCase13_d1ClickTimerNoWorkoutNextDayWorkout_noAttribution() {
        val now = System.currentTimeMillis()
        stateStore.setPendingRetentionClick(1, now)

        // Day 2 (24 hours later), user starts workout
        val nextDayTime = now + (24 * 3600 * 1000L)
        val clickDay = stateStore.getAndClearValidPendingRetentionClickDay(nextDayTime)
        assertEquals(0, clickDay)
    }

    @Test
    fun testCase14_d3TimeBoundary_workoutInWindow_skipsD3Push() {
        // First open Day 0 10:00
        val t0 = 1000000000000L
        stateStore.recordFirstOpenAt(t0)

        // D3 Target = Day 3 10:00 (t0 + 72h)
        val d3Target = t0 + (72 * 3600 * 1000L)

        // Workout at Day 1 15:00 (t0 + 29h), which is inside Activity Window [Day 1 10:00 .. Day 3 10:00]
        val workoutTime = t0 + (29 * 3600 * 1000L)
        stateStore.recordWorkoutStarted(workoutTime)

        val fakeClock = FakeClock(d3Target)
        RetentionWorker.clock = fakeClock

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(0, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase15_d3TimeBoundary_workoutBeforeWindow_triggersD3Push() {
        // First open Day 0 10:00
        val t0 = 1000000000000L
        stateStore.recordFirstOpenAt(t0)

        // D3 Target = Day 3 10:00 (t0 + 72h)
        val d3Target = t0 + (72 * 3600 * 1000L)

        // Workout at Day 1 09:59 (t0 + 23h 59m), before Activity Window cutoff (Day 1 10:00)
        val workoutTime = t0 + (23 * 3600 * 1000L + 59 * 60 * 1000L)
        stateStore.recordWorkoutStarted(workoutTime)

        val fakeClock = FakeClock(d3Target)
        RetentionWorker.clock = fakeClock

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(1, NotificationHelper.postedNotifications.size)
        assertEquals(3, NotificationHelper.postedNotifications[0].retentionDayNumber)
    }

    @Test
    fun testCase16_d3TimeBoundary_delayedWorkerExecution_evaluatesAgainstD3TargetTime() {
        // First open Day 0 10:00
        val t0 = 1000000000000L
        stateStore.recordFirstOpenAt(t0)

        // Workout at Day 1 15:00 (t0 + 29h), inside Activity Window relative to D3 Target Time (t0 + 72h)
        val workoutTime = t0 + (29 * 3600 * 1000L)
        stateStore.recordWorkoutStarted(workoutTime)

        // Worker executes 5 hours late at Day 3 15:00 (t0 + 77h)
        val delayedWorkerTime = t0 + (77 * 3600 * 1000L)
        val fakeClock = FakeClock(delayedWorkerTime)
        RetentionWorker.clock = fakeClock

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        // Should skip because relative to D3 Target Time (t0 + 72h), the workout at t0+29h was within cutoff (t0+24h)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(0, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase17_d3TimeBoundary_workerDelayedExceedsGracePeriod_expiresD3Campaign() {
        val t0 = 1000000000000L
        stateStore.recordFirstOpenAt(t0)

        // Worker executes 30 hours late (exceeds 24h grace period)
        val wayLateTime = t0 + (72 * 3600 * 1000L) + (30 * 3600 * 1000L)
        val fakeClock = FakeClock(wayLateTime)
        RetentionWorker.clock = fakeClock

        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 3))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D3))
        assertEquals(0, NotificationHelper.postedNotifications.size)
    }

    @Test
    fun testCase18_permissionDenied_savesPendingState_resumesOnPermissionGrant() {
        val shadowApp = shadowOf(context.applicationContext as Application)
        shadowApp.denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val t0 = 1000000000000L
        stateStore.recordFirstOpenAt(t0)

        val fakeClock = FakeClock(t0 + 86400000L)
        RetentionWorker.clock = fakeClock
        PushScheduler.clock = fakeClock

        // 1. Worker runs while permission is denied
        val worker = TestListenableWorkerBuilder<RetentionWorker>(context)
            .setInputData(workDataOf(RetentionDay.KEY_RETENTION_DAY to 1))
            .build()

        val result = worker.startWork().get()
        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, NotificationHelper.postedNotifications.size)
        assertFalse(stateStore.isRetentionHandled(RetentionDay.D1))
        assertTrue(stateStore.isRetentionPermissionPending(RetentionDay.D1))

        // 2. User grants permission later
        shadowApp.grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        // 3. App re-enters foreground / PushScheduler processes pending campaigns
        PushScheduler.processPendingRetentionCampaigns(context)

        assertEquals(1, NotificationHelper.postedNotifications.size)
        assertTrue(stateStore.isRetentionHandled(RetentionDay.D1))
        assertFalse(stateStore.isRetentionPermissionPending(RetentionDay.D1))
    }

    @Test
    fun testNotificationClickNavigation_navigatesToTimerScreen() {
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
}
