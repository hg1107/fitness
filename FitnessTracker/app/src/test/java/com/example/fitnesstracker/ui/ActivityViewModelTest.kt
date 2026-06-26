package com.example.fitnesstracker.ui

import android.content.Context
import android.content.Intent
import com.example.fitnesstracker.data.*
import com.example.fitnesstracker.service.LocationPoint
import com.example.fitnesstracker.service.TrackingService
import com.example.fitnesstracker.service.TrackingState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModelTest {

    private lateinit var fakeDao: FakeActivityDaoForActivity
    private lateinit var mockContext: Context
    private lateinit var viewModel: ActivityViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeActivityDaoForActivity()
        mockContext = mockk(relaxed = true)

        // Mock Intent constructor and service starting
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setAction(any()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().putExtra(any<String>(), any<Double>()) } returns mockk(relaxed = true)

        viewModel = ActivityViewModel(
            activityDao = fakeDao,
            appContext = mockContext
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun testUpdateUserProfile() = runTest {
        // Given no user profile in database initially
        // When we update the profile
        viewModel.updateUserProfile("Jane Doe", 28, 62.0, 168.0, "Imperial")

        // Then it should be saved correctly in the fake database
        val profile = fakeDao.getUserProfileSync()
        assertNotNull(profile)
        assertEquals("Jane Doe", profile!!.name)
        assertEquals(28, profile.age)
        assertEquals(62.0, profile.weightKg, 0.01)
        assertEquals(168.0, profile.heightCm, 0.01)
        assertEquals("Imperial", profile.preferredUnits)
    }

    @Test
    fun testSaveCompletedActivity() = runTest {
        // Given a running activity in TrackingService state
        val dummyPoint = LocationPoint(37.7749, -122.4194, System.currentTimeMillis())
        TrackingService.resetState()
        
        // Simulating tracker active state
        val mockField = TrackingService::class.java.getDeclaredField("_trackingState")
        mockField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = mockField.get(null) as kotlinx.coroutines.flow.MutableStateFlow<TrackingState>
        stateFlow.value = TrackingState(
            isTracking = false,
            activityType = "Running",
            startTime = System.currentTimeMillis() - 10000,
            elapsedSeconds = 1000L,
            distanceMeters = 5000.0,
            calories = 650.0,
            routePoints = listOf(dummyPoint)
        )

        // When we save the completed activity
        var savedId = -1L
        viewModel.saveCompletedActivity("Morning run") { id ->
            savedId = id
        }

        // Then it should call saveActivityWithPoints and return the id
        assertEquals(1L, savedId)

        // Check it is in the database
        val activities = fakeDao.activities
        assertEquals(1, activities.size)
        assertEquals("Running", activities[0].activityType)
        assertEquals("Morning run", activities[0].notes)
        assertEquals(5000.0, activities[0].distanceMeters, 0.01)

        // Check points are saved
        val points = fakeDao.activityPoints
        assertEquals(1, points.size)
        assertEquals(37.7749, points[0].latitude, 0.0001)
    }

    @Test
    fun testDeleteActivity() = runTest {
        val record = ActivityRecord(
            id = 45L,
            activityType = "Cycling",
            startTime = System.currentTimeMillis(),
            endTime = System.currentTimeMillis() + 5000,
            durationSeconds = 5000,
            distanceMeters = 20000.0,
            calories = 400.0,
            avgSpeed = 4.0,
            avgPace = 250.0,
            notes = "",
            isSynced = false
        )
        fakeDao.activities.add(record)

        var list = viewModel.allActivities.first()
        assertEquals(1, list.size)

        viewModel.deleteActivity(record)

        list = viewModel.allActivities.first()
        assertEquals(0, list.size)
    }
}

class FakeActivityDaoForActivity : ActivityDao {
    var profile: UserProfile? = null
    val activities = mutableListOf<ActivityRecord>()
    val activityPoints = mutableListOf<ActivityPoint>()

    override suspend fun insertActivity(activity: ActivityRecord): Long {
        val id = (activities.size + 1).toLong()
        activities.add(activity.copy(id = id))
        return id
    }

    override suspend fun insertActivityPoints(points: List<ActivityPoint>) {
        activityPoints.addAll(points)
    }

    override fun getAllActivities(): Flow<List<ActivityRecord>> = flow {
        emit(activities.reversed())
    }

    override suspend fun getActivityById(activityId: Long): ActivityRecord? {
        return activities.firstOrNull { it.id == activityId }
    }

    override fun getPointsForActivity(activityId: Long): Flow<List<ActivityPoint>> = flow {
        emit(activityPoints.filter { it.activityId == activityId })
    }

    override suspend fun getPointsForActivitySync(activityId: Long): List<ActivityPoint> {
        return activityPoints.filter { it.activityId == activityId }
    }

    override suspend fun deleteActivity(activity: ActivityRecord) {
        activities.removeIf { it.id == activity.id }
        activityPoints.removeIf { it.activityId == activity.id }
    }

    override suspend fun updateActivityNotes(id: Long, notes: String) {
        val index = activities.indexOfFirst { it.id == id }
        if (index != -1) {
            activities[index] = activities[index].copy(notes = notes)
        }
    }

    override suspend fun markActivitiesAsSynced(ids: List<Long>) {
        ids.forEach { id ->
            val index = activities.indexOfFirst { it.id == id }
            if (index != -1) {
                activities[index] = activities[index].copy(isSynced = true)
            }
        }
    }

    override fun getTotalDistanceMetersSince(since: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.distanceMeters })
    }

    override fun getTotalCaloriesSince(since: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.calories })
    }

    override fun getActivityCountSince(since: Long): Flow<Int> = flow {
        emit(activities.count { it.startTime >= since })
    }

    override fun getTotalDurationSecondsSince(since: Long): Flow<Long> = flow {
        emit(activities.filter { it.startTime >= since }.sumOf { it.durationSeconds })
    }

    override fun getActiveDayCountSince(since: Long): Flow<Int> = flow {
        val count = activities.filter { it.startTime >= since }
            .map { it.startTime / (24 * 60 * 60 * 1000) }
            .distinct()
            .size
        emit(count)
    }

    override fun getTodayCaloriesBurned(startOfDay: Long): Flow<Double> = flow {
        emit(activities.filter { it.startTime >= startOfDay }.sumOf { it.calories })
    }

    override suspend fun insertUserProfile(profile: UserProfile) {
        this.profile = profile
    }

    override fun getUserProfile(): Flow<UserProfile?> = flow {
        emit(profile)
    }

    override suspend fun getUserProfileSync(): UserProfile? {
        return profile
    }

    override suspend fun getTotalCaloriesBurnedInRange(since: Long, until: Long): Double? {
        return activities.filter { it.startTime in since until until }.sumOf { it.calories }
    }
}
