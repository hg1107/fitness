package com.example.fitnesstracker.di

import android.content.Context
import com.example.fitnesstracker.data.WorkoutDatabase
import com.example.fitnesstracker.data.WorkoutDao
import com.example.fitnesstracker.data.ActivityDao
import com.example.fitnesstracker.data.NutritionDao
import com.example.fitnesstracker.data.BodyMeasurementDao
import com.example.fitnesstracker.data.WorkoutProgramDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return WorkoutDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(database: WorkoutDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    @Singleton
    fun provideActivityDao(database: WorkoutDatabase): ActivityDao {
        return database.activityDao()
    }

    @Provides
    @Singleton
    fun provideNutritionDao(database: WorkoutDatabase): NutritionDao {
        return database.nutritionDao()
    }

    @Provides
    @Singleton
    fun provideBodyMeasurementDao(database: WorkoutDatabase): BodyMeasurementDao {
        return database.bodyMeasurementDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutProgramDao(database: WorkoutDatabase): WorkoutProgramDao {
        return database.workoutProgramDao()
    }
}
