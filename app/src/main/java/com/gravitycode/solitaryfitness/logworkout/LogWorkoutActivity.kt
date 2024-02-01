package com.gravitycode.solitaryfitness.logworkout

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.gravitycode.solitaryfitness.R
import com.gravitycode.solitaryfitness.app.AppState
import com.gravitycode.solitaryfitness.app.FlowLauncher
import com.gravitycode.solitaryfitness.app.SolitaryFitnessApp
import com.gravitycode.solitaryfitness.app.ui.SolitaryFitnessTheme
import com.gravitycode.solitaryfitness.auth.Authenticator
import com.gravitycode.solitaryfitness.auth.User
import com.gravitycode.solitaryfitness.logworkout.data.repo.WorkoutLogsRepositoryFactory
import com.gravitycode.solitaryfitness.logworkout.data.sync.SyncDataService
import com.gravitycode.solitaryfitness.logworkout.data.sync.SyncMode
import com.gravitycode.solitaryfitness.logworkout.presentation.LogWorkoutScreen
import com.gravitycode.solitaryfitness.logworkout.presentation.LogWorkoutViewModel
import com.gravitycode.solitaryfitness.util.android.Log
import com.gravitycode.solitaryfitness.util.android.Snackbar
import com.gravitycode.solitaryfitness.util.android.Toaster
import com.gravitycode.solitaryfitness.util.android.data.DataStoreManager
import com.gravitycode.solitaryfitness.util.android.data.stringSetPreferencesKey
import com.gravitycode.solitaryfitness.util.error
import com.gravitycode.solitaryfitness.util.net.InternetMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.annotation.concurrent.NotThreadSafe
import javax.inject.Inject

/**
 * TODO: Need a splash screen to display, the same as the launch screen until all dependencies are finished
 *  loading. Just set content first with the splash screen at the beginning of onCreate, and then with the
 *  main content again at the end. Or maybe put it in onResume? I don't know
 *
 * TODO: Need to gracefully recover from exceptions anywhere they're thrown. Look through the source code
 *  and look for anywhere there's an explicit `throw` and change this.
 *
 * TODO: What happens when I'm connected to the internet on sign-in but not when adding logs or on sign out?
 *  Is Firebase able to save the logs added offline and then upload them when there's a connection?
 *
 * TODO: Should make an abstract activity that handles ActivityComponent stuff the same way Application
 *  handles the Application component stuff?
 *
 * TODO: What happens if I kill the internet mid-sync or when uploading a workout log? Should have
 *  [SyncDataService] observe [InternetMonitor], I think firestore has an option to manage this itself,
 *  how does it work and does it need to be enabled?
 *
 * TODO: Do I need to redo how I write the getInstance pattern?
 *  [https://stackoverflow.com/questions/35587652/kotlin-thread-safe-native-lazy-singleton-with-parameter]
 *  [https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom#Example_Java_Implementation]
 *  [https://stackoverflow.com/questions/17799976/why-is-static-inner-class-singleton-thread-safe/17800038]
 *  [https://stackoverflow.com/questions/6109896/singleton-pattern-bill-pughs-solution]
 *
 * TODO: Is `@Volatile` required on singleton pattern?
 *  [https://stackoverflow.com/questions/59208041/do-we-need-volatile-when-implementing-singleton-using-double-check-locking]
 *
 * TODO: [lifecycleScope] on cancels jobs when the activity is destroyed. What is the point of this? (I guess
 *  it's for moving between activities, but with something like the [InternetMonitor] in continues to
 *  observe the state even when the activity is off-screen and in the `STOPPED` state) What scope cancels the
 *  couroutines when the activity goes off-screen?
 *
 * TODO: Add Log.v everywhere
 * TODO: Add a test that signs out on UIAutomator.
 * TODO: Figure out DataStore issue.
 * TODO: Write test to test UI with firebase that doesn't choosing an account to sign in with.
 * TODO: Use Mockito for Firestore: https://softwareengineering.stackexchange.com/questions/450508
 * TODO: Test no internet connection
 * TODO: Need error handling and return [Result] everywhere preferences store is accessed.
 * TODO: Check to see of there are other locations where I can use runCatching to return [Result]s
 * TODO: Write test for [AppControllerSettings]
 * TODO: Are there any places where it would be more profitable to us async/await? (Anywhere a result is
 *  waited for, what about logging in and out?)
 * TODO: `onEvent(DateSelected)` still being called 3 times
 * TODO: Am I nesting a `withContext` inside a `launch` anywhere? Replace with `launch(Dispatchers...)`
 * */
class LogWorkoutActivity : ComponentActivity(), FlowLauncher {

    private companion object {

        const val TAG = "MainActivity"
    }

    @Inject lateinit var applicationScope: CoroutineScope
    @Inject lateinit var dataStoreManager: DataStoreManager
    @Inject lateinit var authenticator: Authenticator
    @Inject lateinit var toaster: Toaster
    @Inject lateinit var internetMonitor: InternetMonitor
    @Inject lateinit var syncDataService: SyncDataService
    @Inject lateinit var repositoryFactory: WorkoutLogsRepositoryFactory
    @Inject lateinit var logWorkoutViewModel: LogWorkoutViewModel

    private val appState = MutableSharedFlow<AppState>(1)
    private val snackbar = mutableStateOf<Snackbar?>(null)

    private lateinit var appControllerSettings: AppControllerSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate")

        val app = application as SolitaryFitnessApp
        app.activityComponent(this, appState, this)
            .logWorkoutComponentBuilder()
            .build()
            .inject(this)

        applicationScope.launch {
            internetMonitor.subscribe().collect { networkState ->
                showSnackbar(
                    Snackbar(
                        message = networkState.toString(),
                        duration = SnackbarDuration.Short
                    )
                )
            }
        }

        lifecycleScope.launch {
            val currentUser = authenticator.getSignedInUser()
            appState.emit(AppState(currentUser))

            appControllerSettings = AppControllerSettings.getInstance(dataStoreManager)

            setContent {
                SolitaryFitnessTheme {
                    SnackbarHost {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            LogWorkoutScreen(logWorkoutViewModel)
                        }
                    }
                }
            }
        }
    }

    @Composable
    @SuppressLint("ComposableNaming")
    private fun SnackbarHost(content: @Composable () -> Unit) {

        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { snackbarData: SnackbarData ->
                    Snackbar(
                        modifier = Modifier.padding(22.dp),
                        action = {
                            if (snackbar.value!!.action != null) {
                                Button(
                                    onClick = snackbar.value!!.action!!.onClick
                                ) {
                                    Text(snackbar.value!!.action!!.text)
                                }
                            }
                        }
                    ) {
                        if (snackbar.value != null) {
                            Text(snackbar.value!!.message)
                        }
                    }
                }
            }
        ) { padding ->
            Log.d(TAG, "ignoring padding from scaffold $padding")
            content()
        }

        if (snackbar.value != null) {
            LaunchedEffect(snackbar.value) {
                Log.v(TAG, "launched effect on ${snackbar.value}")
                lifecycleScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "",
                        duration = snackbar.value!!.duration
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.v(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.v(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy")
        if (!isChangingConfigurations) {
            applicationScope.cancel("MainActivity destroyed")
            Log.v(TAG, "cancelled application coroutine scope")
        }
    }

    override fun launchSignInFlow() {
        if (authenticator.isUserSignedIn()) {
            val currentUser = authenticator.getSignedInUser()!!
            val name = currentUser.name ?: currentUser.email ?: currentUser.id
            toaster.toast("You are already signed in as $name")
            error("user is already signed in as $name")
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                authenticator.signIn()
            }

            val offlineRepository = repositoryFactory.getOfflineRepository()
            val firstRecord = offlineRepository.metaData.getRecords().firstOrNull()
            val hasOfflineData = firstRecord != null

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                val hasUserPreviouslySignedIn = appControllerSettings.hasUserPreviouslySignedIn(user)
                if (!hasUserPreviouslySignedIn) {
                    val historyResult = appControllerSettings.addUserToSignInHistory(user)
                    if (historyResult.isSuccess) {
                        Log.i(TAG, "successfully added user ${user.email} to sign in history")
                    } else {
                        error("failed to add user ${user.email} to sign in history")
                    }
                    if (hasOfflineData) {
                        launchSyncOfflineDataFlow {
                            appState.emit(AppState(user))
                        }
                    } else {
                        appState.emit(AppState(user))
                    }
                } else {
                    appState.emit(AppState(user))
                }
                toaster.toast("Signed in: ${user.email}")
                Log.i(TAG, "signed in as user: $user")
            } else {
                toaster.toast("Failed to sign in")
                error("Sign in failed", result)
            }
        }
    }

    override fun launchSignOutFlow() {
        if (!authenticator.isUserSignedIn()) {
            toaster.toast("Can't sign out, you're not signed in")
            error("no user is signed in")
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                authenticator.signOut()
            }

            if (result.isSuccess) {
                appState.emit(AppState(null))
                toaster.toast("Signed out")
                Log.i(TAG, "signed out")
            } else {
                toaster.toast("Failed to sign out")
                error("Sign out failed", result)
            }
        }
    }

    override fun launchSyncOfflineDataFlow() = launchSyncOfflineDataFlow(null)

    private fun showSnackbar(snackbar: Snackbar) {
        this.snackbar.value = snackbar
    }

    private fun launchSyncOfflineDataFlow(onComplete: (suspend () -> (Unit))?) {
        AlertDialog.Builder(this)
            .setTitle(R.string.transfer_dialog_title)
            .setMessage(R.string.transfer_dialog_message)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                dialog.dismiss()
                lifecycleScope.launch {
                    val progressDialog = AlertDialog.Builder(this@LogWorkoutActivity)
                        .setView(R.layout.sync_progress_dialog)
                        .setCancelable(false)
                        .setOnDismissListener {
                            toaster.toast("Sync complete")
                        }
                        .show()

                    try {
                        withContext(Dispatchers.IO) {
                            syncDataService.sync(SyncMode.OVERWRITE).collect { resultOf ->
                                if (resultOf.isFailure) {
                                    withContext(Dispatchers.Main) {
                                        toaster.toast("Sync failed for ${resultOf.subject}")
                                    }
                                } else {
                                    Log.i(TAG, "successfully synced ${resultOf.subject}")
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        toaster.toast("Sync failed...")
                        error(
                            "sync data service failed: ${t.message}",
                            t
                        )
                    } finally {
                        progressDialog.dismiss()
                        onComplete?.invoke()
                    }
                }
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                lifecycleScope.launch {
                    dialog.dismiss()
                    onComplete?.invoke()
                }
            }
            .show()
    }
}

@NotThreadSafe
private class AppControllerSettings private constructor(dataStoreManager: DataStoreManager) {

    companion object {

        private val USERS_KEY = stringSetPreferencesKey("users")

        private var instance: AppControllerSettings? = null

        /**
         * Return the singleton instance of [AppControllerSettings]
         * */
        suspend fun getInstance(dataStoreManager: DataStoreManager): AppControllerSettings {
            return instance ?: AppControllerSettings(dataStoreManager).apply {
                try {
                    val preferences = withContext(Dispatchers.IO) {
                        preferencesStore.data.first()
                    }
                    val userIds: Set<String>? = preferences[USERS_KEY]
                    if (userIds != null) {
                        users.addAll(userIds)
                    }
                } catch (ioe: IOException) {
                    error(
                        "failed to read app controller settings from preferences store",
                        ioe
                    )
                }

                instance = this
            }
        }
    }

    private val preferencesStore = dataStoreManager.datastore("app_controller")
    private val users: MutableSet<String> = mutableSetOf()

    suspend fun addUserToSignInHistory(user: User): Result<Unit> {
        return runCatching {
            withContext(Dispatchers.IO) {
                preferencesStore.edit { preferences ->
                    users.add(user.id)
                    preferences[USERS_KEY] = users
                }
            }
        }
    }

    fun hasUserPreviouslySignedIn(user: User): Boolean {
        return users.contains(user.id)
    }
}