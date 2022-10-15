package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private var reminders: MutableList<ReminderDTO> = mutableListOf()
) : ReminderDataSource {

//    TODO: Create a fake data source to act as a double to the real data source

    private var shouldReturnError = false

    fun setShouldReturnError(flag: Boolean) {
        shouldReturnError = flag
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) return Result.Error("Error getting data")
        return Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        if (shouldReturnError) throw java.lang.IllegalArgumentException("error while adding new reminder")
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) return Result.Error("Error getting reminder")
        val loadedReminder =
            reminders.firstOrNull { it.id == id } ?: return Result.Error("Reminder not found!")
        return Result.Success(loadedReminder)
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    override suspend fun removeReminder(reminder: ReminderDTO) {
        if(shouldReturnError) throw java.lang.IllegalArgumentException("error happens removing item")
        reminders.remove(reminder)
    }


}