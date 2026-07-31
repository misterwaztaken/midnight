package com.kaz.midnight

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Dream::class, Tag::class, DreamTagCrossRef::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dreamDao(): DreamDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "midnight-db"
                ).addMigrations(MIGRATION_2_3)
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
