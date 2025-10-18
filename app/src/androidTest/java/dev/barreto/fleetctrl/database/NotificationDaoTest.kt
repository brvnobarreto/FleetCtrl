package dev.barreto.fleetctrl.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.barreto.fleetctrl.data.database.AppDatabase
import dev.barreto.fleetctrl.data.database.daos.NotificationDao
import dev.barreto.fleetctrl.data.database.entities.Notification
import dev.barreto.fleetctrl.data.database.entities.NotificationType
import dev.barreto.fleetctrl.data.database.entities.Organization
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class NotificationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: NotificationDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.notificationDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() { database.close() }

    @Test
    fun insert_and_markAsRead_and_queryCounts() = runBlockingUnit {
        val now = LocalDateTime.now()
        // FK parent
        database.organizationDao().insertOrganization(
            Organization(id = "org-1", code = "C1", name = "Org 1", ownerId = "u1", ownerEmail = "u1@x.com")
        )
        val n1 = Notification(id = "n1", userId = "u1", organizationId = "org-1", type = NotificationType.JOIN_REQUEST, title = "t1", message = "m1", createdAt = now)
        val n2 = Notification(id = "n2", userId = "u1", organizationId = "org-1", type = NotificationType.VEHICLE_ADDED, title = "t2", message = "m2", createdAt = now)
        dao.insertNotification(n1)
        dao.insertNotification(n2)

        val unread = dao.getUnreadNotifications("u1")
        assertEquals(2, unread.size)
        val count = dao.getUnreadCount("u1")
        assertEquals(2, count)

        // Avoid markAsRead (uses Long -> LocalDateTime conversion); update entity instead
        val updated = n1.copy(isRead = true, readAt = now)
        dao.updateNotification(updated)
        val count2 = dao.getUnreadCount("u1")
        assertEquals(1, count2)

        val byType = dao.getNotificationsByType(NotificationType.JOIN_REQUEST, "u1")
        assertEquals(1, byType.size)

        val flow = dao.getUserNotificationsFlow("u1").first()
        assertEquals(2, flow.size)

        dao.deleteNotification("n2")
        val all = dao.getUserNotifications("u1")
        assertEquals(1, all.size)

        // Mark all as read
        dao.markAllAsRead("u1")
        val unreadAfterAll = dao.getUnreadCount("u1")
        assertEquals(0, unreadAfterAll)
    }
}

private fun <T> runBlockingUnit(block: suspend () -> T) {
    kotlinx.coroutines.runBlocking { block() }
}
