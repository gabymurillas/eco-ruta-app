package com.ecoruta.dispositivo.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PendingPositionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingPositionDao(): PendingPositionDao

    companion object {
        private const val NAME = "ecoruta.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                // The table is a disposable delivery queue (max two hours of data), so on a schema
                // change dropping it is acceptable. Add real migrations if durable tables appear.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
