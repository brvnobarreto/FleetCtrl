package dev.barreto.fleetctrl.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DataRefreshNotifierTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshTrigger should be a SharedFlow`() {
        // Assert
        assertTrue(DataRefreshNotifier.refreshTrigger is kotlinx.coroutines.flow.SharedFlow<Unit>)
    }

    @Test
    fun `triggerRefresh should not throw exception`() = runTest {
        // Act & Assert
        try {
            DataRefreshNotifier.triggerRefresh()
            // If we get here, the function executed without throwing
            assertTrue(true)
        } catch (e: Exception) {
            fail("triggerRefresh should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `triggerRefreshSync should not throw exception`() {
        // Act & Assert
        try {
            DataRefreshNotifier.triggerRefreshSync()
            // If we get here, the function executed without throwing
            assertTrue(true)
        } catch (e: Exception) {
            fail("triggerRefreshSync should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `multiple refresh triggers should not throw exception`() = runTest {
        // Act & Assert
        try {
            DataRefreshNotifier.triggerRefresh()
            DataRefreshNotifier.triggerRefresh()
            DataRefreshNotifier.triggerRefresh()
            // If we get here, all functions executed without throwing
            assertTrue(true)
        } catch (e: Exception) {
            fail("Multiple refresh triggers should not throw exception: ${e.message}")
        }
    }
}
