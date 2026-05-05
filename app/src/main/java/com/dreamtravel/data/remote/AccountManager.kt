package com.dreamtravel.data.remote

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AccountManager @Inject constructor(
    private val auth: FirebaseAuth?
) {

    fun isSignedIn(): Boolean = auth?.currentUser != null

    fun isAnonymous(): Boolean = auth?.currentUser?.isAnonymous == true

    fun getUserEmail(): String? = auth?.currentUser?.email

    suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        val firebaseAuth = auth ?: return AuthResult.Error("Firebase 不可用")
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val firebaseAuth = auth ?: return AuthResult.Error("Firebase 不可用")
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    suspend fun linkAnonymousToEmail(email: String, password: String): AuthResult {
        val firebaseAuth = auth ?: return AuthResult.Error("Firebase 不可用")
        val currentUser = firebaseAuth.currentUser
            ?: return AuthResult.Error("未登录")

        if (!currentUser.isAnonymous) {
            return AuthResult.Error("当前账户已关联邮箱")
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        return try {
            currentUser.linkWithCredential(credential).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    suspend fun signOut(): AuthResult {
        val firebaseAuth = auth
        return try {
            firebaseAuth?.signOut()
            // After sign-out, re-sign-in anonymously for continued use
            if (firebaseAuth != null) {
                firebaseAuth.signInAnonymously().await()
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    private fun mapAuthException(e: Exception): String {
        val message = e.message ?: "未知错误"
        return when {
            message.contains("email address is already in use", ignoreCase = true) ||
            message.contains("account already exists", ignoreCase = true) ->
                "该邮箱已被注册"
            message.contains("email address is badly formatted", ignoreCase = true) ||
            message.contains("invalid email", ignoreCase = true) ->
                "邮箱格式无效"
            message.contains("password is invalid", ignoreCase = true) ||
            message.contains("wrong password", ignoreCase = true) ->
                "密码错误"
            message.contains("user not found", ignoreCase = true) ->
                "该邮箱未注册"
            message.contains("credential is already in use", ignoreCase = true) ->
                "该邮箱已关联到其他账户"
            message.contains("requires recent authentication", ignoreCase = true) ->
                "操作需要重新登录"
            message.contains("network", ignoreCase = true) ->
                "网络错误，请检查网络连接"
            message.contains("weak password", ignoreCase = true) ->
                "密码强度不够，请使用至少6位密码"
            else -> "操作失败：${message.take(80)}"
        }
    }
}
