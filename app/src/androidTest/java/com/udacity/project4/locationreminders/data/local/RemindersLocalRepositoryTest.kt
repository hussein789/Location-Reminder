package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

//    TODO: Add testing implementation to the RemindersLocalRepository.kt

    private lateinit var reminderLocalDataSource: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val workoutReminder = ReminderDTO("workout","chest workout","balancer",
        1231354.264,56498.221,"1")

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
        RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reminderLocalDataSource = RemindersLocalRepository(database.reminderDao(),Dispatchers.Main)
    }

    @After
    fun tearDown(){
        database.close()
    }

    @Test
    fun getReminder_existReminder_ReturnSavedReminder() = runBlocking {
        reminderLocalDataSource.saveReminder(workoutReminder)

        val result = reminderLocalDataSource.getReminder(workoutReminder.id) as Result.Success

        assertThat(result.data.id,`is`(workoutReminder.id))
        assertThat(result.data.title,`is`(workoutReminder.title))
        assertThat(result.data.location,`is`(workoutReminder.location))
        assertThat(result.data.longitude,`is`(workoutReminder.longitude))
    }

    @Test
    fun getReminders_reminderNotExist_returnError() = runBlocking{
        reminderLocalDataSource.saveReminder(workoutReminder)

        val result = reminderLocalDataSource.getReminder("5") as Result.Error

        assertThat(result.message,`is`("Reminder not found!"))
    }

    @Test
    fun deleteAllReminders_returnEmptyList() = runBlocking {
        reminderLocalDataSource.saveReminder(workoutReminder)

        reminderLocalDataSource.deleteAllReminders()

        val result = reminderLocalDataSource.getReminders() as Result.Success

        assertThat(result.data,`is`(emptyList<ReminderDTO>()))
    }

}