package com.aijobassistant.app.data.jobs

import com.aijobassistant.app.data.network.ApiClient
import com.aijobassistant.app.data.network.AnalyzeResumeRequest
import com.aijobassistant.app.data.network.EvaluateJobRequest
import com.aijobassistant.app.data.network.GeneratePdfRequest
import com.aijobassistant.app.data.network.JobSearchRequest
import com.aijobassistant.app.data.profile.ProfileRepository
import com.aijobassistant.app.model.Job
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.ResponseBody
import kotlinx.coroutines.channels.awaitClose

/**
 * Repository for job discovery operations.
 * Calls the free Vercel Python backend for scraping and evaluation.
 */
class JobsRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val profileRepository = ProfileRepository()
    private val api = ApiClient.apiService

    /**
     * Search for jobs using Vercel API.
     */
    suspend fun searchJobs(keyword: String, location: String = "Remote", offset: Int = 0, resultsWanted: Int = 30): Result<List<Job>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val userDoc = firestore.collection("users").document(uid).collection("profile").document("master").get().await()
            val profile = if (userDoc.exists() && userDoc.data != null) {
                userDoc.data!!
            } else {
                return Result.failure(Exception("Profile not found"))
            }

            val request = JobSearchRequest(
                search_keyword = keyword, 
                location = location, 
                offset = offset, 
                uid = uid,
                results_wanted = resultsWanted,
                profile = profile
            )
            val response = api.searchJobs(request)
            if (response.success && response.jobs != null) {
                val jobs = response.jobs.map { Job.fromMap(it) }
                Result.success(jobs)
            } else {
                Result.failure(Exception("Failed to fetch jobs"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluate a list of jobs against the user's profile using Vercel API.
     */
    suspend fun evaluateJobs(jobs: List<Map<String, Any?>>): Result<List<Job>> {
        return try {
            val profileResult = profileRepository.getProfile()
            if (profileResult.isFailure || profileResult.getOrNull() == null) {
                return Result.success(jobs.map { Job.fromMap(it) }) // Return unevaluated if no profile
            }
            val profile = profileResult.getOrNull()!!.toMap()
            
            val evaluatedJobs = mutableListOf<Job>()
            
            // Note: In production we'd use a batch endpoint, but evaluating one by one here based on existing single endpoint.
            for (jobData in jobs) {
                try {
                    val resp = api.evaluateJob(EvaluateJobRequest(job = jobData, profile = profile))
                    if (resp.success && resp.job != null) {
                        evaluatedJobs.add(Job.fromMap(resp.job))
                    } else {
                        evaluatedJobs.add(Job.fromMap(jobData))
                    }
                } catch (e: Exception) {
                    evaluatedJobs.add(Job.fromMap(jobData))
                }
            }

            Result.success(evaluatedJobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save a job to the user's saved jobs collection.
     */
    suspend fun saveJob(job: Job): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val docId = job.signature.replace(Regex("[^a-zA-Z0-9]"), "_")
            firestore.collection("users").document(uid)
                .collection("savedJobs")
                .document(docId)
                .set(job.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Unsave a job (remove from user's saved jobs).
     */
    suspend fun unsaveJob(job: Job): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val docId = job.signature.replace(Regex("[^a-zA-Z0-9]"), "_")
            firestore.collection("users").document(uid)
                .collection("savedJobs")
                .document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe the user's saved jobs.
     */
    fun observeSavedJobs(): kotlinx.coroutines.flow.Flow<List<Job>> {
        return kotlinx.coroutines.flow.callbackFlow {
            var firestoreListener: com.google.firebase.firestore.ListenerRegistration? = null
            
            val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                val uid = firebaseAuth.currentUser?.uid
                if (uid == null) {
                    firestoreListener?.remove()
                    firestoreListener = null
                    trySend(emptyList())
                } else {
                    if (firestoreListener == null) {
                        firestoreListener = firestore.collection("users").document(uid)
                            .collection("savedJobs")
                            .addSnapshotListener { snapshot, error ->
                                if (error != null) {
                                    close(error)
                                    return@addSnapshotListener
                                }
                                if (snapshot != null) {
                                    val jobs = snapshot.documents.mapNotNull {
                                        val map = it.data ?: return@mapNotNull null
                                        Job.fromMap(map)
                                    }
                                    trySend(jobs)
                                }
                            }
                    }
                }
            }
            
            auth.addAuthStateListener(authListener)
            awaitClose {
                auth.removeAuthStateListener(authListener)
                firestoreListener?.remove()
            }
        }
    }

    /**
     * Analyze a job description for resume tailoring using Vercel API.
     */
    suspend fun analyzeJobDescription(jobDescription: String): Result<Map<String, Any?>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val userDoc = firestore.collection("users").document(uid).collection("profile").document("master").get().await()
            val profile = if (userDoc.exists() && userDoc.data != null) {
                userDoc.data!!
            } else {
                return Result.failure(Exception("Profile not found"))
            }
            
            val response = api.analyzeResume(AnalyzeResumeRequest(job_description = jobDescription, uid = uid, profile = profile))
            if (response.success) {
                val analysisResult = mapOf(
                    "matchRate" to response.match_rate,
                    "keywordsToInclude" to response.keywords_to_include,
                    "missingKeywords" to response.missing_keywords,
                    "matchedSkills" to response.matched_skills,
                    "tailoredData" to response.tailored_data,
                    "coverLetterText" to response.cover_letter_text
                )
                Result.success(analysisResult)
            } else {
                Result.failure(Exception("Failed to analyze resume"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a tailored resume PDF using Vercel API.
     * Returns the binary stream which can be saved directly to the device.
     */
    suspend fun generateResumePdf(tailoredData: Map<String, Any?>): Result<ResponseBody> {
        return try {
            val responseBody = api.generatePdf(GeneratePdfRequest(tailored_data = tailoredData))
            Result.success(responseBody)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
