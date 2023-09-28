package com.gravitycode.simpletracker.workout_list

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gravitycode.simpletracker.workout_list.data.WorkoutHistoryRepository
import com.gravitycode.simpletracker.workout_list.data.WorkoutHistoryRepositoryImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * TODO: Need to use Android scopes, e.g. @ActivityScope
 * */
@Module
class WorkoutListModule {

    @Provides @Singleton fun providesWorkoutHistoryDataStore(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("workout_history")
        }
    }

    @Provides @Singleton fun provideWorkoutHistoryRepository(
        dataStore: DataStore<Preferences>
    ): WorkoutHistoryRepository = WorkoutHistoryRepositoryImpl(dataStore)
}