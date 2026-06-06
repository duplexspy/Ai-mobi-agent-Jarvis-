package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.flow.Flow

class JarvisRepository(private val database: JarvisDatabase) {

    private val memoryDao = database.memoryDao()
    private val workflowDao = database.workflowDao()
    private val auditLogDao = database.auditLogDao()
    private val pinterestPinDao = database.pinterestPinDao()

    // Memories
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemoriesFlow()
    fun getMemoriesByCategory(category: String): Flow<List<MemoryEntity>> =
        memoryDao.getMemoriesByCategoryFlow(category)

    suspend fun insertMemory(memory: MemoryEntity) {
        memoryDao.insertMemory(memory)
        logAction("Memory Added", "Saved memory for [${memory.category}] - Prefix: ${memory.key}", "INFO")
    }

    suspend fun deleteMemory(memory: MemoryEntity) = memoryDao.deleteMemory(memory)
    suspend fun deleteMemoryById(id: Int) = memoryDao.deleteMemoryById(id)

    // Workflows
    val allWorkflows: Flow<List<WorkflowEntity>> = workflowDao.getAllWorkflowsFlow()
    suspend fun insertWorkflow(workflow: WorkflowEntity) {
        workflowDao.insertWorkflow(workflow)
        logAction("Workflow Created", "New autonomous workflow: '${workflow.title}'", "SUCCESS")
    }
    suspend fun updateWorkflow(workflow: WorkflowEntity) = workflowDao.updateWorkflow(workflow)
    suspend fun deleteWorkflow(workflow: WorkflowEntity) {
        workflowDao.deleteWorkflow(workflow)
        logAction("Workflow Deleted", "Removed workflow: '${workflow.title}'", "WARNING")
    }

    // Audit Logs
    val recentLogs: Flow<List<AuditLogEntity>> = auditLogDao.getRecentLogsFlow()
    suspend fun insertLog(log: AuditLogEntity) = auditLogDao.insertLog(log)
    suspend fun clearLogs() = auditLogDao.clearLogs()

    suspend fun logAction(action: String, details: String, severity: String = "INFO") {
        auditLogDao.insertLog(
            AuditLogEntity(
                action = action,
                details = details,
                severity = severity
            )
        )
    }

    // Pinterest Pins
    val allPins: Flow<List<PinterestPinEntity>> = pinterestPinDao.getAllPinsFlow()
    suspend fun insertPin(pin: PinterestPinEntity) {
        pinterestPinDao.insertPin(pin)
        logAction("Pinterest Pin Created", "[${pin.status}] '${pin.title}' scheduled for ${pin.scheduledTime}", "SUCCESS")
    }
    suspend fun updatePin(pin: PinterestPinEntity) = pinterestPinDao.updatePin(pin)
    suspend fun deletePin(pin: PinterestPinEntity) = pinterestPinDao.deletePin(pin)
}
