package com.aijobassistant.app.model

/**
 * Mirrors the master_profile.json structure from the original Python app.
 * Stored in Firestore at: users/{uid}/profile
 */
data class UserProfile(
    val personalInfo: PersonalInfo = PersonalInfo(),
    val jobPreferences: JobPreferences = JobPreferences(),
    val summary: String = "",
    val experience: List<Experience> = emptyList(),
    val education: List<Education> = emptyList(),
    val projects: List<Project> = emptyList(),
    val skills: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val awards: List<String> = emptyList(),
    val isFromResume: Boolean = false
) {
    /** Convert to Firestore-compatible map */
    fun toMap(): Map<String, Any?> = mapOf(
        "personalInfo" to personalInfo.toMap(),
        "jobPreferences" to jobPreferences.toMap(),
        "summary" to summary,
        "experience" to experience.map { it.toMap() },
        "education" to education.map { it.toMap() },
        "projects" to projects.map { it.toMap() },
        "skills" to skills,
        "certifications" to certifications,
        "awards" to awards,
        "isFromResume" to isFromResume
    )

    companion object {
        /** Create from Firestore document snapshot */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                personalInfo = PersonalInfo.fromMap(map["personalInfo"] as? Map<String, Any?> ?: emptyMap()),
                jobPreferences = JobPreferences.fromMap(map["jobPreferences"] as? Map<String, Any?> ?: emptyMap()),
                summary = map["summary"] as? String ?: "",
                experience = (map["experience"] as? List<Map<String, Any?>>)?.map { Experience.fromMap(it) } ?: emptyList(),
                education = (map["education"] as? List<Map<String, Any?>>)?.map { Education.fromMap(it) } ?: emptyList(),
                projects = (map["projects"] as? List<Map<String, Any?>>)?.map { Project.fromMap(it) } ?: emptyList(),
                skills = (map["skills"] as? List<String>) ?: emptyList(),
                certifications = (map["certifications"] as? List<String>) ?: emptyList(),
                awards = (map["awards"] as? List<String>) ?: emptyList(),
                isFromResume = map["isFromResume"] as? Boolean ?: false
            )
        }
    }
}

data class PersonalInfo(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedinUrl: String = "",
    val portfolioUrl: String = ""
) {
    val fullName: String get() = "$firstName $lastName".trim()

    fun toMap(): Map<String, Any?> = mapOf(
        "firstName" to firstName,
        "lastName" to lastName,
        "email" to email,
        "phone" to phone,
        "location" to location,
        "linkedinUrl" to linkedinUrl,
        "portfolioUrl" to portfolioUrl
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): PersonalInfo = PersonalInfo(
            firstName = map["firstName"] as? String ?: map["first_name"] as? String ?: "",
            lastName = map["lastName"] as? String ?: map["last_name"] as? String ?: "",
            email = map["email"] as? String ?: "",
            phone = map["phone"] as? String ?: "",
            location = map["location"] as? String ?: "",
            linkedinUrl = map["linkedinUrl"] as? String ?: map["linkedin_url"] as? String ?: "",
            portfolioUrl = map["portfolioUrl"] as? String ?: map["portfolio_url"] as? String ?: ""
        )
    }
}

data class JobPreferences(
    val desiredRoles: List<String> = emptyList(),
    val workType: List<String> = listOf("Remote", "Hybrid", "On-site"),
    val locations: List<String> = listOf("Philippines", "Remote"),
    val salaryExpectation: String = "Open to discussion"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "desiredRoles" to desiredRoles,
        "workType" to workType,
        "locations" to locations,
        "salaryExpectation" to salaryExpectation
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): JobPreferences = JobPreferences(
            desiredRoles = (map["desiredRoles"] as? List<String>) ?: (map["desired_roles"] as? List<String>) ?: emptyList(),
            workType = (map["workType"] as? List<String>) ?: (map["work_type"] as? List<String>) ?: listOf("Remote"),
            locations = (map["locations"] as? List<String>) ?: listOf("Remote"),
            salaryExpectation = map["salaryExpectation"] as? String ?: map["salary_expectation"] as? String ?: ""
        )
    }
}

data class Experience(
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = "",
    val skillsUsed: List<String> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "company" to company,
        "location" to location,
        "startDate" to startDate,
        "endDate" to endDate,
        "description" to description,
        "skillsUsed" to skillsUsed
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): Experience = Experience(
            title = map["title"] as? String ?: "",
            company = map["company"] as? String ?: "",
            location = map["location"] as? String ?: "",
            startDate = map["startDate"] as? String ?: map["start_date"] as? String ?: "",
            endDate = map["endDate"] as? String ?: map["end_date"] as? String ?: "",
            description = map["description"] as? String ?: "",
            skillsUsed = (map["skillsUsed"] as? List<String>) ?: (map["skills_used"] as? List<String>) ?: emptyList()
        )
    }
}

data class Education(
    val degree: String = "",
    val university: String = "",
    val graduationYear: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "degree" to degree,
        "university" to university,
        "graduationYear" to graduationYear
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Education = Education(
            degree = map["degree"] as? String ?: "",
            university = map["university"] as? String ?: "",
            graduationYear = map["graduationYear"] as? String ?: map["graduation_year"] as? String ?: ""
        )
    }
}

data class Project(
    val title: String = "",
    val role: String = "",
    val link: String = "",
    val description: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "role" to role,
        "link" to link,
        "description" to description
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Project = Project(
            title = map["title"] as? String ?: "",
            role = map["role"] as? String ?: "",
            link = map["link"] as? String ?: "",
            description = map["description"] as? String ?: ""
        )
    }
}
