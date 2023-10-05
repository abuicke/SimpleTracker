package com.gravitycode.simpletracker.workout_list.domain

import com.gravitycode.simpletracker.workout_list.util.Workout

sealed class WorkoutListEvent(val workout: Workout) {

    class Increment(wO: Workout, val quantity: Int) : WorkoutListEvent(wO)
}