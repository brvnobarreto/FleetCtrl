package dev.barreto.fleetctrl.di

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.*
import dev.barreto.fleetctrl.data.preferences.AppPreferences
import dev.barreto.fleetctrl.data.repositories.*
import javax.inject.Singleton

/**
 * Módulo Dagger Hilt para injeção de dependência do banco de dados
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @RequiresApi(Build.VERSION_CODES.P)
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideVehicleDao(database: AppDatabase): VehicleDao {
        return database.vehicleDao()
    }
    
    @Provides
    fun provideFuelRecordDao(database: AppDatabase): FuelRecordDao {
        return database.fuelRecordDao()
    }
    
    
    @Provides
    fun provideMaintenanceRecordDao(database: AppDatabase): MaintenanceRecordDao {
        return database.maintenanceRecordDao()
    }
    
    @Provides
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }
    
    @Provides
    fun provideOrganizationDao(database: AppDatabase): OrganizationDao {
        return database.organizationDao()
    }
    
    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }
    
    @Provides
    @Singleton
    fun provideVehicleRepository(
        vehicleDao: VehicleDao,
        syncRepository: SyncRepository,
        fuelRecordDao: FuelRecordDao,
        activityRecordDao: ActivityRecordDao,
        maintenanceRecordDao: MaintenanceRecordDao
    ): VehicleRepository {
        return VehicleRepository(
            vehicleDao = vehicleDao,
            syncRepository = syncRepository,
            fuelRecordDao = fuelRecordDao,
            activityRecordDao = activityRecordDao,
            maintenanceRecordDao = maintenanceRecordDao
        )
    }
    
    @Provides
    @Singleton
    fun provideFuelRecordRepository(fuelRecordDao: FuelRecordDao, syncRepository: SyncRepository): FuelRecordRepository {
        return FuelRecordRepository(fuelRecordDao, syncRepository)
    }
    
    
    @Provides
    @Singleton
    fun provideActivityRecordRepository(activityRecordDao: ActivityRecordDao, syncRepository: SyncRepository): ActivityRecordRepository {
        return ActivityRecordRepository(activityRecordDao, syncRepository)
    }
    
    @Provides
    @Singleton
    fun provideMaintenanceRecordRepository(maintenanceRecordDao: MaintenanceRecordDao, syncRepository: SyncRepository): MaintenanceRecordRepository {
        return MaintenanceRecordRepository(maintenanceRecordDao, syncRepository)
    }
    
    @Provides
    @Singleton
    fun provideFleetRepository(
        vehicleRepository: VehicleRepository,
        fuelRecordRepository: FuelRecordRepository,
        maintenanceRecordRepository: MaintenanceRecordRepository,
        syncRepository: SyncRepository
    ): FleetRepository {
        return FleetRepository(
            vehicleRepository = vehicleRepository,
            fuelRecordRepository = fuelRecordRepository,
            maintenanceRecordRepository = maintenanceRecordRepository,
            syncRepository = syncRepository
        )
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        notificationDao: NotificationDao
    ): NotificationRepository {
        return NotificationRepository(firestore, notificationDao)
    }

    @Provides
    @Singleton
    fun provideOrganizationRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        firebaseAuth: com.google.firebase.auth.FirebaseAuth,
        notificationRepository: NotificationRepository
    ): dev.barreto.fleetctrl.data.repositories.OrganizationRepository {
        return dev.barreto.fleetctrl.data.repositories.OrganizationRepository(
            firestore,
            firebaseAuth,
            notificationRepository
        )
    }

    @Provides
    @Singleton
    fun provideDataManagementViewModel(
        appPreferences: AppPreferences,
        organizationRepository: dev.barreto.fleetctrl.data.repositories.OrganizationRepository
    ): dev.barreto.fleetctrl.viewmodels.DataManagementViewModel {
        return dev.barreto.fleetctrl.viewmodels.DataManagementViewModel(appPreferences, organizationRepository)
    }
    
    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences {
        return AppPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSyncRepository(
        firestore: com.google.firebase.firestore.FirebaseFirestore,
        vehicleDao: VehicleDao,
        fuelRecordDao: FuelRecordDao,
        activityRecordDao: ActivityRecordDao,
        maintenanceRecordDao: MaintenanceRecordDao,
        organizationDao: OrganizationDao,
        appPreferences: AppPreferences
    ): SyncRepository {
        return SyncRepository(
            firestore,
            vehicleDao,
            fuelRecordDao,
            activityRecordDao,
            maintenanceRecordDao,
            organizationDao,
            appPreferences
        )
    }
}
