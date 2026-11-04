package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.FavoriteApp
import com.example.data.model.HiddenApp

@Database(
    entities = [FavoriteApp::class, HiddenApp::class],
    version = 1,
    exportSchema = false
)
abstract class LumiaDatabase : RoomDatabase() {
    abstract fun favoriteAppDao(): FavoriteAppDao
    abstract fun hiddenAppDao(): HiddenAppDao

    companion object {
        @Volatile
        private var INSTANCE: LumiaDatabase? = null

        fun getInstance(context: Context): LumiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LumiaDatabase::class.java,
                    "lumia_launcher.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
