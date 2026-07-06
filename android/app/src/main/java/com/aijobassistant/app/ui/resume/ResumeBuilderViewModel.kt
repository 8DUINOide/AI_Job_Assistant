package com.aijobassistant.app.ui.resume

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aijobassistant.app.data.network.ApiClient
import com.aijobassistant.app.data.network.GenerateResumeFromTemplateRequest
import com.aijobassistant.app.data.profile.ProfileRepository
import com.aijobassistant.app.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// === Mutable state holders for each section ===

data class PersonalInfoState(
    var name: String = "",
    var location: String = "",
    var phone: String = "",
    var email: String = "",
    var linkedin: String = "",
    var github: String = ""
)

data class EducationItemState(
    var university: String = "",
    var location: String = "",
    var degree: String = "",
    var year: String = "",
    var details: String = "",
    var coursework: String = ""
)

data class ExperienceItemState(
    var company: String = "",
    var companyNote: String = "",
    var location: String = "",
    var title: String = "",
    var dateRange: String = "",
    var bullets: String = "" // Newline-separated
)

data class ProjectItemState(
    var name: String = "",
    var year: String = "",
    var bullets: String = ""
)

data class ActivityItemState(
    var organization: String = "",
    var location: String = "",
    var role: String = "",
    var dateRange: String = "",
    var bullets: String = ""
)

data class AdditionalState(
    var technicalSkills: String = "",
    var certifications: String = "",
    var languages: String = ""
)

data class ResumeBuilderState(
    val personalInfo: PersonalInfoState = PersonalInfoState(),
    val education: MutableList<EducationItemState> = mutableListOf(),
    val experience: MutableList<ExperienceItemState> = mutableListOf(),
    val projects: MutableList<ProjectItemState> = mutableListOf(),
    val activities: MutableList<ActivityItemState> = mutableListOf(),
    val additional: AdditionalState = AdditionalState(),
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ResumeBuilderViewModel : ViewModel() {
    private val _state = MutableStateFlow(ResumeBuilderState())
    val state: StateFlow<ResumeBuilderState> = _state.asStateFlow()

    private val profileRepository = ProfileRepository()
    private val api = ApiClient.apiService

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            val result = profileRepository.getProfile()
            if (result.isSuccess) {
                val profile = result.getOrNull()
                if (profile != null) {
                    mapProfileToState(profile)
                }
            } else {
                _state.update { it.copy(error = "Failed to load profile. Upload a resume first.") }
            }
            
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun mapProfileToState(profile: UserProfile) {
        val pi = PersonalInfoState(
            name = profile.personalInfo.fullName,
            location = profile.personalInfo.location,
            phone = profile.personalInfo.phone,
            email = profile.personalInfo.email,
            linkedin = profile.personalInfo.linkedinUrl,
            github = profile.personalInfo.portfolioUrl
        )

        val eduList = profile.education.map { edu ->
            EducationItemState(
                university = edu.university,
                location = "",
                degree = edu.degree,
                year = edu.graduationYear,
                details = "",
                coursework = ""
            )
        }.toMutableList()

        val expList = profile.experience.map { exp ->
            val start = exp.startDate
            val end = exp.endDate
            val dateRange = if (start.isNotBlank() && end.isNotBlank()) "$start – $end" 
                           else start.ifBlank { end }
            ExperienceItemState(
                company = exp.company,
                companyNote = "",
                location = exp.location,
                title = exp.title,
                dateRange = dateRange,
                bullets = exp.description
            )
        }.toMutableList()

        val projList = profile.projects.map { proj ->
            ProjectItemState(
                name = proj.title,
                year = "",
                bullets = proj.description
            )
        }.toMutableList()

        val actList = profile.activities.map { act ->
            val start = act.startDate
            val end = act.endDate
            val dateRange = if (start.isNotBlank() && end.isNotBlank()) "$start – $end"
                           else start.ifBlank { end }
            ActivityItemState(
                organization = act.organization,
                location = act.location,
                role = act.role,
                dateRange = dateRange,
                bullets = act.description
            )
        }.toMutableList()

        val additional = AdditionalState(
            technicalSkills = profile.skills.joinToString(", "),
            certifications = profile.certifications.joinToString(", "),
            languages = ""
        )

        _state.update {
            it.copy(
                personalInfo = pi,
                education = eduList,
                experience = expList,
                projects = projList,
                activities = actList,
                additional = additional
            )
        }
    }

    /** Build the profile_data map that the backend expects */
    private fun buildProfileDataMap(): Map<String, Any?> {
        val s = _state.value
        return mapOf(
            "personal_info" to mapOf(
                "name" to s.personalInfo.name,
                "location" to s.personalInfo.location,
                "phone" to s.personalInfo.phone,
                "email" to s.personalInfo.email,
                "linkedin" to s.personalInfo.linkedin,
                "github" to s.personalInfo.github
            ),
            "education" to s.education.map { edu ->
                mapOf(
                    "university" to edu.university,
                    "location" to edu.location,
                    "degree" to edu.degree,
                    "year" to edu.year,
                    "details" to edu.details,
                    "coursework" to edu.coursework
                )
            },
            "experience" to s.experience.map { exp ->
                mapOf(
                    "company" to exp.company,
                    "company_note" to exp.companyNote,
                    "location" to exp.location,
                    "title" to exp.title,
                    "date_range" to exp.dateRange,
                    "bullets" to exp.bullets.split("\n").filter { it.isNotBlank() }
                )
            },
            "projects" to s.projects.map { proj ->
                mapOf(
                    "name" to proj.name,
                    "year" to proj.year,
                    "bullets" to proj.bullets.split("\n").filter { it.isNotBlank() }
                )
            },
            "activities" to s.activities.map { act ->
                mapOf(
                    "organization" to act.organization,
                    "location" to act.location,
                    "role" to act.role,
                    "date_range" to act.dateRange,
                    "bullets" to act.bullets.split("\n").filter { it.isNotBlank() }
                )
            },
            "additional" to mapOf(
                "technical_skills" to s.additional.technicalSkills,
                "certifications" to s.additional.certifications,
                "languages" to s.additional.languages
            )
        )
    }

    fun generatePdf(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null, successMessage = null) }
            try {
                val profileData = buildProfileDataMap()
                val response = api.generateResumeTemplatePdf(
                    GenerateResumeFromTemplateRequest(profile_data = profileData)
                )
                
                val fileName = "${_state.value.personalInfo.name.ifBlank { "Resume" }}_Resume.pdf"
                saveFileToDownloads(context, response.bytes(), fileName)
                
                _state.update { it.copy(isGenerating = false, successMessage = "PDF saved to Downloads/$fileName") }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = "Failed to generate PDF: ${e.message}") }
            }
        }
    }

    fun generateDocx(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null, successMessage = null) }
            try {
                val profileData = buildProfileDataMap()
                val response = api.generateResumeDocx(
                    GenerateResumeFromTemplateRequest(profile_data = profileData)
                )
                
                val fileName = "${_state.value.personalInfo.name.ifBlank { "Resume" }}_Resume.docx"
                saveFileToDownloads(context, response.bytes(), fileName)
                
                _state.update { it.copy(isGenerating = false, successMessage = "DOCX saved to Downloads/$fileName") }
            } catch (e: Exception) {
                _state.update { it.copy(isGenerating = false, error = "Failed to generate DOCX: ${e.message}") }
            }
        }
    }

    private fun saveFileToDownloads(context: Context, bytes: ByteArray, fileName: String) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { fos ->
            fos.write(bytes)
        }
    }

    // === Section Add/Remove helpers ===
    
    fun addEducation() {
        _state.update {
            val newList = it.education.toMutableList()
            newList.add(EducationItemState())
            it.copy(education = newList)
        }
    }

    fun removeEducation(index: Int) {
        _state.update {
            val newList = it.education.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            it.copy(education = newList)
        }
    }

    fun addExperience() {
        _state.update {
            val newList = it.experience.toMutableList()
            newList.add(ExperienceItemState())
            it.copy(experience = newList)
        }
    }

    fun removeExperience(index: Int) {
        _state.update {
            val newList = it.experience.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            it.copy(experience = newList)
        }
    }

    fun addProject() {
        _state.update {
            val newList = it.projects.toMutableList()
            newList.add(ProjectItemState())
            it.copy(projects = newList)
        }
    }

    fun removeProject(index: Int) {
        _state.update {
            val newList = it.projects.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            it.copy(projects = newList)
        }
    }

    fun addActivity() {
        _state.update {
            val newList = it.activities.toMutableList()
            newList.add(ActivityItemState())
            it.copy(activities = newList)
        }
    }

    fun removeActivity(index: Int) {
        _state.update {
            val newList = it.activities.toMutableList()
            if (index in newList.indices) newList.removeAt(index)
            it.copy(activities = newList)
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }

    /** Force a recomposition by updating state with the same reference */
    fun notifyFieldChanged() {
        _state.update { it.copy() }
    }
}
