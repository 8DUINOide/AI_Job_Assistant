package com.aijobassistant.app.model

import com.google.firebase.Timestamp

/**
 * Represents a tracked job application.
 * Stored in Firestore at: users/{uid}/applications/{docId}
 * Replaces the Google Sheets tracker from the original Python app.
 */
data class Application(
    val id: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val techStack: String = "",
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val dateApplied: String = "",
    val jobLink: String = "",
    val location: String = "",
    val salary: String = "",
    val contactPerson: String = "",
    val createdAt: Timestamp? = null
) {
    /** Unique signature for deduplication */
    val signature: String get() = "${company.lowercase()}|${jobTitle.lowercase()}"

    fun toMap(): Map<String, Any?> = mapOf(
        "company" to company,
        "jobTitle" to jobTitle,
        "techStack" to techStack,
        "status" to status.value,
        "dateApplied" to dateApplied,
        "jobLink" to jobLink,
        "location" to location,
        "salary" to salary,
        "contactPerson" to contactPerson,
        "createdAt" to (createdAt ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): Application = Application(
            id = id,
            company = map["company"] as? String ?: "",
            jobTitle = map["jobTitle"] as? String ?: map["job_title"] as? String ?: "",
            techStack = map["techStack"] as? String ?: map["tech_stack"] as? String ?: "",
            status = ApplicationStatus.fromValue(map["status"] as? String ?: "Pending"),
            dateApplied = map["dateApplied"] as? String ?: map["date_applied"] as? String ?: "",
            jobLink = map["jobLink"] as? String ?: map["job_link"] as? String ?: "",
            location = map["location"] as? String ?: "",
            salary = map["salary"] as? String ?: "",
            contactPerson = map["contactPerson"] as? String ?: map["contact_person"] as? String ?: "",
            createdAt = map["createdAt"] as? Timestamp
        )
    }
}

enum class ApplicationStatus(val value: String, val displayName: String) {
    PENDING("Pending", "Pending"),
    APPLIED("Applied", "Applied"),
    INTERVIEW("Interview", "Interview"),
    OFFER("Offer", "Offer"),
    REJECTED("Rejected", "Rejected"),
    DENIED("Denied", "Denied"),
    WITHDRAWN("Withdrawn", "Withdrawn");

    companion object {
        fun fromValue(value: String): ApplicationStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) }
                ?: when {
                    value.contains("applied", ignoreCase = true) -> APPLIED
                    value.contains("rejected", ignoreCase = true) -> REJECTED
                    value.contains("denied", ignoreCase = true) -> DENIED
                    value.contains("did not proceed", ignoreCase = true) -> REJECTED
                    value.contains("interview", ignoreCase = true) -> INTERVIEW
                    value.contains("offer", ignoreCase = true) -> OFFER
                    else -> PENDING
                }
        }
    }
}
