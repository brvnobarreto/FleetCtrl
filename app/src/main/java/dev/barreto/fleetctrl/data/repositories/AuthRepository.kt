package dev.barreto.fleetctrl.data.repositories

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    val googleSignInClient: GoogleSignInClient
) {
    
    /**
     * Flow que emite o usuário atual (null se não estiver logado)
     */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        
        // Enviar usuário atual imediatamente
        trySend(auth.currentUser)
        
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }
    
    /**
     * Verifica se o usuário está logado
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null
    
    /**
     * Obtém o usuário atual
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    /**
     * Faz login com Google usando o resultado do ActivityResult
     */
    suspend fun signInWithGoogle(data: android.content.Intent?): kotlin.Result<FirebaseUser> {
        return try {
            println("DEBUG AuthRepository: Starting Google Sign-In process")
            
            if (data == null) {
                println("DEBUG AuthRepository: Intent data is null")
                return kotlin.Result.failure(Exception("Intent data is null"))
            }
            
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
            println("DEBUG AuthRepository: Got task from intent")
            
            val account = task.getResult(ApiException::class.java)
            println("DEBUG AuthRepository: Got account: ${account?.email}")
            println("DEBUG AuthRepository: Account ID token: ${account?.idToken?.take(20)}...")
            
            if (account == null) {
                println("DEBUG AuthRepository: Account is null")
                return kotlin.Result.failure(Exception("Account is null"))
            }
            
            // Verificar se temos ID token
            if (account.idToken == null) {
                println("DEBUG AuthRepository: ID token is null")
                return kotlin.Result.failure(Exception("ID token is null - Google Sign-In not properly configured. Verifique o Web Client ID no Firebase Console."))
            }
            
            // Obter credenciais do Firebase
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            println("DEBUG AuthRepository: Created Firebase credential")
            
            // Fazer login no Firebase
            val authResult = auth.signInWithCredential(credential).await()
            println("DEBUG AuthRepository: Firebase auth successful: ${authResult.user?.email}")
            
            kotlin.Result.success(authResult.user!!)
        } catch (e: ApiException) {
            println("DEBUG AuthRepository: ApiException: ${e.statusCode} - ${e.message}")
            when (e.statusCode) {
                7 -> kotlin.Result.failure(Exception("SIGN_IN_FAILED (7): Google Sign-In não está habilitado no Firebase Console. Vá em Authentication > Sign-in method > Google > Enable"))
                8 -> kotlin.Result.failure(Exception("NETWORK_ERROR (8): Verifique sua conexão com a internet"))
                10 -> kotlin.Result.failure(Exception("DEVELOPER_ERROR (10): Verifique o SHA-1 e package name no Firebase Console"))
                else -> kotlin.Result.failure(Exception("Google Sign-In Error ${e.statusCode}: ${e.message}"))
            }
        } catch (e: Exception) {
            println("DEBUG AuthRepository: Exception: ${e.message}")
            kotlin.Result.failure(e)
        }
    }
    
    /**
     * Obtém o Intent para iniciar o Google Sign-In
     */
    fun getSignInIntent(): android.content.Intent {
        return googleSignInClient.signInIntent
    }
    
    /**
     * Faz logout
     */
    suspend fun signOut(): kotlin.Result<Unit> {
        return try {
            auth.signOut()
            googleSignInClient.signOut().await()
            kotlin.Result.success(Unit)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    /**
     * Obtém o ID do token para autenticação
     */
    suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}