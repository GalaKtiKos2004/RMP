package ru.fefu.activitiesfefu.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ActivityEntity::class, UserEntity::class], version = 5, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun userDao(): UserDao
    
    companion object {
        // Миграция с версии 4 на версию 5: добавление поля comment в таблицу activities
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавляем столбец comment в таблицу activities с пустой строкой по умолчанию
                database.execSQL("ALTER TABLE activities ADD COLUMN comment TEXT NOT NULL DEFAULT ''")
            }
        }
    }
} 