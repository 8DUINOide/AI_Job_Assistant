package com.aijobassistant.app.data.profile

import com.aijobassistant.app.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.aijobassistant.app.data.network.ApiClient

/**
 * Repository for managing user profiles in Firestore.
 * Firestore path: users/{uid}/profile/master
 */
class ProfileRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String? get() = auth.currentUser?.uid

    private fun profileDocRef() = uid?.let {
        firestore.collection("users").document(it)
            .collection("profile").document("master")
    }

    /**
     * Save or update the user's profile.
     */
    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val docRef = profileDocRef() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            docRef.set(profile.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get the user's profile (one-shot).
     */
    suspend fun getProfile(): Result<UserProfile?> {
        val docRef = profileDocRef() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val doc = docRef.get().await()
            if (doc.exists() && doc.data != null) {
                Result.success(UserProfile.fromMap(doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe the user's profile in real-time.
     */
    fun observeProfile(): Flow<UserProfile?> = callbackFlow {
        val docRef = profileDocRef()
        if (docRef == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists() && snapshot.data != null) {
                trySend(UserProfile.fromMap(snapshot.data!!))
            } else {
                trySend(null)
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Delete the user's profile.
     */
    suspend fun deleteProfile(): Result<Unit> {
        val docRef = profileDocRef() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload resume PDF to Vercel API to build the profile using Gemini.
     */
    suspend fun buildProfileFromPdf(pdfBytes: ByteArray, portfolioUrl: String): Result<UserProfile> {
        return try {
            val requestFile = pdfBytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "resume.pdf", requestFile)
            val urlBody = portfolioUrl.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = ApiClient.apiService.buildProfile(body, urlBody)
            
            if (response.success && response.profile != null) {
                var userProfile = UserProfile.fromMap(response.profile)
                
                // Fallback to basic info from auth document if resume extraction missed it
                uid?.let { userId ->
                    try {
                        val userDoc = firestore.collection("users").document(userId).get().await()
                        if (userDoc.exists()) {
                            val authFirstName = userDoc.getString("firstName") ?: ""
                            val authLastName = userDoc.getString("lastName") ?: ""
                            val authEmail = userDoc.getString("email") ?: ""
                            
                            userProfile = userProfile.copy(
                                personalInfo = userProfile.personalInfo.copy(
                                    firstName = userProfile.personalInfo.firstName.ifBlank { authFirstName },
                                    lastName = userProfile.personalInfo.lastName.ifBlank { authLastName },
                                    email = userProfile.personalInfo.email.ifBlank { authEmail }
                                ),
                                isFromResume = true
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Save to Firestore
                saveProfile(userProfile)
                Result.success(userProfile)
            } else {
                Result.failure(Exception(response.error ?: "Failed to build profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
