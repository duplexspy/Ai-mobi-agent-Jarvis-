package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoryEntity::class,
        WorkflowEntity::class,
        AuditLogEntity::class,
        PinterestPinEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun pinterestPinDao(): PinterestPinDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        fun getDatabase(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
