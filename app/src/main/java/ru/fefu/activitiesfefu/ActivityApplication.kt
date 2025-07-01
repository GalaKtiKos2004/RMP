package ru.fefu.activitiesfefu

import android.app.Application
import androidx.room.Room
import ru.fefu.activitiesfefu.data.AppDatabase

class ActivityApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "activity-db"
        )
        .addMigrations(AppDatabase.MIGRATION_4_5)
        .fallbackToDestructiveMigration()
        .build()
    }
    
    companion object {
        private var instance: ActivityApplication? = null
        
        fun getInstance(): ActivityApplication {
            return instance!!
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
} 