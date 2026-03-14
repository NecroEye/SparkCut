package com.muratcangzm.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.muratcangzm.database.project.ProjectDao
import com.muratcangzm.database.project.ProjectEntity
import com.muratcangzm.database.project.ProjectSlotBindingEntity
import com.muratcangzm.database.project.ProjectTextValueEntity
import com.muratcangzm.database.project.ProjectTransitionOverrideEntity

@Database(
    entities = [
        ProjectEntity::class,
        ProjectSlotBindingEntity::class,
        ProjectTextValueEntity::class,
        ProjectTransitionOverrideEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SparkCutDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
