package dev.barreto.fleetctrl.data.audit

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sistema de auditoria e logs de segurança
 */
@Singleton
class SecurityAuditLogger @Inject constructor(
    private val context: Context
) {
    private val auditDir = context.filesDir.resolve("audit_logs")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val maxLogFiles = 30 // Manter apenas os últimos 30 dias de logs
    private val maxLogSize = 10 * 1024 * 1024 // 10MB por arquivo

    init {
        createAuditDirectory()
    }

    /**
     * Cria o diretório de auditoria se não existir
     */
    private fun createAuditDirectory() {
        if (!auditDir.exists()) {
            auditDir.mkdirs()
        }
    }

    /**
     * Registra uma ação de segurança
     */
    suspend fun logSecurityEvent(
        action: SecurityAction,
        userId: String? = null,
        details: Map<String, String> = emptyMap(),
        severity: SecuritySeverity = SecuritySeverity.INFO
    ) = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val logEntry = SecurityLogEntry(
                timestamp = timestamp,
                action = action,
                userId = userId,
                details = details,
                severity = severity,
                deviceInfo = getDeviceInfo()
            )

            writeLogEntry(logEntry)
            
            // Log também no console para debug
            Log.i("SecurityAudit", "${logEntry.action.name}: ${logEntry.details}")
            
        } catch (e: Exception) {
            Log.e("SecurityAudit", "Erro ao registrar evento de segurança", e)
        }
    }

    /**
     * Registra tentativa de acesso não autorizado
     */
    suspend fun logUnauthorizedAccess(
        resource: String,
        userId: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        logSecurityEvent(
            action = SecurityAction.UNAUTHORIZED_ACCESS,
            userId = userId,
            details = details + mapOf("resource" to resource),
            severity = SecuritySeverity.HIGH
        )
    }

    /**
     * Registra modificação de dados sensíveis
     */
    suspend fun logDataModification(
        entityType: String,
        entityId: String,
        action: String,
        userId: String? = null,
        details: Map<String, String> = emptyMap()
    ) {
        logSecurityEvent(
            action = SecurityAction.DATA_MODIFICATION,
            userId = userId,
            details = details + mapOf(
                "entity_type" to entityType,
                "entity_id" to entityId,
                "modification_action" to action
            ),
            severity = SecuritySeverity.MEDIUM
        )
    }

    /**
     * Registra operação de backup/restore
     */
    suspend fun logBackupOperation(
        operation: String,
        success: Boolean,
        details: Map<String, String> = emptyMap()
    ) {
        logSecurityEvent(
            action = SecurityAction.BACKUP_OPERATION,
            details = details + mapOf(
                "operation" to operation,
                "success" to success.toString()
            ),
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.HIGH
        )
    }

    /**
     * Registra erro de validação de dados
     */
    suspend fun logDataValidationError(
        entityType: String,
        entityId: String,
        errors: List<String>,
        userId: String? = null
    ) {
        logSecurityEvent(
            action = SecurityAction.DATA_VALIDATION_ERROR,
            userId = userId,
            details = mapOf(
                "entity_type" to entityType,
                "entity_id" to entityId,
                "errors" to errors.joinToString("; ")
            ),
            severity = SecuritySeverity.MEDIUM
        )
    }

    /**
     * Registra tentativa de migração de dados
     */
    suspend fun logDataMigration(
        fromVersion: Int,
        toVersion: Int,
        success: Boolean,
        details: Map<String, String> = emptyMap()
    ) {
        logSecurityEvent(
            action = SecurityAction.DATA_MIGRATION,
            details = details + mapOf(
                "from_version" to fromVersion.toString(),
                "to_version" to toVersion.toString(),
                "success" to success.toString()
            ),
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.HIGH
        )
    }

    /**
     * Registra operação de criptografia
     */
    suspend fun logEncryptionOperation(
        operation: String,
        success: Boolean,
        details: Map<String, String> = emptyMap()
    ) {
        logSecurityEvent(
            action = SecurityAction.ENCRYPTION_OPERATION,
            details = details + mapOf(
                "operation" to operation,
                "success" to success.toString()
            ),
            severity = if (success) SecuritySeverity.INFO else SecuritySeverity.HIGH
        )
    }

    /**
     * Escreve uma entrada de log no arquivo
     */
    private suspend fun writeLogEntry(logEntry: SecurityLogEntry) = withContext(Dispatchers.IO) {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val logFile = auditDir.resolve("security_audit_$today.log")
            
            val logLine = formatLogEntry(logEntry)
            
            logFile.appendText(logLine + "\n")
            
            // Verifica se o arquivo está muito grande
            if (logFile.length() > maxLogSize) {
                rotateLogFile(logFile)
            }
            
        } catch (e: Exception) {
            Log.e("SecurityAudit", "Erro ao escrever entrada de log", e)
        }
    }

    /**
     * Formata uma entrada de log
     */
    private fun formatLogEntry(logEntry: SecurityLogEntry): String {
        val timestamp = dateFormat.format(Date(logEntry.timestamp))
        val userId = logEntry.userId ?: "unknown"
        val details = logEntry.details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        
        return "[$timestamp] [${logEntry.severity.name}] [${logEntry.action.name}] [User:$userId] $details"
    }

    /**
     * Rotaciona arquivo de log quando fica muito grande
     */
    private fun rotateLogFile(logFile: java.io.File) {
        try {
            val timestamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            val rotatedFile = java.io.File(logFile.parent, "${logFile.nameWithoutExtension}_$timestamp.log")
            logFile.renameTo(rotatedFile)
        } catch (e: Exception) {
            Log.e("SecurityAudit", "Erro ao rotacionar arquivo de log", e)
        }
    }

    /**
     * Obtém informações do dispositivo
     */
    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "android_version" to android.os.Build.VERSION.RELEASE,
            "device_model" to android.os.Build.MODEL,
            "app_version" to getAppVersion()
        )
    }

    /**
     * Obtém versão do app
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Limpa logs antigos
     */
    suspend fun cleanupOldLogs() = withContext(Dispatchers.IO) {
        try {
            val logFiles = auditDir.listFiles()
                ?.filter { it.name.startsWith("security_audit_") && it.name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?: return@withContext

            if (logFiles.size > maxLogFiles) {
                val filesToDelete = logFiles.drop(maxLogFiles)
                filesToDelete.forEach { file ->
                    try {
                        file.delete()
                        Log.d("SecurityAudit", "Log antigo removido: ${file.name}")
                    } catch (e: Exception) {
                        Log.e("SecurityAudit", "Erro ao remover log antigo", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SecurityAudit", "Erro ao limpar logs antigos", e)
        }
    }

    /**
     * Obtém logs de segurança para análise
     */
    suspend fun getSecurityLogs(
        startDate: Date? = null,
        endDate: Date? = null,
        severity: SecuritySeverity? = null,
        action: SecurityAction? = null
    ): List<SecurityLogEntry> = withContext(Dispatchers.IO) {
        try {
            val logFiles = auditDir.listFiles()
                ?.filter { it.name.startsWith("security_audit_") && it.name.endsWith(".log") }
                ?: return@withContext emptyList()

            val allLogs = mutableListOf<SecurityLogEntry>()
            
            for (logFile in logFiles) {
                try {
                    val logs = parseLogFile(logFile)
                    allLogs.addAll(logs)
                } catch (e: Exception) {
                    Log.e("SecurityAudit", "Erro ao parsear arquivo de log: ${logFile.name}", e)
                }
            }

            // Aplica filtros
            allLogs.filter { logEntry ->
                val dateFilter = when {
                    startDate != null && Date(logEntry.timestamp) < startDate -> false
                    endDate != null && Date(logEntry.timestamp) > endDate -> false
                    else -> true
                }
                
                val severityFilter = severity?.let { logEntry.severity == it } ?: true
                val actionFilter = action?.let { logEntry.action == it } ?: true
                
                dateFilter && severityFilter && actionFilter
            }.sortedByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e("SecurityAudit", "Erro ao obter logs de segurança", e)
            emptyList()
        }
    }

    /**
     * Parseia um arquivo de log
     */
    private fun parseLogFile(logFile: java.io.File): List<SecurityLogEntry> {
        // Implementação simplificada - em produção usar uma biblioteca de parsing
        return emptyList()
    }
}

/**
 * Ações de segurança que podem ser auditadas
 */
enum class SecurityAction {
    UNAUTHORIZED_ACCESS,
    DATA_MODIFICATION,
    BACKUP_OPERATION,
    DATA_VALIDATION_ERROR,
    DATA_MIGRATION,
    ENCRYPTION_OPERATION,
    LOGIN_ATTEMPT,
    LOGOUT,
    PERMISSION_CHANGE,
    SYSTEM_ERROR
}

/**
 * Níveis de severidade dos eventos de segurança
 */
enum class SecuritySeverity {
    LOW,
    INFO,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Entrada de log de segurança
 */
data class SecurityLogEntry(
    val timestamp: Long,
    val action: SecurityAction,
    val userId: String?,
    val details: Map<String, String>,
    val severity: SecuritySeverity,
    val deviceInfo: Map<String, String>
)