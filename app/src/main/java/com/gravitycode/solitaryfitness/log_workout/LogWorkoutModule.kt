package com.gravitycode.solitaryfitness.log_workout

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.firebase.firestore.FirebaseFirestore
import com.gravitycode.solitaryfitness.BuildConfig
import com.gravitycode.solitaryfitness.app.AppController
import com.gravitycode.solitaryfitness.auth.Authenticator
import com.gravitycode.solitaryfitness.di.ApplicationScope
import com.gravitycode.solitaryfitness.log_workout.data.LazySyncDataService
import com.gravitycode.solitaryfitness.log_workout.data.LazyWorkoutLogsRepositoryFactory
import com.gravitycode.solitaryfitness.log_workout.data.PreferencesWorkoutLogsRepository
import com.gravitycode.solitaryfitness.log_workout.data.SyncDataService
import com.gravitycode.solitaryfitness.log_workout.data.WorkoutLogsRepository
import com.gravitycode.solitaryfitness.log_workout.data.WorkoutLogsRepositoryFactory
import com.gravitycode.solitaryfitness.log_workout.data.firestore.DebugFirestoreWorkoutLogsRepository
import com.gravitycode.solitaryfitness.log_workout.data.firestore.ProductionFirestoreWorkoutLogsRepository
import com.gravitycode.solitaryfitness.log_workout.presentation.LogWorkoutViewModel
import com.gravitycode.solitaryfitness.util.data.createPreferencesStoreFromFile
import com.gravitycode.solitaryfitness.util.data.firestoreSettings
import com.gravitycode.solitaryfitness.util.ui.Messenger
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
private annotation class InternalDependency

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
private annotation class OfflineRepository

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
private annotation class OnlineRepository

@Module
object LogWorkoutModule {

    @Provides
    @ApplicationScope
    @InternalDependency
    fun providesWorkoutLogsPreferencesStore(
        context: Context
    ): DataStore<Preferences> {
        return createPreferencesStoreFromFile(context, "workout_logs")
    }

    @Provides
    @ApplicationScope
    @OfflineRepository
    fun provideOfflineWorkoutLogsRepository(
        appController: AppController,
        @InternalDependency preferencesStore: DataStore<Preferences>
    ): WorkoutLogsRepository {
        return PreferencesWorkoutLogsRepository(appController, preferencesStore)
    }

    @Provides
    @InternalDependency
    fun providesFirebaseFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = firestoreSettings(
            persistentCacheSizeMb = 10
        )

        return firestore
    }

    @Provides
    @ApplicationScope
    @OnlineRepository
    fun provideOnlineWorkoutLogsRepository(
        appController: AppController,
        authenticator: Authenticator,
        @InternalDependency firestore: FirebaseFirestore
    ): WorkoutLogsRepository {
        val applicationScope = appController.applicationScope
        return if (BuildConfig.DEBUG) {
            DebugFirestoreWorkoutLogsRepository(applicationScope, authenticator, firestore)
        } else {
            ProductionFirestoreWorkoutLogsRepository(applicationScope, authenticator, firestore)
        }
    }

    @Provides
    @ApplicationScope
    fun providesWorkoutLogsRepositoryFactory(
        @OfflineRepository offlineRepository: Lazy<WorkoutLogsRepository>,
        @OnlineRepository onlineRepository: Lazy<WorkoutLogsRepository>
    ): WorkoutLogsRepositoryFactory {
        return LazyWorkoutLogsRepositoryFactory(offlineRepository, onlineRepository)
    }

    @Provides
    @ApplicationScope
    fun providesSyncDataService(
        @OfflineRepository offlineRepository: Lazy<WorkoutLogsRepository>,
        @OnlineRepository onlineRepository: Lazy<WorkoutLogsRepository>
    ): SyncDataService {
        return LazySyncDataService(offlineRepository, onlineRepository)
    }

    /**
     * TODO: This shouldn't necessarily be kept alive for the duration of the activity.
     * */
    @Provides
    fun providesLogWorkoutViewModel(
        appController: AppController,
        messenger: Messenger,
        factory: WorkoutLogsRepositoryFactory
    ) = LogWorkoutViewModel(appController, messenger, factory)
}