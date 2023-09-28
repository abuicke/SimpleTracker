package com.gravitycode.simpletracker.workout_list

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.lifecycleScope
import com.gravitycode.simpletracker.app.SimpleTrackerApp
import com.gravitycode.simpletracker.app.ui.SimpleTrackerTheme
import com.gravitycode.simpletracker.workout_list.data.WorkoutHistory
import com.gravitycode.simpletracker.workout_list.data.WorkoutHistoryRepository
import com.gravitycode.simpletracker.workout_list.data.WorkoutHistoryRepositoryImpl
import com.gravitycode.simpletracker.workout_list.domain.WorkoutListViewModel
import com.gravitycode.simpletracker.workout_list.presentation.WorkoutListScreen
import com.gravitycode.simpletracker.workout_list.util.Workout
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * TODO: Finish reading Dagger articles:
 * [https://developer.android.com/training/dependency-injection/dagger-android#best-practices]
 * [https://distillery.com/blog/dagger-2-how-to-understand-and-use-components-and-modules/]
 **/



class WorkoutListActivity : ComponentActivity() {

    @Inject lateinit var workoutListViewModel: WorkoutListViewModel

    /**
     * Essential unit tests
     *
     * When following best practice, you should ensure you use unit tests in the following cases:
     *      1)  Unit tests for ViewModels, or presenters.
     *      2)  Unit tests for the data layer, especially repositories. Most of the data layer should
     *          be platform-independent. Doing so enables test doubles to replace database modules and
     *          remote data sources in tests. See the guide on using test doubles in Android
     *      3)  Unit tests for other platform-independent layers such as the Domain layer, as with
     *          use cases and interactors.
     *      4)  Unit tests for utility classes such as string manipulation and math.
     *
     * [https://developer.android.com/training/testing/fundamentals/what-to-test]
     *
     * TODO: Write tests for [WorkoutHistoryRepositoryImpl] using the same code as below
     * TODO: Inject [DataStore] into [WorkoutHistoryRepositoryImpl] with Dagger
     * TODO: get, set, inc and dec functions on [WorkoutHistory] need to be synchronized
     * */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as SimpleTrackerApp).appComponent.inject(this)

        setContent {
            SimpleTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkoutListScreen(workoutListViewModel)
                }
            }
        }
    }

    fun workoutHistoryDataStoreTest(dataStore: DataStore<Preferences>) {
        val repo: WorkoutHistoryRepository = WorkoutHistoryRepositoryImpl(dataStore)

        lifecycleScope.launch {
            repo.readWorkoutHistory().collect { workoutHistory ->
                Log.i("workout_history", "collect-1: $workoutHistory")
            }

            repo.writeWorkoutHistory(
                WorkoutHistory(
                    mapOf(
                        Workout.HANDSTAND_PRESS_UP to 4,
                        Workout.PRESS_UP to 8,
                        Workout.SIT_UP to 14,
                        Workout.SQUAT to 2,
                        Workout.SQUAT_THRUST to 4,
                        Workout.BURPEE to 25,
                        Workout.STAR_JUMP to 15,
                        Workout.STEP_UP to 0,
                    )
                )
            )

            repo.readWorkoutHistory().collect { workoutHistory ->
                Log.i("workout_history", "collect-2: $workoutHistory")
            }

            repo.writeWorkoutHistory(
                WorkoutHistory(
                    mapOf(
                        Workout.HANDSTAND_PRESS_UP to 12,
                        Workout.PRESS_UP to 32,
                        Workout.SIT_UP to 85,
                        Workout.SQUAT to 36,
                        Workout.SQUAT_THRUST to 100,
                        Workout.BURPEE to 200,
                        Workout.STAR_JUMP to 500,
                        Workout.STEP_UP to 50,
                    )
                )
            )

            repo.readWorkoutHistory().collect { workoutHistory ->
                Log.i("workout_history", "collect-3: $workoutHistory")
            }
        }
    }
}