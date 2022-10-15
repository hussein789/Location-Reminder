package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.example.android.architecture.blueprints.todoapp.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: RemindersListViewModel
    private lateinit var repo: FakeDataSource

    private val workoutReminder = ReminderDataItem("workout","chest workout","Balance",1231385.235,56498723.325,"1")
    private val courseReminder = ReminderDataItem("Advanced Android","Compose","Udacity",1231385.235,56498723.325,"2")
    private val buyGroceryReminder = ReminderDataItem("Hp","fruits","deep",1231385.235,56498723.325,"3")

    @Before
    fun setup() {
        repo = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), repo)
    }

    @After
    fun tearDown(){
        stopKoin()
    }

    @Test
    fun loadReminders_showLoading(){
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()

        assertThat(viewModel.showLoading.getOrAwaitValue(),`is`(true))
        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(),`is`(false))
    }

    @Test
    fun loadReminders_emptyListInDataSource_emitEmptyList(){
        viewModel.loadReminders()

        val result = viewModel.remindersList.getOrAwaitValue()

        assertThat(result, IsEqual(emptyList<ReminderDataItem>()))
    }

    @Test
    fun loadReminders_hasItems_emitDataSourceItems() = runBlockingTest {
        addItemToRepo()

        viewModel.loadReminders()

        val result = viewModel.remindersList.getOrAwaitValue()

        assertThat(result[0].title,`is`(workoutReminder.title))
        assertThat(result[0].description,`is`(workoutReminder.description))
        assertThat(result[0].longitude,`is`(workoutReminder.longitude))
        assertThat(result[0].id,`is`(workoutReminder.id))
    }

    private suspend fun addItemToRepo() {
        repo.saveReminder(
            ReminderDTO(
                workoutReminder.title,
                workoutReminder.description,
                workoutReminder.location,
                workoutReminder.latitude,
                workoutReminder.longitude,
                workoutReminder.id
            )
        )
    }


    @Test
    fun loadReminders_failGettingData_showSnackBarForFailure(){
        repo.setShouldReturnError(true)

        viewModel.loadReminders()

        val result = viewModel.showSnackBar.getOrAwaitValue()

        assertThat(result,`is`("Error getting data"))
    }

    @Test
    fun invalidateShowNoData_noData_emitNoDataBoolean() = runBlockingTest{
        viewModel.loadReminders()

        val result = viewModel.showNoData.getOrAwaitValue()

        assertThat(result,`is`(true))
    }

    @Test
    fun invalidateShowNoData_dataExit_emitFalseForNoData() = runBlockingTest{
        addItemToRepo()

        viewModel.loadReminders()

        val result = viewModel.showNoData.getOrAwaitValue()

        assertThat(result,`is`(false))
    }

    @Test
    fun onLogoutUser_logoutSuccessfully_deleteAllTasksAndNavigateToSignup() = runBlockingTest{
        addItemToRepo()

        viewModel.onLogoutUser()

        assertThat(viewModel.navigateToSingUp.getOrAwaitValue(),`is`(true))

        viewModel.loadReminders()

        assertThat(viewModel.remindersList.getOrAwaitValue(),`is`(emptyList()))
    }


}