package com.gravitycode.solitaryfitness.log_workout.presentation

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.gravitycode.solitaryfitness.app.AppController
import com.gravitycode.solitaryfitness.app.AppEvent
import com.gravitycode.solitaryfitness.log_workout.data.WorkoutLogsRepository
import com.gravitycode.solitaryfitness.log_workout.data.WorkoutLogsRepositoryFactory
import com.gravitycode.solitaryfitness.log_workout.domain.WorkoutLog
import com.gravitycode.solitaryfitness.log_workout.util.Workout
import com.gravitycode.solitaryfitness.util.debugError
import com.gravitycode.solitaryfitness.util.ui.Toaster
import com.gravitycode.solitaryfitness.util.ui.ViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Never collect a flow from the UI directly from launch or the launchIn extension function if the UI needs
 * to be updated. These functions process events even when the view is not visible. This behavior can lead
 * to app crashes. To avoid that, use [androidx.lifecycle.repeatOnLifecycle].
 *
 * See warning in (StateFlow and SharedFlow)[https://developer.android.com/kotlin/flow/stateflow-and-sharedflow]
 * */
class LogWorkoutViewModel(
    private val appController: AppController,
    private val toaster: Toaster,
    private val repositoryFactory: WorkoutLogsRepositoryFactory
) : ViewModel<LogWorkoutState, LogWorkoutEvent>() {

    private companion object {

        const val TAG = "TrackRepsViewModel"
    }

    override val state = mutableStateOf(LogWorkoutState())

    private lateinit var repository: WorkoutLogsRepository

    init {
        /**
         * TODO: Need to make sure this job is canceled when I no longer need to observe this state.
         *  Check why and how the Job was cancelled in the clean architecture project.
         * */
        viewModelScope.launch {
            appController.appState.collect { appState ->
                Log.d(TAG, "app state collected: $appState")
                state.value = state.value.copy(user = appState.user)
                repository = repositoryFactory.create(appState.isUserSignedIn())
                loadWorkoutLog()
            }
        }
    }

    override fun onEvent(event: AppEvent) {
        Log.v(TAG, "onEvent($event)")
        when (event) {
            is AppEvent.SignIn -> appController.requestSignIn()
            is AppEvent.SignOut -> appController.requestSignOut()
        }
    }

    override fun onEvent(event: LogWorkoutEvent) {
        Log.v(TAG, "onEvent($event)")
        when (event) {
            is LogWorkoutEvent.DateSelected -> changeDate(event.date)
            is LogWorkoutEvent.Increment -> incrementWorkout(event.workout, event.quantity)
            is LogWorkoutEvent.Reset -> resetReps()
            is LogWorkoutEvent.Edit -> editReps(event.mode)
        }
    }

    private suspend fun loadWorkoutLog() {
        val currentDate = state.value.date
        val result = repository.readWorkoutLog(currentDate)

        if (result.isSuccess) {
            val workoutLog = result.getOrNull()
            if (workoutLog != null) {
                state.value = state.value.copy(log = workoutLog)
            } else {
                val log = WorkoutLog()
                state.value = state.value.copy(log = log)
                repository.writeWorkoutLog(currentDate, log)
            }
        } else {
            debugError("failed to read workout log from repository", result)
        }
    }

    private fun changeDate(date: LocalDate) {
        if (date != state.value.date) {
            state.value = state.value.copy(date = date)
            viewModelScope.launch {
                loadWorkoutLog()
            }
        }
    }

    private fun incrementWorkout(workout: Workout, quantity: Int) {
        require(quantity >= 0) { "cannot increment by a negative value" }

        val oldState = state.value
        val currentDate = state.value.date
        val newReps = state.value.log[workout] + quantity
        state.value = state.value.copy(log = state.value.log.copy(workout, newReps))

        viewModelScope.launch {
            val result = repository.updateWorkoutLog(currentDate, workout, newReps)
            if (result.isFailure) {
                state.value = oldState
                toaster("Couldn't save reps")
                debugError("Failed to write workout log to repository", result)
            } else {
                Log.v(TAG, "incrementWorkout(${workout.string}, $quantity)")
            }
        }
    }

    private fun resetReps() {
        val oldState = state.value

        val log = WorkoutLog()
        state.value = state.value.copy(log = log)

        viewModelScope.launch {
            val result = repository.writeWorkoutLog(state.value.date, log)
            if (result.isFailure) {
                state.value = oldState
                toaster("Couldn't reset reps")
                debugError("Failed to reset reps and write to repository", result)
            } else {
                Log.v(TAG, "reps reset successfully")
            }
        }
    }

    /**
     * TODO: When in edit mode turn each Text that contains a rep count into an EditText and show two
     *  floating action buttons, one to save the changes that were made (floppy disk save icon) and one to
     *  cancel all the edits that were made (X icon [https://www.google.com/search?q=x+close+icon])
     * */
    private fun editReps(mode: LogWorkoutEvent.Edit.Mode) {
        state.value = state.value.copy(editMode = mode == LogWorkoutEvent.Edit.Mode.START)
    }
}
