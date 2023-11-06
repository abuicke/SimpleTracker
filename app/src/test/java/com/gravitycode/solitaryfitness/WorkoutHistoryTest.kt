package com.gravitycode.solitaryfitness

import com.gravitycode.solitaryfitness.track_reps.domain.WorkoutHistory
import com.gravitycode.solitaryfitness.track_reps.util.Workout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

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
 * */
class WorkoutHistoryTest {

    @Test
    fun `empty WorkoutHistory returns zero for all values`() {
        val workoutHistory = WorkoutHistory()
        for (workout in Workout.values()) {
            assertEquals(0, workoutHistory[workout])
        }
    }

    @Test
    fun `assigning negative values in constructor throws IllegalArgumentException`() {
        assertThrows(
            "WorkoutHistory should not accept negative values",
            IllegalArgumentException::class.java
        ) {
            WorkoutHistory(
                mapOf(
                    Workout.BURPEE to -50,
                    Workout.HANDSTAND_PRESS_UP to 10,
                    Workout.SQUAT_THRUST to 100
                )
            )
        }
    }

    @Test
    fun `assigning negative values in setter throws IllegalArgumentException`() {
        assertThrows(
            "WorkoutHistory should not accept negative values",
            IllegalArgumentException::class.java
        ) {
            val history = WorkoutHistory()
            history[Workout.SIT_UP] = -50
            history[Workout.STAR_JUMP] = 12
            history[Workout.HANDSTAND_PRESS_UP] = -15
        }
    }

    @Test
    fun `does get() retrieve what set() sets`() {
        val workoutHistory = WorkoutHistory()

        workoutHistory[Workout.STAR_JUMP] = 15
        workoutHistory[Workout.BURPEE] = 5
        workoutHistory[Workout.SQUAT_THRUST] = 100

        assertEquals(15, workoutHistory[Workout.STAR_JUMP])
        assertEquals(5, workoutHistory[Workout.BURPEE])
        assertEquals(100, workoutHistory[Workout.SQUAT_THRUST])
    }

    @Test
    fun `does empty WorkoutHistory equal empty WorkoutHistory`() {
        assertEquals(
            "empty WorkoutHistory objects should be equal",
            WorkoutHistory(),
            WorkoutHistory()
        )
    }

    @Test
    fun `does partial WorkoutHistory equal partial WorkoutHistory with the same values`() {
        val workoutHistory_1 = WorkoutHistory(
            mapOf(
                Workout.HANDSTAND_PRESS_UP to 15,
                Workout.PRESS_UP to 50,
                Workout.SQUAT_THRUST to 30,
                Workout.STEP_UP to 100
            )
        )

        val workoutHistory_2 = WorkoutHistory()
        workoutHistory_2[Workout.HANDSTAND_PRESS_UP] = 15
        workoutHistory_2[Workout.PRESS_UP] = 50
        workoutHistory_2[Workout.SQUAT_THRUST] = 30
        workoutHistory_2[Workout.STEP_UP] = 100

        assertEquals(
            "WorkoutHistory objects with the same values should be equal",
            workoutHistory_1,
            workoutHistory_2
        )
    }

    @Test
    fun `does hashCode() return the same value for equal WorkoutHistory objects`() {
        val workoutHistory_1 = WorkoutHistory(
            mapOf(
                Workout.STAR_JUMP to 15,
                Workout.BURPEE to 50,
                Workout.HANDSTAND_PRESS_UP to 30,
            )
        )

        val workoutHistory_2 = WorkoutHistory(
            mapOf(
                Workout.STAR_JUMP to 15,
                Workout.BURPEE to 50,
                Workout.HANDSTAND_PRESS_UP to 30,
            )
        )

        assertEquals(workoutHistory_1, workoutHistory_2)
        assertEquals(workoutHistory_1.hashCode(), workoutHistory_2.hashCode())
    }
}