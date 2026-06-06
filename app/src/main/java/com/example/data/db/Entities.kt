package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val key: String,
    val value: String,
    val category: String, // "Preference", "Business", "Project", "Style", "Short-term"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workflows")
data class WorkflowEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val prompt: String,
    val scheduledTime: String, // e.g. "09:00 AM" or "Every Hour"
    val lastRunStatus: String = "Idle", // "Success", "Failed", "Idle", "Running"
    val isActive: Boolean = true,
    val category: String = "Social Media"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workflowId: Int? = null,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: String = "INFO" // "INFO", "SUCCESS", "WARNING", "AUTH"
)

@Entity(tableName = "pinterest_pins")
data class PinterestPinEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val tags: String,
    val imageBase64: String, // base64 encoded image or empty string
    val scheduledTime: String,
    val status: String = "Scheduled", // "Draft", "Scheduled", "Published"
    val clicks: Int = 0,
    val impressions: Int = 0
)
