package dev.barreto.fleetctrl.data.repositories

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleClient: GoogleSignInClient
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        auth = mockk(relaxed = true)
        googleClient = mockk(relaxed = true)
        repository = AuthRepository(auth, googleClient)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `isLoggedIn reflects currentUser presence`() {
        every { auth.currentUser } returns null
        assertFalse(repository.isLoggedIn())

        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun `getCurrentUser returns FirebaseUser`() {
        val user = mockk<FirebaseUser>()
        every { auth.currentUser } returns user
        assertEquals(user, repository.getCurrentUser())
    }

    @Test
    fun `currentUser flow emits initial user and updates`() = runTest {
        val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
        val initialUser = mockk<FirebaseUser>()
        val updatedUser = mockk<FirebaseUser>()

        every { auth.addAuthStateListener(capture(listenerSlot)) } answers { }
        every { auth.removeAuthStateListener(any()) } answers { }
        every { auth.currentUser } returns initialUser andThen updatedUser

        val first = repository.currentUser.first()
        assertEquals(initialUser, first)

        // Simulate auth state change
        listenerSlot.captured.onAuthStateChanged(auth)
        val second = repository.currentUser.first()
        assertEquals(updatedUser, second)
    }

    @Test
    fun `getSignInIntent delegates to GoogleSignInClient`() {
        val intent = mockk<Intent>()
        every { googleClient.signInIntent } returns intent
        assertEquals(intent, repository.getSignInIntent())
    }

    @Test
    fun `signOut returns success when auth and google signOut succeed`() = runTest {
        // auth.signOut() is a void call
        justRun { auth.signOut() }
        val task = mockk<Task<Void>>()
        every { googleClient.signOut() } returns task
        val voidResult = mockk<java.lang.Void>()
        coEvery { task.await() } returns voidResult

        val result = repository.signOut()
        assertTrue(result.isSuccess)
        verify { auth.signOut() }
        verify { googleClient.signOut() }
    }

    @Test
    fun `signOut returns failure when google signOut fails`() = runTest {
        justRun { auth.signOut() }
        val task = mockk<Task<Void>>()
        every { googleClient.signOut() } returns task
        coEvery { task.await() } throws RuntimeException("network")

        val result = repository.signOut()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getIdToken returns token when available`() = runTest {
        val user = mockk<FirebaseUser>()
        val tokenTask = mockk<Task<GetTokenResult>>()
        val tokenResult = mockk<GetTokenResult>()
        every { auth.currentUser } returns user
        every { user.getIdToken(false) } returns tokenTask
        coEvery { tokenTask.await() } returns tokenResult
        every { tokenResult.token } returns "token-123"

        val token = repository.getIdToken()
        assertEquals("token-123", token)
    }

    @Test
    fun `getIdToken returns null on error`() = runTest {
        val user = mockk<FirebaseUser>()
        val tokenTask = mockk<Task<GetTokenResult>>()
        every { auth.currentUser } returns user
        every { user.getIdToken(false) } returns tokenTask
        coEvery { tokenTask.await() } throws RuntimeException("boom")

        val token = repository.getIdToken()
        assertNull(token)
    }
}
