package com.gravitycode.simpletracker.workout_list.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gravitycode.simpletracker.workout_list.presentation.preview.PreviewWorkoutListViewModel
import com.gravitycode.simpletracker.workout_list.util.Workout

/**
 * Composable functions should be idempotent, and free of side-effects:
 *
 * The rememberSaveable API behaves similarly to remember because it retains state across
 * recompositions, and also across activity or process recreation using the saved instance
 * state mechanism. For example, this happens, when the screen is rotated.
 *
 * [https://developer.android.com/jetpack/compose/state#restore-ui-state]
 * */

private val ADD_REPS_BG = Color(81, 207, 135)

@Composable
@Preview(showSystemUi = true)//, widthDp = 250)
fun WorkoutListScreen() {
    WorkoutListScreen(viewModel = PreviewWorkoutListViewModel(10000))
}

/**
 * TODO: Need to learn more about [NavController] and if it's even a good solution.
 * TODO: Once the reps get past 1000, should start using K notation, like 1.2K, 50.8K etc.
 * */
@Composable
fun WorkoutListScreen(
    modifier: Modifier = Modifier,
//    navController: NavController,
    viewModel: WorkoutListViewModel
) {
    val listState = viewModel.state.value

    LazyColumn(
        modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val workouts = Workout.values()
        items(workouts) { workout: Workout ->

            val isShowingRepButtons = remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .padding(12.dp, 6.dp, 12.dp, 6.dp)
                    .clickable {
                        if (!isShowingRepButtons.value) {
                            isShowingRepButtons.value = true
                        }
                    }
            ) {
                // TODO: Min and Max do the same thing.
                //  Don't know what the difference is.
                Box(modifier.height(IntrinsicSize.Min)) {
                    TitleAndCount(
                        title = workout.toPrettyString(),
                        count = listState[workout]
                    )
                    if (isShowingRepButtons.value) {
                        AddRepsButtonRow(Modifier.fillMaxSize()) { reps ->
                            isShowingRepButtons.value = false
                            viewModel.onEvent(WorkoutListEvent.Increment(workout, reps))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TitleAndCount(
    modifier: Modifier = Modifier,
    title: String,
    count: Int
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    top = 12.dp,
                    bottom = 12.dp
                )
                .weight(0.7f),
            text = title,
            fontSize = 24.sp
        )
        Text(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(0.3f),
            text = count.toString(),
            textAlign = TextAlign.Right,
            fontSize = 24.sp
        )
    }
}

@Composable
fun AddRepsButtonRow(modifier: Modifier = Modifier, onClickReps: (Int) -> Unit) {
    Row(
        modifier.background(ADD_REPS_BG),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    onClickReps(1)
                },
            text = "+1",
            textAlign = TextAlign.Center
        )
        Divider(
            color = Color.Black,
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    onClickReps(5)
                },
            text = "+5",
            textAlign = TextAlign.Center
        )
        Divider(
            color = Color.Black,
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    onClickReps(10)
                },
            text = "+10",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddRepsButton() {

}