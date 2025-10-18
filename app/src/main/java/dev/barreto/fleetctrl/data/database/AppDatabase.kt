package dev.barreto.fleetctrl.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dev.barreto.fleetctrl.data.database.converters.Converters
import dev.barreto.fleetctrl.data.database.daos.*
import dev.barreto.fleetctrl.data.database.entities.*
import dev.barreto.fleetctrl.data.database.migrations.DatabaseMigrations

/**
 * Database principal da aplicação FleetCtrl
 * 
 * Versão atual: 1
 * 
 * Entidades:
 * - Vehicle (Frota de veículos)
 * - FuelRecord (Registros de abastecimento)
 * - MaintenanceRecord (Registros de manutenção)
 * - DiaryEntry (Entradas do diário de bordo)
 */
@Database(
    entities = [
        Vehicle::class,
        FuelRecord::class,
        MaintenanceRecord::class,
        ActivityRecord::class,
        Organization::class,
        UserOrganization::class,
        OrganizationInvite::class,
        Notification::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    // ===== DAOs =====
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelRecordDao(): FuelRecordDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun notificationDao(): NotificationDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "fleetctrl_database"
        
        /**
         * Obtém a instância do banco de dados
         * Implementa o padrão Singleton para garantir uma única instância
         */
        @RequiresApi(Build.VERSION_CODES.P)
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                // Usar migrações robustas para desenvolvimento e produção
                .addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)
                .enableMultiInstanceInvalidation() // Otimização para múltiplas instâncias
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE) // Desabilitar WAL mode
                // Removido fallbackToDestructiveMigration para preservar dados
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        println("DEBUG: Banco de dados criado com sucesso")
                    }
                    
                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)
                        println("DEBUG: Banco de dados aberto")
                        
                        // Verificar e corrigir dados críticos após abertura
                        try {
                            val cursor = db.query("SELECT COUNT(*) FROM organizations")
                            if (cursor.moveToFirst()) {
                                val orgCount = cursor.getInt(0)
                                println("DEBUG: Organizações encontradas no banco: $orgCount")
                            }
                            cursor.close()
                            
                            // Verificar se a coluna requiresApproval existe
                            val pragmaCursor = db.query("PRAGMA table_info(organizations)")
                            var hasRequiresApproval = false
                            while (pragmaCursor.moveToNext()) {
                                val columnName = pragmaCursor.getString(pragmaCursor.getColumnIndexOrThrow("name"))
                                if (columnName == "requiresApproval") {
                                    hasRequiresApproval = true
                                    break
                                }
                            }
                            pragmaCursor.close()
                            
                            if (!hasRequiresApproval) {
                                println("DEBUG: Adicionando campo requiresApproval em tempo de execução")
                                db.execSQL("ALTER TABLE organizations ADD COLUMN requiresApproval INTEGER DEFAULT 1")
                                db.execSQL("UPDATE organizations SET requiresApproval = 1")
                            }
                            
                        } catch (e: Exception) {
                            println("DEBUG: Erro ao verificar dados críticos: ${e.message}")
                        }
                    }
                })
                .setQueryCallback(object : RoomDatabase.QueryCallback {
                    override fun onQuery(sqlQuery: String, bindArgs: List<Any?>) {
                        // Log de queries para debug
                        println("SQL Query: $sqlQuery")
                    }
                }, context.mainExecutor)
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Obtém a instância do banco de dados para testes
         * Cria uma instância em memória para testes unitários
         */
        fun getTestDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries() // Permitido apenas para testes
            .build()
        }
        
    }
}
