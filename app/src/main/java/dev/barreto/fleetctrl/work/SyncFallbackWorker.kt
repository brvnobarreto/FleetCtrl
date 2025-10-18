package dev.barreto.fleetctrl.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.barreto.fleetctrl.data.database.daos.OrganizationDao
import dev.barreto.fleetctrl.data.repositories.SyncRepository
import kotlinx.coroutines.flow.first

class SyncFallbackWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDepsEntryPoint {
        fun organizationDao(): OrganizationDao
        fun syncRepository(): SyncRepository
    }

    private val deps: WorkerDepsEntryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerDepsEntryPoint::class.java
        )
    }

    override suspend fun doWork(): Result {
        return try {
            // Tenta sincronizar incrementalmente todos os escopos para todas as orgs ativas
            val orgs = deps.organizationDao().getAllActiveOrganizations().first()
            orgs.forEach { org ->
                val orgId = org.id
                deps.syncRepository().syncVehiclesOnly(orgId)
                deps.syncRepository().syncFuelOnly(orgId)
                deps.syncRepository().syncActivityOnly(orgId)
                deps.syncRepository().syncMaintenanceOnly(orgId)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
