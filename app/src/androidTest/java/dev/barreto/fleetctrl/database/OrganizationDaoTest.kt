package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.OrganizationDao
import dev.barreto.fleetctrl.data.database.entities.Organization
import dev.barreto.fleetctrl.data.database.entities.UserOrganization
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class OrganizationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: OrganizationDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.organizationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { database.close() }

    @Test
    fun insertUpdateAndQueryOrganization() = runBlockingUnit {
        val org = Organization(id = "org-1", code = "C1", name = "Org 1", ownerId = "u1", ownerEmail = "u1@x.com")
        dao.insertOrganization(org)
        val fetched = dao.getOrganizationById("org-1")
        assertNotNull(fetched)
        assertEquals("Org 1", fetched!!.name)

        val updated = fetched.copy(name = "Org 1 Updated")
        dao.updateOrganization(updated)
        val fetched2 = dao.getOrganizationById("org-1")
        assertEquals("Org 1 Updated", fetched2!!.name)
    }

    @Test
    fun userOrganizationsRelations_andSyncFlags() = runBlockingUnit {
        val now = LocalDateTime.now()
        val org = Organization(id = "org-1", code = "C1", name = "Org 1", ownerId = "u1", ownerEmail = "u1@x.com")
        dao.insertOrganization(org)
        val uo1 = UserOrganization(userId = "u1", organizationId = "org-1", role = "owner", joinedAt = now)
        val uo2 = UserOrganization(userId = "u2", organizationId = "org-1", role = "editor", joinedAt = now)
        dao.insertUserOrganization(uo1)
        dao.insertUserOrganization(uo2)

        val members = dao.getOrganizationMembers("org-1").first()
        assertEquals(2, members.size)

        // Details flow should return the organization itself
        val orgsForU1 = dao.getUserOrganizationsWithDetails("u1").first()
        assertEquals(1, orgsForU1.size)
        assertEquals("org-1", orgsForU1.first().id)

        // Mark sync flags
        dao.markOrganizationAsSynced("org-1", now)
        dao.markUserOrganizationAsSynced("u1", "org-1", now)

        val syncedOrg = dao.getOrganizationById("org-1")
        assertTrue(syncedOrg!!.isSynced)
        assertEquals(now, syncedOrg.lastSyncAt)

        // Remove user from all orgs
        dao.removeUserFromAllOrganizations("u2")
        val membersAfter = dao.getOrganizationMembers("org-1").first()
        assertEquals(1, membersAfter.size)
    }
}

private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
