package dev.barreto.fleetctrl.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações do banco de dados
 * 
 * Cada migração deve ser testada individualmente e deve ser reversível quando possível
 */
object DatabaseMigrations {
    
    /**
     * Migração da versão 1 para 2
     * Adiciona as tabelas activity_records e maintenance_records
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Criar tabela activity_records
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS activity_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER NOT NULL,
                    plate TEXT NOT NULL,
                    driver TEXT NOT NULL,
                    date TEXT NOT NULL,
                    startMileage INTEGER NOT NULL,
                    endMileage INTEGER NOT NULL,
                    observation TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Criar tabela maintenance_records
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS maintenance_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    laborCost TEXT NOT NULL,
                    partsCost TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    workshop TEXT,
                    mechanic TEXT,
                    warrantyUntil TEXT,
                    isCompleted INTEGER NOT NULL,
                    nextMaintenanceMileage INTEGER,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Criar índices para activity_records
            database.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_vehicleId ON activity_records (vehicleId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_plate ON activity_records (plate)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_driver ON activity_records (driver)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_date ON activity_records (date)")
            
            // Criar índices para maintenance_records
            database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_records_vehicleId ON maintenance_records (vehicleId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_records_date ON maintenance_records (date)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_maintenance_records_type ON maintenance_records (type)")
        }
    }
    
    /**
     * Migração da versão 2 para 3
     * Corrige a tabela activity_records que pode estar corrompida
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Dropar a tabela activity_records se existir (pode estar corrompida)
            database.execSQL("DROP TABLE IF EXISTS activity_records")
            
            // Recriar a tabela activity_records corretamente
            database.execSQL("""
                CREATE TABLE activity_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER NOT NULL,
                    plate TEXT NOT NULL,
                    driver TEXT NOT NULL,
                    date TEXT NOT NULL,
                    startMileage INTEGER NOT NULL,
                    endMileage INTEGER NOT NULL,
                    observation TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Recriar índices
            database.execSQL("CREATE INDEX index_activity_records_vehicleId ON activity_records (vehicleId)")
            database.execSQL("CREATE INDEX index_activity_records_plate ON activity_records (plate)")
            database.execSQL("CREATE INDEX index_activity_records_driver ON activity_records (driver)")
            database.execSQL("CREATE INDEX index_activity_records_date ON activity_records (date)")
        }
    }
    
    /**
     * Migração da versão 3 para 4
     * Adiciona novos campos à tabela maintenance_records para próxima revisão
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Adicionar novos campos à tabela maintenance_records
            database.execSQL("ALTER TABLE maintenance_records ADD COLUMN nextRevisionDate TEXT")
            database.execSQL("ALTER TABLE maintenance_records ADD COLUMN minRevisionDate TEXT")
            database.execSQL("ALTER TABLE maintenance_records ADD COLUMN maxRevisionDate TEXT")
            database.execSQL("ALTER TABLE maintenance_records ADD COLUMN distanceToDealer INTEGER")
        }
    }
    
    /**
     * Migração da versão 4 para 5
     * Recria a tabela maintenance_records com todos os campos necessários
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Dropar a tabela maintenance_records se existir
            database.execSQL("DROP TABLE IF EXISTS maintenance_records")
            
            // Recriar a tabela maintenance_records com todos os campos
            database.execSQL("""
                CREATE TABLE maintenance_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    laborCost TEXT NOT NULL,
                    partsCost TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    workshop TEXT,
                    mechanic TEXT,
                    warrantyUntil TEXT,
                    isCompleted INTEGER NOT NULL,
                    nextMaintenanceMileage INTEGER,
                    nextRevisionDate TEXT,
                    minRevisionDate TEXT,
                    maxRevisionDate TEXT,
                    distanceToDealer INTEGER,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Recriar índices
            database.execSQL("CREATE INDEX index_maintenance_records_vehicleId ON maintenance_records (vehicleId)")
            database.execSQL("CREATE INDEX index_maintenance_records_date ON maintenance_records (date)")
            database.execSQL("CREATE INDEX index_maintenance_records_type ON maintenance_records (type)")
        }
    }
    
    /**
     * Migração da versão 5 para 6
     * Adiciona as tabelas de organizações (organizations e user_organizations)
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Criar tabela organizations
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS organizations (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    ownerId TEXT NOT NULL,
                    ownerEmail TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    maxMembers INTEGER,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    isSynced INTEGER NOT NULL,
                    lastSyncAt TEXT
                )
            """)
            
            // Criar tabela user_organizations
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS user_organizations (
                    userId TEXT NOT NULL,
                    organizationId TEXT NOT NULL,
                    role TEXT NOT NULL,
                    joinedAt TEXT NOT NULL,
                    isActive INTEGER NOT NULL,
                    isSynced INTEGER NOT NULL,
                    lastSyncAt TEXT,
                    PRIMARY KEY (userId, organizationId),
                    FOREIGN KEY (organizationId) REFERENCES organizations(id) ON DELETE CASCADE
                )
            """)
            
            // Criar índices para organizations
            database.execSQL("CREATE INDEX IF NOT EXISTS index_organizations_ownerId ON organizations (ownerId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_organizations_isActive ON organizations (isActive)")
            
            // Criar índices para user_organizations
            database.execSQL("CREATE INDEX IF NOT EXISTS index_user_organizations_userId ON user_organizations (userId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_user_organizations_organizationId ON user_organizations (organizationId)")
        }
    }
    
    /**
     * Migração da versão 6 para 7
     * Adiciona o campo 'code' à tabela organizations
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Adicionar campo 'code' à tabela organizations
            database.execSQL("ALTER TABLE organizations ADD COLUMN code TEXT NOT NULL DEFAULT ''")
        }
    }
    
    /**
     * Migração da versão 7 para 8
     * Adiciona campo organizationId na tabela vehicles
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE vehicles ADD COLUMN organizationId TEXT")
        }
    }
    
    /**
     * Migração da versão 8 para 9
     * Adiciona campo organizationId em todas as tabelas de registros
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE fuel_records ADD COLUMN organizationId TEXT")
            database.execSQL("ALTER TABLE activity_records ADD COLUMN organizationId TEXT")
            database.execSQL("ALTER TABLE maintenance_records ADD COLUMN organizationId TEXT")
        }
    }
    
    /**
     * Migração da versão 9 para 10
     * Remove a tabela diary_entries que não é mais usada
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Remover a tabela diary_entries
            database.execSQL("DROP TABLE IF EXISTS diary_entries")
        }
    }
    
    /**
     * Migração da versão 10 para 11
     * Torna vehicleId opcional para evitar conflitos de chave estrangeira
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Desabilitar chaves estrangeiras temporariamente
            database.execSQL("PRAGMA foreign_keys = OFF")
            
            // Recriar tabelas com vehicleId opcional
            database.execSQL("""
                CREATE TABLE fuel_records_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    date TEXT NOT NULL,
                    fuelType TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    pricePerLiter TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    gasStation TEXT,
                    location TEXT,
                    receiptNumber TEXT,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            database.execSQL("""
                CREATE TABLE activity_records_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    plate TEXT NOT NULL,
                    driver TEXT NOT NULL,
                    date TEXT NOT NULL,
                    startMileage INTEGER NOT NULL,
                    endMileage INTEGER NOT NULL,
                    observation TEXT,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            database.execSQL("""
                CREATE TABLE maintenance_records_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    date TEXT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    laborCost TEXT NOT NULL,
                    partsCost TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    workshop TEXT,
                    mechanic TEXT,
                    warrantyUntil TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 1,
                    nextMaintenanceMileage INTEGER,
                    nextRevisionDate TEXT,
                    minRevisionDate TEXT,
                    maxRevisionDate TEXT,
                    distanceToDealer INTEGER,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Copiar dados existentes
            database.execSQL("INSERT INTO fuel_records_new SELECT * FROM fuel_records")
            database.execSQL("INSERT INTO activity_records_new SELECT * FROM activity_records")
            database.execSQL("INSERT INTO maintenance_records_new SELECT * FROM maintenance_records")
            
            // Remover tabelas antigas
            database.execSQL("DROP TABLE fuel_records")
            database.execSQL("DROP TABLE activity_records")
            database.execSQL("DROP TABLE maintenance_records")
            
            // Renomear tabelas novas
            database.execSQL("ALTER TABLE fuel_records_new RENAME TO fuel_records")
            database.execSQL("ALTER TABLE activity_records_new RENAME TO activity_records")
            database.execSQL("ALTER TABLE maintenance_records_new RENAME TO maintenance_records")
            
            // Recriar índices
            database.execSQL("CREATE INDEX index_fuel_records_vehicleId ON fuel_records (vehicleId)")
            database.execSQL("CREATE INDEX index_fuel_records_date ON fuel_records (date)")
            database.execSQL("CREATE INDEX index_activity_records_vehicleId ON activity_records (vehicleId)")
            database.execSQL("CREATE INDEX index_activity_records_plate ON activity_records (plate)")
            database.execSQL("CREATE INDEX index_activity_records_date ON activity_records (date)")
            database.execSQL("CREATE INDEX index_activity_records_driver ON activity_records (driver)")
            database.execSQL("CREATE INDEX index_maintenance_records_vehicleId ON maintenance_records (vehicleId)")
            database.execSQL("CREATE INDEX index_maintenance_records_date ON maintenance_records (date)")
            database.execSQL("CREATE INDEX index_maintenance_records_type ON maintenance_records (type)")
            
            // Reabilitar chaves estrangeiras
            database.execSQL("PRAGMA foreign_keys = ON")
        }
    }
    
    /**
     * Migração da versão 11 para 12
     * Força recriação das tabelas para aplicar vehicleId opcional
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Desabilitar chaves estrangeiras
            database.execSQL("PRAGMA foreign_keys = OFF")
            
            // Recriar tabelas com estrutura correta
            database.execSQL("DROP TABLE IF EXISTS fuel_records")
            database.execSQL("DROP TABLE IF EXISTS activity_records")
            database.execSQL("DROP TABLE IF EXISTS maintenance_records")
            database.execSQL("DROP TABLE IF EXISTS vehicles")
            
            // Recriar tabela vehicles
            database.execSQL("""
                CREATE TABLE vehicles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    customId TEXT NOT NULL,
                    model TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    plate TEXT NOT NULL,
                    driver TEXT NOT NULL,
                    showInDiary INTEGER NOT NULL DEFAULT 1,
                    photoPath TEXT,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT
                )
            """)
            
            // Recriar tabela fuel_records com vehicleId opcional
            database.execSQL("""
                CREATE TABLE fuel_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    date TEXT NOT NULL,
                    fuelType TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    pricePerLiter TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    gasStation TEXT,
                    location TEXT,
                    receiptNumber TEXT,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Recriar tabela activity_records com vehicleId opcional
            database.execSQL("""
                CREATE TABLE activity_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    plate TEXT NOT NULL,
                    driver TEXT NOT NULL,
                    date TEXT NOT NULL,
                    startMileage INTEGER NOT NULL,
                    endMileage INTEGER NOT NULL,
                    observation TEXT,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Recriar tabela maintenance_records com vehicleId opcional
            database.execSQL("""
                CREATE TABLE maintenance_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    vehicleId INTEGER,
                    date TEXT NOT NULL,
                    type TEXT NOT NULL,
                    description TEXT NOT NULL,
                    mileage INTEGER NOT NULL,
                    laborCost TEXT NOT NULL,
                    partsCost TEXT NOT NULL,
                    totalCost TEXT NOT NULL,
                    workshop TEXT,
                    mechanic TEXT,
                    warrantyUntil TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 1,
                    nextMaintenanceMileage INTEGER,
                    nextRevisionDate TEXT,
                    minRevisionDate TEXT,
                    maxRevisionDate TEXT,
                    distanceToDealer INTEGER,
                    organizationId TEXT,
                    createdAt TEXT NOT NULL,
                    updatedAt TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(vehicleId) REFERENCES vehicles(id) ON DELETE CASCADE
                )
            """)
            
            // Recriar índices
            database.execSQL("CREATE INDEX index_fuel_records_vehicleId ON fuel_records (vehicleId)")
            database.execSQL("CREATE INDEX index_fuel_records_date ON fuel_records (date)")
            database.execSQL("CREATE INDEX index_activity_records_vehicleId ON activity_records (vehicleId)")
            database.execSQL("CREATE INDEX index_activity_records_plate ON activity_records (plate)")
            database.execSQL("CREATE INDEX index_activity_records_date ON activity_records (date)")
            database.execSQL("CREATE INDEX index_activity_records_driver ON activity_records (driver)")
            database.execSQL("CREATE INDEX index_maintenance_records_vehicleId ON maintenance_records (vehicleId)")
            database.execSQL("CREATE INDEX index_maintenance_records_date ON maintenance_records (date)")
            database.execSQL("CREATE INDEX index_maintenance_records_type ON maintenance_records (type)")
            
            // Reabilitar chaves estrangeiras
            database.execSQL("PRAGMA foreign_keys = ON")
        }
    }
    
    /**
     * Migração da versão 12 para 13
     * Adiciona campo requiresApproval na tabela organizations e define como true para dados legados
     */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                // Verificar se a coluna já existe antes de adicionar
                val cursor = database.query("PRAGMA table_info(organizations)")
                var columnExists = false
                while (cursor.moveToNext()) {
                    val columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    if (columnName == "requiresApproval") {
                        columnExists = true
                        break
                    }
                }
                cursor.close()
                
                // Adicionar campo requiresApproval apenas se não existir
                if (!columnExists) {
                    database.execSQL("ALTER TABLE organizations ADD COLUMN requiresApproval INTEGER NOT NULL DEFAULT 1")
                    println("DEBUG: Campo requiresApproval adicionado à tabela organizations")
                } else {
                    println("DEBUG: Campo requiresApproval já existe na tabela organizations")
                }
                
                // Atualizar todas as organizações existentes para ter aprovação manual ativada
                val updateResult = database.execSQL("UPDATE organizations SET requiresApproval = 1 WHERE requiresApproval IS NULL OR requiresApproval = 0")
                println("DEBUG: Organizações atualizadas para requiresApproval = 1")
                
            } catch (e: Exception) {
                println("DEBUG: Erro na migração 12->13: ${e.message}")
                // Em caso de erro, tentar adicionar a coluna de forma mais segura
                try {
                    database.execSQL("ALTER TABLE organizations ADD COLUMN requiresApproval INTEGER DEFAULT 1")
                    database.execSQL("UPDATE organizations SET requiresApproval = 1")
                } catch (e2: Exception) {
                    println("DEBUG: Erro crítico na migração: ${e2.message}")
                    throw e2
                }
            }
        }
    }

    /**
     * Migração da versão 13 para 14
     * Adiciona tabelas de notificações e convites de organização
     */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                println("DEBUG: Executando migração 13 -> 14: Adicionando tabelas de notificações")
                
                // Criar tabela organization_invites
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS organization_invites (
                        id TEXT NOT NULL PRIMARY KEY,
                        organizationId TEXT NOT NULL,
                        organizationName TEXT NOT NULL,
                        invitedUserId TEXT NOT NULL,
                        invitedUserEmail TEXT NOT NULL,
                        invitedUserName TEXT,
                        invitedByUserId TEXT NOT NULL,
                        invitedByUserEmail TEXT NOT NULL,
                        invitedByUserName TEXT,
                        message TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        expiresAt TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        lastSyncAt TEXT,
                        FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE
                    )
                """)
                
                // Criar índices para organization_invites
                database.execSQL("CREATE INDEX IF NOT EXISTS index_organization_invites_organizationId ON organization_invites (organizationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_organization_invites_invitedUserId ON organization_invites (invitedUserId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_organization_invites_invitedByUserId ON organization_invites (invitedByUserId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_organization_invites_status ON organization_invites (status)")
                
                // Criar tabela notifications
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        organizationId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        relatedUserId TEXT,
                        relatedUserName TEXT,
                        relatedUserEmail TEXT,
                        actionData TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        isActionable INTEGER NOT NULL DEFAULT 1,
                        createdAt TEXT NOT NULL,
                        readAt TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        lastSyncAt TEXT,
                        FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE
                    )
                """)
                
                // Criar índices para notifications
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_userId ON notifications (userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_organizationId ON notifications (organizationId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_type ON notifications (type)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_isRead ON notifications (isRead)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_notifications_createdAt ON notifications (createdAt)")
                
                println("DEBUG: Migração 13 -> 14 concluída com sucesso")
            } catch (e: Exception) {
                println("ERRO na migração 13 -> 14: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Migração da versão 14 para 15
     * Corrige problemas de schema das tabelas de notificações
     */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                println("DEBUG: Executando migração 14 -> 15: Corrigindo schema das tabelas")
                
                // Dropar e recriar tabelas problemáticas
                database.execSQL("DROP TABLE IF EXISTS organization_invites")
                database.execSQL("DROP TABLE IF EXISTS notifications")
                
                // Recriar organization_invites com schema correto
                database.execSQL("""
                    CREATE TABLE organization_invites (
                        id TEXT NOT NULL PRIMARY KEY,
                        organizationId TEXT NOT NULL,
                        organizationName TEXT NOT NULL,
                        invitedUserId TEXT NOT NULL,
                        invitedUserEmail TEXT NOT NULL,
                        invitedUserName TEXT,
                        invitedByUserId TEXT NOT NULL,
                        invitedByUserEmail TEXT NOT NULL,
                        invitedByUserName TEXT,
                        message TEXT,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        expiresAt TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        lastSyncAt TEXT,
                        FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE
                    )
                """)
                
                // Recriar notifications com schema correto
                database.execSQL("""
                    CREATE TABLE notifications (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        organizationId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL,
                        relatedUserId TEXT,
                        relatedUserName TEXT,
                        relatedUserEmail TEXT,
                        actionData TEXT,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        isActionable INTEGER NOT NULL DEFAULT 1,
                        createdAt TEXT NOT NULL,
                        readAt TEXT,
                        isSynced INTEGER NOT NULL DEFAULT 0,
                        lastSyncAt TEXT,
                        FOREIGN KEY(organizationId) REFERENCES organizations(id) ON DELETE CASCADE
                    )
                """)
                
                // Recriar índices
                database.execSQL("CREATE INDEX index_organization_invites_organizationId ON organization_invites (organizationId)")
                database.execSQL("CREATE INDEX index_organization_invites_invitedUserId ON organization_invites (invitedUserId)")
                database.execSQL("CREATE INDEX index_organization_invites_invitedByUserId ON organization_invites (invitedByUserId)")
                database.execSQL("CREATE INDEX index_organization_invites_status ON organization_invites (status)")
                
                database.execSQL("CREATE INDEX index_notifications_userId ON notifications (userId)")
                database.execSQL("CREATE INDEX index_notifications_organizationId ON notifications (organizationId)")
                database.execSQL("CREATE INDEX index_notifications_type ON notifications (type)")
                database.execSQL("CREATE INDEX index_notifications_isRead ON notifications (isRead)")
                database.execSQL("CREATE INDEX index_notifications_createdAt ON notifications (createdAt)")
                
                println("DEBUG: Migração 14 -> 15 concluída com sucesso")
            } catch (e: Exception) {
                println("ERRO na migração 14 -> 15: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Migração de emergência para qualquer versão anterior para 13
     * Garante que dados importantes sejam preservados
     */
    val MIGRATION_ANY_TO_13 = object : Migration(1, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            try {
                println("DEBUG: Executando migração de emergência para versão 13")
                
                // Verificar se as tabelas existem
                val tables = mutableListOf<String>()
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table'")
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
                cursor.close()
                
                println("DEBUG: Tabelas encontradas: $tables")
                
                // Se as tabelas principais existem, apenas adicionar o campo requiresApproval
                if (tables.contains("organizations")) {
                    try {
                        val orgCursor = database.query("PRAGMA table_info(organizations)")
                        var hasRequiresApproval = false
                        while (orgCursor.moveToNext()) {
                            val columnName = orgCursor.getString(orgCursor.getColumnIndexOrThrow("name"))
                            if (columnName == "requiresApproval") {
                                hasRequiresApproval = true
                                break
                            }
                        }
                        orgCursor.close()
                        
                        if (!hasRequiresApproval) {
                            database.execSQL("ALTER TABLE organizations ADD COLUMN requiresApproval INTEGER DEFAULT 1")
                            database.execSQL("UPDATE organizations SET requiresApproval = 1")
                            println("DEBUG: Campo requiresApproval adicionado via migração de emergência")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Erro ao adicionar requiresApproval: ${e.message}")
                    }
                }
                
                println("DEBUG: Migração de emergência concluída")
                
            } catch (e: Exception) {
                println("DEBUG: Erro na migração de emergência: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Lista de todas as migrações disponíveis
     * Ordenadas por versão de origem
     */
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
        MIGRATION_12_13,
        MIGRATION_13_14,
        MIGRATION_14_15,
        MIGRATION_ANY_TO_13
    )
    
    /**
     * Obtém as migrações necessárias entre duas versões
     */
    fun getMigrations(fromVersion: Int, toVersion: Int): Array<Migration> {
        return ALL_MIGRATIONS.filter { 
            it.startVersion >= fromVersion && it.endVersion <= toVersion 
        }.toTypedArray()
    }
}