package dev.barreto.fleetctrl.data.repositories

import dev.barreto.fleetctrl.data.database.daos.DiaryEntryDao
import dev.barreto.fleetctrl.data.database.entities.DiaryEntry
import dev.barreto.fleetctrl.data.database.entities.DiaryEntryType
import dev.barreto.fleetctrl.data.database.entities.Priority
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository para operações com entradas do diário de bordo
 */
@Singleton
class DiaryEntryRepository @Inject constructor(
    private val diaryEntryDao: DiaryEntryDao
) {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>> = diaryEntryDao.getAllDiaryEntries()
    
    fun getDiaryEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicle(vehicleId)
    
    suspend fun getDiaryEntryById(id: Long): DiaryEntry? = diaryEntryDao.getDiaryEntryById(id)
    
    suspend fun insertDiaryEntry(diaryEntry: DiaryEntry): Long = diaryEntryDao.insertDiaryEntry(diaryEntry)
    
    suspend fun insertDiaryEntries(diaryEntries: List<DiaryEntry>) = diaryEntryDao.insertDiaryEntries(diaryEntries)
    
    suspend fun updateDiaryEntry(diaryEntry: DiaryEntry) = diaryEntryDao.updateDiaryEntry(diaryEntry)
    
    suspend fun deleteDiaryEntry(diaryEntry: DiaryEntry) = diaryEntryDao.deleteDiaryEntry(diaryEntry)
    
    suspend fun deleteDiaryEntryById(id: Long) = diaryEntryDao.deleteDiaryEntryById(id)
    
    // ===== OPERAÇÕES POR TIPO =====
    
    fun getDiaryEntriesByType(type: DiaryEntryType): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByType(type)
    
    fun getDiaryEntriesByVehicleAndType(vehicleId: Long, type: DiaryEntryType): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndType(vehicleId, type)
    
    // ===== OPERAÇÕES POR PRIORIDADE =====
    
    fun getDiaryEntriesByPriority(priority: Priority): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByPriority(priority)
    
    fun getDiaryEntriesByVehicleAndPriority(vehicleId: Long, priority: Priority): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndPriority(vehicleId, priority)
    
    // ===== OPERAÇÕES POR PERÍODO =====
    
    fun getDiaryEntriesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByDateRange(startDate, endDate)
    
    fun getDiaryEntriesByVehicleAndDateRange(vehicleId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndDateRange(vehicleId, startDate, endDate)
    
    fun getDiaryEntriesFromDate(date: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesFromDate(date)
    
    fun getDiaryEntriesUntilDate(date: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesUntilDate(date)
    
    fun getDiaryEntriesByDate(date: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByDate(date)
    
    fun getDiaryEntriesByVehicleAndDate(vehicleId: Long, date: LocalDateTime): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndDate(vehicleId, date)
    
    // ===== OPERAÇÕES DE STATUS =====
    
    fun getCompletedDiaryEntries(): Flow<List<DiaryEntry>> = diaryEntryDao.getCompletedDiaryEntries()
    
    fun getPendingDiaryEntries(): Flow<List<DiaryEntry>> = diaryEntryDao.getPendingDiaryEntries()
    
    fun getPendingDiaryEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getPendingDiaryEntriesByVehicle(vehicleId)
    
    // ===== OPERAÇÕES DE LOCALIZAÇÃO =====
    
    fun getDiaryEntriesByLocation(location: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByLocation(location)
    
    fun getDiaryEntriesByVehicleAndLocation(vehicleId: Long, location: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndLocation(vehicleId, location)
    
    fun getAllStartLocations(): Flow<List<String>> = diaryEntryDao.getAllStartLocations()
    
    fun getAllEndLocations(): Flow<List<String>> = diaryEntryDao.getAllEndLocations()
    
    // ===== OPERAÇÕES DE DISTÂNCIA =====
    
    fun getDiaryEntriesByMinDistance(minDistance: Double): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByMinDistance(minDistance)
    
    fun getDiaryEntriesByVehicleAndMinDistance(vehicleId: Long, minDistance: Double): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndMinDistance(vehicleId, minDistance)
    
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Double? = 
        diaryEntryDao.getTotalDistanceByVehicle(vehicleId)
    
    suspend fun getAverageDistanceByVehicle(vehicleId: Long): Double? = 
        diaryEntryDao.getAverageDistanceByVehicle(vehicleId)
    
    // ===== OPERAÇÕES DE DURAÇÃO =====
    
    fun getDiaryEntriesByMinDuration(minDuration: Long): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByMinDuration(minDuration)
    
    fun getDiaryEntriesByVehicleAndMinDuration(vehicleId: Long, minDuration: Long): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndMinDuration(vehicleId, minDuration)
    
    suspend fun getTotalDurationByVehicle(vehicleId: Long): Long? = 
        diaryEntryDao.getTotalDurationByVehicle(vehicleId)
    
    suspend fun getAverageDurationByVehicle(vehicleId: Long): Double? = 
        diaryEntryDao.getAverageDurationByVehicle(vehicleId)
    
    // ===== OPERAÇÕES DE TAGS =====
    
    fun getDiaryEntriesByTag(tag: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByTag("%$tag%")
    
    fun getDiaryEntriesByVehicleAndTag(vehicleId: Long, tag: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getDiaryEntriesByVehicleAndTag(vehicleId, "%$tag%")
    
    suspend fun getAllTags(): List<String> = diaryEntryDao.getAllTags()
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    suspend fun getDiaryEntryCountByVehicle(vehicleId: Long): Int = 
        diaryEntryDao.getDiaryEntryCountByVehicle(vehicleId)
    
    suspend fun getDiaryEntryCountByVehicleAndType(vehicleId: Long, type: DiaryEntryType): Int = 
        diaryEntryDao.getDiaryEntryCountByVehicleAndType(vehicleId, type)
    
    suspend fun getPendingDiaryEntryCountByVehicle(vehicleId: Long): Int = 
        diaryEntryDao.getPendingDiaryEntryCountByVehicle(vehicleId)
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    fun searchDiaryEntries(query: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.searchDiaryEntries("%$query%")
    
    fun searchDiaryEntriesByVehicle(vehicleId: Long, query: String): Flow<List<DiaryEntry>> = 
        diaryEntryDao.searchDiaryEntriesByVehicle(vehicleId, "%$query%")
    
    // ===== OPERAÇÕES RECENTES =====
    
    fun getRecentDiaryEntries(vehicleId: Long, limit: Int = 10): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getRecentDiaryEntries(vehicleId, limit)
    
    fun getRecentDiaryEntries(limit: Int = 10): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getRecentDiaryEntries(limit)
    
    // ===== OPERAÇÕES DE VIAGEM =====
    
    fun getTripEntries(): Flow<List<DiaryEntry>> = diaryEntryDao.getTripEntries()
    
    fun getTripEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>> = 
        diaryEntryDao.getTripEntriesByVehicle(vehicleId)
    
    // ===== OPERAÇÕES DE VALIDAÇÃO =====
    
    suspend fun validateDiaryEntry(diaryEntry: DiaryEntry): Boolean {
        // Validar veículo
        if (diaryEntry.vehicleId <= 0) return false
        
        // Validar data
        if (diaryEntry.date.isAfter(LocalDateTime.now())) return false
        
        // Validar título
        if (diaryEntry.title.isBlank()) return false
        
        // Validar descrição
        if (diaryEntry.description.isBlank()) return false
        
        // Validar horários
        if (diaryEntry.startTime != null && diaryEntry.endTime != null) {
            if (diaryEntry.startTime.isAfter(diaryEntry.endTime)) return false
        }
        
        // Validar quilometragem
        if (diaryEntry.startMileage != null && diaryEntry.endMileage != null) {
            if (diaryEntry.startMileage > diaryEntry.endMileage) return false
        }
        
        // Validar distância
        if (diaryEntry.distance != null && diaryEntry.distance < 0) return false
        
        // Validar duração
        if (diaryEntry.duration != null && diaryEntry.duration < 0) return false
        
        return true
    }
    
    // ===== OPERAÇÕES DE CÁLCULO =====
    
    suspend fun calculateAverageSpeed(vehicleId: Long): Double? {
        val totalDistance = getTotalDistanceByVehicle(vehicleId)
        val totalDuration = getTotalDurationByVehicle(vehicleId)
        
        return if (totalDistance != null && totalDuration != null && totalDuration > 0) {
            totalDistance / (totalDuration / 60.0) // Converter minutos para horas
        } else null
    }
    
    suspend fun calculateEfficiency(vehicleId: Long): Double? {
        val totalDistance = getTotalDistanceByVehicle(vehicleId)
        val totalDuration = getTotalDurationByVehicle(vehicleId)
        
        return if (totalDistance != null && totalDuration != null && totalDuration > 0) {
            totalDistance / totalDuration // km por minuto
        } else null
    }
}