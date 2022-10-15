package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.android.architecture.blueprints.todoapp.MainCoroutineRule
import com.example.android.architecture.blueprints.todoapp.getOrAwaitValue
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {


    //TODO: provide testing to the SaveReminderView and its live data objects

    private val workoutReminder = ReminderDataItem("workout","chest workout","Balance",1231385.235,56498723.325,"1")
    private val courseReminder = ReminderDataItem("Advanced Android","Compose","Udacity",1231385.235,56498723.325,"2")
    private val buyGroceryReminder = ReminderDataItem("Hp","fruits","deep",1231385.235,56498723.325,"3")


    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var repo:FakeDataSource


    @Before
    fun setup(){
        repo = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(),repo)
    }

    @After
    fun tearDown(){
        stopKoin()
    }

    @Test
    fun onClear_clearAllData(){
        viewModel.onClear()

        assertThat(viewModel.reminderTitle.value,`is`(nullValue()))
        assertThat(viewModel.reminderDescription.value,`is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.value,`is`(nullValue()))
        assertThat(viewModel.selectedPOI.value,`is`(nullValue()))
        assertThat(viewModel.latitude.value,`is`(nullValue()))
        assertThat(viewModel.longitude.value,`is`(nullValue()))
    }


    @Test
    fun validateEnteredData_titleEmpty_emitNeedTitleAndReturnFalse(){
        workoutReminder.title = ""
        val result = viewModel.validateEnteredData(workoutReminder)
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(),`is`(R.string.err_enter_title))

        assertThat(result,`is`(false))
    }

    @Test
    fun validateEnteredData_locationEmpty_emitNeedTitleAndReturnFalse(){
        workoutReminder.location = ""
        val result = viewModel.validateEnteredData(workoutReminder)
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(),`is`(R.string.err_select_location))

        assertThat(result,`is`(false))
    }

    @Test
    fun validateEnteredData_validReminder_returnTrue(){
        val result = viewModel.validateEnteredData(workoutReminder)

        assertThat(result,`is`(true))
    }

    @Test
    fun saveReminder_showLoading(){
        mainCoroutineRule.pauseDispatcher()
        viewModel.saveReminder(workoutReminder)

        assertThat(viewModel.showLoading.getOrAwaitValue(),`is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(),`is`(false))
    }

    @Test
    fun saveReminder_validData_saveInRepo() = runBlockingTest{
        viewModel.saveReminder(workoutReminder)

        val result = repo.getReminder(workoutReminder.id)

        assertThat(result, `is`(notNullValue()))
    }

    @Test
    fun saveReminder_validData_addGeofenceAndShowSavedToast(){
        viewModel.saveReminder(workoutReminder)

        assertThat(viewModel.addGeofence.getOrAwaitValue(),`is`(workoutReminder))
        assertThat(viewModel.showToast.getOrAwaitValue(),`is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_failureCase_emitErrorHappen(){
        repo.setShouldReturnError(true)

        viewModel.saveReminder(workoutReminder)

        assertThat(viewModel.showToast.getOrAwaitValue(),`is`("Error happened"))
    }

    @Test
    fun onDoneTask_validData_removeTaskFromRepo() = runBlockingTest{
        viewModel.saveReminder(workoutReminder)

        viewModel.onDoneTask(workoutReminder)

        val result = repo.getReminder(workoutReminder.id) as Result.Error

        assertThat(result.message,`is`("Reminder not found!"))
    }

    @Test
    fun onDoneTask_issueWhileAdding_emitError() = runBlockingTest{
        viewModel.saveReminder(workoutReminder)

        repo.setShouldReturnError(true)

        viewModel.onDoneTask(workoutReminder)

        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(),`is`(R.string.error_remove_reminder))
    }

}