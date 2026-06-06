package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategoryFlow(category: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)
}

@Dao
interface WorkflowDao {
    @Query("SELECT * FROM workflows ORDER BY id DESC")
    fun getAllWorkflowsFlow(): Flow<List<WorkflowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: WorkflowEntity)

    @Update
    suspend fun updateWorkflow(workflow: WorkflowEntity)

    @Delete
    suspend fun deleteWorkflow(workflow: WorkflowEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()
}

@Dao
interface PinterestPinDao {
    @Query("SELECT * FROM pinterest_pins ORDER BY id DESC")
    fun getAllPinsFlow(): Flow<List<PinterestPinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPin(pin: PinterestPinEntity)

    @Update
    suspend fun updatePin(pin: PinterestPinEntity)

    @Delete
    suspend fun deletePin(pin: PinterestPinEntity)
}
