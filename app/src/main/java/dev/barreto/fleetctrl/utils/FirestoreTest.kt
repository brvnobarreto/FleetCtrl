package dev.barreto.fleetctrl.utils

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirestoreTest {
    
    suspend fun testConnection(): Boolean {
        return try {
            println("🔍 Testing Firestore connection...")
            
            // Verificar se Firebase está inicializado
            val firebaseApp = FirebaseApp.getInstance()
            println("📱 Firebase App initialized: ${firebaseApp.name}")
            
            val firestore = FirebaseFirestore.getInstance()
            println("📱 Project ID: ${firestore.app.options.projectId}")
            println("📱 App ID: ${firestore.app.options.applicationId}")
            
            // Verificar autenticação
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            if (currentUser != null) {
                println("✅ User authenticated: ${currentUser.uid}")
                println("📧 User email: ${currentUser.email}")
            } else {
                println("❌ User NOT authenticated!")
                println("🔑 Please login first before testing Firestore")
                return false
            }
            
            // Teste simples: tentar acessar user_organizations (sempre permitido para o próprio usuário)
            val result = firestore.collection("user_organizations")
                .whereEqualTo("userId", currentUser.uid)
                .limit(1)
                .get()
                .await()
            
            println("✅ Firestore connection successful!")
            println("📊 Documents found: ${result.size()}")
            println("📊 Metadata: ${result.metadata.isFromCache}")
            
            true
        } catch (e: Exception) {
            println("❌ Firestore connection failed: ${e.message}")
            println("🔍 Error type: ${e.javaClass.simpleName}")
            println("🔍 Error details: ${e.localizedMessage}")
            
            // Verificar tipos específicos de erro
            when (e) {
                is com.google.firebase.FirebaseException -> {
                    println("🔥 Firebase Exception: Message=${e.message}")
                }
                is java.net.UnknownHostException -> {
                    println("🌐 Network Error: No internet connection or DNS issues")
                }
                is java.net.SocketTimeoutException -> {
                    println("⏰ Timeout Error: Request took too long")
                }
                is java.security.cert.CertificateException -> {
                    println("🔒 Certificate Error: SSL/TLS certificate issues")
                }
                else -> {
                    println("❓ Unknown Error: ${e.javaClass.simpleName}")
                }
            }
            
            false
        }
    }
    
    suspend fun testWrite(): Boolean {
        return try {
            println("📝 Testing Firestore write...")
            
            val firestore = FirebaseFirestore.getInstance()
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                println("❌ User not authenticated for write test")
                return false
            }
            
            // As regras exigem: docId = "uid_orgId" e role ∈ {viewer, owner}.
            // Vamos criar um vínculo viewer temporário com um orgId fictício.
            val orgId = "test_org_${System.currentTimeMillis()}"
            val testDocId = "${currentUser.uid}_${orgId}"

            val testData = mapOf(
                "userId" to currentUser.uid,
                "organizationId" to orgId,
                "role" to "viewer",
                "joinedAt" to com.google.firebase.Timestamp.now(),
                "isActive" to true
            )

            firestore.collection("user_organizations").document(testDocId).set(testData).await()
            
            println("✅ Firestore write test successful!")
            
            // Limpar o documento de teste
            try {
                firestore.collection("user_organizations")
                    .document(testDocId)
                    .delete()
                    .await()
                println("🧹 Test document cleaned up")
            } catch (cleanupError: Exception) {
                println("⚠️ Could not clean up test document: ${cleanupError.message}")
            }
            
            true
        } catch (e: Exception) {
            println("❌ Firestore write test failed: ${e.message}")
            println("🔍 Error type: ${e.javaClass.simpleName}")
            println("🔍 Error details: ${e.localizedMessage}")
            false
        }
    }
    
    suspend fun testFullConnection(): Boolean {
        println("🚀 Starting full Firestore test...")
        
        val connectionTest = testConnection()
        val writeTest = testWrite()
        
        val allTestsPassed = connectionTest && writeTest
        
        if (allTestsPassed) {
            println("✅ All Firestore tests passed!")
        } else {
            println("❌ Some Firestore tests failed!")
        }
        
        return allTestsPassed
    }
}
