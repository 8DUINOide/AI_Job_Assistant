package com.aijobassistant.app.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

data class JobSearchRequest(
    val search_keyword: String,
    val offset: Int = 0,
    val uid: String
)

data class JobSearchResponse(
    val success: Boolean,
    val profile: Map<String, Any>?,
    val jobs: List<Map<String, Any?>>?
)

data class EvaluateJobRequest(
    val job: Map<String, Any?>,
    val profile: Map<String, Any?>
)

data class EvaluateJobResponse(
    val success: Boolean,
    val job: Map<String, Any?>?
)

data class AnalyzeResumeRequest(
    val job_description: String,
    val uid: String
)

data class AnalyzeResumeResponse(
    val success: Boolean,
    val match_rate: Int,
    val keywords_to_include: List<String>,
    val missing_keywords: List<String>,
    val matched_skills: List<String>,
    val tailored_data: Map<String, Any?>,
    val cover_letter_text: String
)

data class GeneratePdfRequest(
    val tailored_data: Map<String, Any?>
)

data class BuildProfileResponse(
    val success: Boolean,
    val profile: Map<String, Any>?,
    val error: String?
)

/**
 * Retrofit interface for the Vercel Python API.
 */
interface ApiService {
    
    @POST("api/run-agent-manually")
    suspend fun searchJobs(@Body request: JobSearchRequest): JobSearchResponse
    
    @POST("api/evaluate-job")
    suspend fun evaluateJob(@Body request: EvaluateJobRequest): EvaluateJobResponse
    
    @POST("api/analyze-resume")
    suspend fun analyzeResume(@Body request: AnalyzeResumeRequest): AnalyzeResumeResponse
    
    @POST("api/generate-pdf")
    suspend fun generatePdf(@Body request: GeneratePdfRequest): ResponseBody
    
    @Multipart
    @POST("api/build-profile")
    suspend fun buildProfile(
        @Part file: MultipartBody.Part,
        @Part("portfolio_url") portfolioUrl: RequestBody
    ): BuildProfileResponse
}
