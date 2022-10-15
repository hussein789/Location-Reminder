package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

//    TODO: Add testing implementation to the RemindersDao.kt

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private val workoutReminder = ReminderDTO("workout","chest workout","balancer",
    1231354.264,56498.221,"1")

    @Before
    fun setup(){
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
        RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown(){
        database.close()
    }

    @Test
    fun getReminders_returnDatabaseReminders() = runBlockingTest{
        database.reminderDao().saveReminder(workoutReminder)

        val result = database.reminderDao().getReminders()

        assertThat(result.get(0).id,`is`(workoutReminder.id))
        assertThat(result.get(0).title,`is`(workoutReminder.title))
        assertThat(result.get(0).description,`is`(workoutReminder.description))
        assertThat(result.get(0).latitude,`is`(workoutReminder.latitude))
    }

    @Test
    fun getReminderById_returnReminderById() = runBlockingTest{
        database.reminderDao().saveReminder(workoutReminder)

        val result = database.reminderDao().getReminderById(workoutReminder.id)

        assertThat(result,`is`(notNullValue()))
        assertThat(result?.id,`is`(workoutReminder.id))
        assertThat(result?.title,`is`(workoutReminder.title))
        assertThat(result?.description,`is`(workoutReminder.description))
        assertThat(result?.latitude,`is`(workoutReminder.latitude))
    }

    @Test
    fun deleteAllReminders_deleteAllReminders() = runBlockingTest{
        database.reminderDao().saveReminder(workoutReminder)

        database.reminderDao().deleteAllReminders()

        val result = database.reminderDao().getReminders()

        assertThat(result,`is`(emptyList()))
    }

}