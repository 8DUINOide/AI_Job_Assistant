package com.aijobassistant.app.data.tracker

import com.aijobassistant.app.model.Application
import com.aijobassistant.app.model.ApplicationStatus
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing job applications in Firestore.
 * Replaces the Google Sheets tracker from tracker.py.
 * Firestore path: users/{uid}/applications/{docId}
 */
class TrackerRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val uid: String? get() = auth.currentUser?.uid

    private fun applicationsCollection() = uid?.let {
        firestore.collection("users").document(it).collection("applications")
    }

    /**
     * Add a new application.
     */
    suspend fun addApplication(application: Application): Result<String> {
        val collection = applicationsCollection() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val docRef = collection.add(application.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add multiple applications in a batch (replaces log_applications_batch from tracker.py).
     */
    suspend fun addApplicationsBatch(applications: List<Application>): Result<Int> {
        val collection = applicationsCollection() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val batch = firestore.batch()
            applications.forEach { app ->
                val docRef = collection.document()
                batch.set(docRef, app.toMap())
            }
            batch.commit().await()
            Result.success(applications.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the status of an application (replaces update_application_status from tracker.py).
     */
    suspend fun updateStatus(applicationId: String, newStatus: ApplicationStatus): Result<Unit> {
        val collection = applicationsCollection() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            collection.document(applicationId)
                .update("status", newStatus.value)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an application.
     */
    suspend fun deleteApplication(applicationId: String): Result<Unit> {
        val collection = applicationsCollection() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            collection.document(applicationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all applications (one-shot, replaces get_recent_logs from tracker.py).
     */
    suspend fun getApplications(limit: Int = 200): Result<List<Application>> {
        val collection = applicationsCollection() ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val snapshot = collection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val apps = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Application.fromMap(doc.id, it) }
            }
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe applications in real-time.
     */
    fun observeApplications(): Flow<List<Application>> = callbackFlow {
        val collection = applicationsCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val apps = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.let { Application.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(apps)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all applied job signatures (for deduplication, replaces get_applied_job_ids from tracker.py).
     */
    suspend fun getAppliedSignatures(): Set<String> {
        val collection = applicationsCollection() ?: return emptySet()
        return try {
            val snapshot = collection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val company = (data["company"] as? String)?.lowercase() ?: ""
                val title = (data["jobTitle"] as? String)?.lowercase() ?: ""
                if (company.isNotBlank() && title.isNotBlank()) "$company|$title" else null
            }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}
