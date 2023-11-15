package com.gravitycode.solitaryfitness.track_reps.presentation

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gravitycode.solitaryfitness.app.ActivityScope
import com.gravitycode.solitaryfitness.track_reps.data.WorkoutHistoryRepo
import com.gravitycode.solitaryfitness.track_reps.domain.WorkoutLog
import com.gravitycode.solitaryfitness.track_reps.util.Workout
import com.gravitycode.solitaryfitness.util.debugError
import com.gravitycode.solitaryfitness.util.ui.ToastDuration
import com.gravitycode.solitaryfitness.util.ui.Toaster
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrackRepsViewModel(
    private val toaster: Toaster,
    private val workoutHistoryRepo: WorkoutHistoryRepo
) : ViewModel() {

    private companion object {
        const val TAG = "TrackRepsViewModel"
    }

    private var currentDate: LocalDate = LocalDate.now()

    private val _state = mutableStateOf(TrackRepsState(currentDate))
    val state: State<TrackRepsState> = _state

    // Specifies whether a workout log already exists for
    // this date, in which case the update function should
    // be called on the repository. If a new record needs
    // to be created the write function should be used.
    private var doesRecordAlreadyExist = false

    init {
        loadWorkoutLog()
    }

    fun onEvent(event: TrackRepsEvent) {
        when (event) {
            is TrackRepsEvent.DateSelected -> changeDate(event.date)
            is TrackRepsEvent.Increment -> incrementWorkout(event.workout, event.quantity)
        }
    }

    private fun loadWorkoutLog() {
        viewModelScope.launch {
            val result = workoutHistoryRepo.readWorkoutLog(currentDate)
            if (result.isSuccess) {
                val workoutLog = result.getOrNull()
                if (workoutLog != null) {
                    doesRecordAlreadyExist = true
                    _state.value = TrackRepsState(currentDate, workoutLog)
                } else {
                    doesRecordAlreadyExist = false
                    _state.value = TrackRepsState(currentDate, WorkoutLog())
                }
            } else {
                debugError("failed to read workout log from repository", result)
            }
        }
    }

    private fun changeDate(date: LocalDate) {
        currentDate = date
        loadWorkoutLog()
    }

    /**
     * TODO: Could just retain a reference to the old TrackRepsState and reassign that if the update fails
     * TODO: Need to check that `quantity` isn't negative
     * */
    private fun incrementWorkout(workout: Workout, quantity: Int) {
        require(quantity >= 0) { "cannot increment by a negative value" }
        val newReps = state.value.log[workout] + quantity
        _state.value = TrackRepsState(currentDate, state.value.log.copy(workout, newReps))

        viewModelScope.launch {
            var firstTimeWrite = false
            val result = if (doesRecordAlreadyExist) {
                workoutHistoryRepo.updateWorkoutLog(currentDate, workout, newReps)
            } else {
                firstTimeWrite = true
                doesRecordAlreadyExist = true
                workoutHistoryRepo.writeWorkoutLog(currentDate, state.value.log)
            }
            if (result.isFailure) {
                doesRecordAlreadyExist = !firstTimeWrite && doesRecordAlreadyExist
                val oldReps = state.value.log[workout] - quantity
                _state.value = TrackRepsState(currentDate, state.value.log.copy(workout, oldReps))
                toaster("Couldn't save reps", ToastDuration.SHORT)
                debugError("Failed to write workout history to repository", result)
            } else {
                Log.v(TAG, "incrementWorkout(${workout.string}, $quantity)")
            }
        }
    }
}
