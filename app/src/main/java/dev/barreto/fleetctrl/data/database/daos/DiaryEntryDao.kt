package dev.barreto.fleetctrl.data.database.daos

import androidx.room.*
import dev.barreto.fleetctrl.data.database.entities.DiaryEntry
import dev.barreto.fleetctrl.data.database.entities.DiaryEntryType
import dev.barreto.fleetctrl.data.database.entities.Priority
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * DAO para operações com entradas do diário de bordo
 */
@Dao
interface DiaryEntryDao {
    
    // ===== OPERAÇÕES BÁSICAS =====
    
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun getDiaryEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE id = :id")
    suspend fun getDiaryEntryById(id: Long): DiaryEntry?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(diaryEntry: DiaryEntry): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntries(diaryEntries: List<DiaryEntry>)
    
    @Update
    suspend fun updateDiaryEntry(diaryEntry: DiaryEntry)
    
    @Delete
    suspend fun deleteDiaryEntry(diaryEntry: DiaryEntry)
    
    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteDiaryEntryById(id: Long)
    
    // ===== OPERAÇÕES POR TIPO =====
    
    @Query("SELECT * FROM diary_entries WHERE type = :type ORDER BY date DESC")
    fun getDiaryEntriesByType(type: DiaryEntryType): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND type = :type ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndType(vehicleId: Long, type: DiaryEntryType): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES POR PRIORIDADE =====
    
    @Query("SELECT * FROM diary_entries WHERE priority = :priority ORDER BY date DESC")
    fun getDiaryEntriesByPriority(priority: Priority): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND priority = :priority ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndPriority(vehicleId: Long, priority: Priority): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES POR PERÍODO =====
    
    @Query("SELECT * FROM diary_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getDiaryEntriesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndDateRange(vehicleId: Long, startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE date >= :date ORDER BY date DESC")
    fun getDiaryEntriesFromDate(date: LocalDateTime): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE date <= :date ORDER BY date DESC")
    fun getDiaryEntriesUntilDate(date: LocalDateTime): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE DATE(date) = DATE(:date) ORDER BY date DESC")
    fun getDiaryEntriesByDate(date: LocalDateTime): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND DATE(date) = DATE(:date) ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndDate(vehicleId: Long, date: LocalDateTime): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES DE STATUS =====
    
    @Query("SELECT * FROM diary_entries WHERE isCompleted = 1 ORDER BY date DESC")
    fun getCompletedDiaryEntries(): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE isCompleted = 0 ORDER BY priority DESC, date ASC")
    fun getPendingDiaryEntries(): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND isCompleted = 0 ORDER BY priority DESC, date ASC")
    fun getPendingDiaryEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES DE LOCALIZAÇÃO =====
    
    @Query("SELECT * FROM diary_entries WHERE startLocation = :location OR endLocation = :location ORDER BY date DESC")
    fun getDiaryEntriesByLocation(location: String): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND (startLocation = :location OR endLocation = :location) ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndLocation(vehicleId: Long, location: String): Flow<List<DiaryEntry>>
    
    @Query("SELECT DISTINCT startLocation FROM diary_entries WHERE startLocation IS NOT NULL ORDER BY startLocation ASC")
    fun getAllStartLocations(): Flow<List<String>>
    
    @Query("SELECT DISTINCT endLocation FROM diary_entries WHERE endLocation IS NOT NULL ORDER BY endLocation ASC")
    fun getAllEndLocations(): Flow<List<String>>
    
    // ===== OPERAÇÕES DE DISTÂNCIA =====
    
    @Query("SELECT * FROM diary_entries WHERE distance IS NOT NULL AND distance >= :minDistance ORDER BY distance DESC")
    fun getDiaryEntriesByMinDistance(minDistance: Double): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND distance IS NOT NULL AND distance >= :minDistance ORDER BY distance DESC")
    fun getDiaryEntriesByVehicleAndMinDistance(vehicleId: Long, minDistance: Double): Flow<List<DiaryEntry>>
    
    @Query("SELECT SUM(distance) FROM diary_entries WHERE vehicleId = :vehicleId AND distance IS NOT NULL")
    suspend fun getTotalDistanceByVehicle(vehicleId: Long): Double?
    
    @Query("SELECT AVG(distance) FROM diary_entries WHERE vehicleId = :vehicleId AND distance IS NOT NULL")
    suspend fun getAverageDistanceByVehicle(vehicleId: Long): Double?
    
    // ===== OPERAÇÕES DE DURAÇÃO =====
    
    @Query("SELECT * FROM diary_entries WHERE duration IS NOT NULL AND duration >= :minDuration ORDER BY duration DESC")
    fun getDiaryEntriesByMinDuration(minDuration: Long): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND duration IS NOT NULL AND duration >= :minDuration ORDER BY duration DESC")
    fun getDiaryEntriesByVehicleAndMinDuration(vehicleId: Long, minDuration: Long): Flow<List<DiaryEntry>>
    
    @Query("SELECT SUM(duration) FROM diary_entries WHERE vehicleId = :vehicleId AND duration IS NOT NULL")
    suspend fun getTotalDurationByVehicle(vehicleId: Long): Long?
    
    @Query("SELECT AVG(duration) FROM diary_entries WHERE vehicleId = :vehicleId AND duration IS NOT NULL")
    suspend fun getAverageDurationByVehicle(vehicleId: Long): Double?
    
    // ===== OPERAÇÕES DE TAGS =====
    
    @Query("SELECT * FROM diary_entries WHERE tags LIKE :tag ORDER BY date DESC")
    fun getDiaryEntriesByTag(tag: String): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND tags LIKE :tag ORDER BY date DESC")
    fun getDiaryEntriesByVehicleAndTag(vehicleId: Long, tag: String): Flow<List<DiaryEntry>>
    
    @Query("SELECT DISTINCT tags FROM diary_entries WHERE tags IS NOT NULL")
    suspend fun getAllTags(): List<String>
    
    // ===== OPERAÇÕES DE ESTATÍSTICAS =====
    
    @Query("SELECT COUNT(*) FROM diary_entries WHERE vehicleId = :vehicleId")
    suspend fun getDiaryEntryCountByVehicle(vehicleId: Long): Int
    
    @Query("SELECT COUNT(*) FROM diary_entries WHERE vehicleId = :vehicleId AND type = :type")
    suspend fun getDiaryEntryCountByVehicleAndType(vehicleId: Long, type: DiaryEntryType): Int
    
    @Query("SELECT COUNT(*) FROM diary_entries WHERE vehicleId = :vehicleId AND isCompleted = 0")
    suspend fun getPendingDiaryEntryCountByVehicle(vehicleId: Long): Int
    
    // ===== OPERAÇÕES DE BUSCA =====
    
    @Query("""
        SELECT * FROM diary_entries 
        WHERE (title LIKE :query OR description LIKE :query OR notes LIKE :query OR tags LIKE :query)
        ORDER BY 
            CASE 
                WHEN title LIKE :query THEN 1
                WHEN description LIKE :query THEN 2
                WHEN tags LIKE :query THEN 3
                ELSE 4
            END,
            date DESC
    """)
    fun searchDiaryEntries(query: String): Flow<List<DiaryEntry>>
    
    @Query("""
        SELECT * FROM diary_entries 
        WHERE vehicleId = :vehicleId 
        AND (title LIKE :query OR description LIKE :query OR notes LIKE :query OR tags LIKE :query)
        ORDER BY 
            CASE 
                WHEN title LIKE :query THEN 1
                WHEN description LIKE :query THEN 2
                WHEN tags LIKE :query THEN 3
                ELSE 4
            END,
            date DESC
    """)
    fun searchDiaryEntriesByVehicle(vehicleId: Long, query: String): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES RECENTES =====
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId ORDER BY date DESC LIMIT :limit")
    fun getRecentDiaryEntries(vehicleId: Long, limit: Int): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries ORDER BY date DESC LIMIT :limit")
    fun getRecentDiaryEntries(limit: Int): Flow<List<DiaryEntry>>
    
    // ===== OPERAÇÕES DE VIAGEM =====
    
    @Query("SELECT * FROM diary_entries WHERE type = 'TRIP' AND startTime IS NOT NULL AND endTime IS NOT NULL ORDER BY startTime DESC")
    fun getTripEntries(): Flow<List<DiaryEntry>>
    
    @Query("SELECT * FROM diary_entries WHERE vehicleId = :vehicleId AND type = 'TRIP' AND startTime IS NOT NULL AND endTime IS NOT NULL ORDER BY startTime DESC")
    fun getTripEntriesByVehicle(vehicleId: Long): Flow<List<DiaryEntry>>
}