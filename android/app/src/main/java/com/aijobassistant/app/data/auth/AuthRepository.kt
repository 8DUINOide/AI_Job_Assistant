package com.aijobassistant.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository wrapping Firebase Authentication.
 * Handles email/password login, signup, Google Sign-In, and session management.
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    /** Currently signed-in user, or null */
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** Whether a user is currently signed in */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /** Observe auth state changes as a Flow */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign up with email and password.
     * Also creates the user document in Firestore.
     */
    suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign up failed: no user returned")

            // Create initial user document in Firestore
            val userData = mapOf(
                "email" to email,
                "firstName" to firstName,
                "lastName" to lastName,
                "role" to "USER",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "onboardingComplete" to false
            )
            firestore.collection("users").document(user.uid).set(userData).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed: no user returned")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google credential (ID token from Google Sign-In).
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google sign in failed")

            // Check if this is a new user and create Firestore doc if needed
            if (result.additionalUserInfo?.isNewUser == true) {
                val userData = mapOf(
                    "email" to (user.email ?: ""),
                    "firstName" to (user.displayName?.split(" ")?.firstOrNull() ?: ""),
                    "lastName" to (user.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: ""),
                    "role" to "USER",
                    "createdAt" to com.google.firebase.Timestamp.now(),
                    "onboardingComplete" to false
                )
                firestore.collection("users").document(user.uid).set(userData).await()
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if onboarding has been completed for the current user.
     */
    suspend fun isOnboardingComplete(): Boolean {
        val uid = currentUser?.uid ?: return false
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.getBoolean("onboardingComplete") ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mark onboarding as complete.
     */
    suspend fun markOnboardingComplete() {
        val uid = currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid)
                .update("onboardingComplete", true)
                .await()
        } catch (e: Exception) {
            // Log but don't crash
            e.printStackTrace()
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Delete the current user's account and all their data.
     */
    suspend fun deleteAccount(): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("No user signed in"))
        return try {
            // Delete Firestore data
            val uid = user.uid
            firestore.collection("users").document(uid).delete().await()

            // Delete the Firebase Auth user
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
