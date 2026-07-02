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
    suspend fun searchJobs(keyword: String, location: String = "Remote", offset: Int = 0): Result<List<Job>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val response = api.searchJobs(JobSearchRequest(search_keyword = keyword, offset = offset, uid = uid))
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
            firestore.collection("users").document(uid)
                .collection("savedJobs")
                .document(job.signature.replace("|", "_"))
                .set(job.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Analyze a job description for resume tailoring using Vercel API.
     */
    suspend fun analyzeJobDescription(jobDescription: String): Result<Map<String, Any?>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val response = api.analyzeResume(AnalyzeResumeRequest(job_description = jobDescription, uid = uid))
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
