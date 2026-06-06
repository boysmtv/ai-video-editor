package com.changecut.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.changecut.core.database.dao.ProjectDao
import com.changecut.core.database.entity.ProjectEntity

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
