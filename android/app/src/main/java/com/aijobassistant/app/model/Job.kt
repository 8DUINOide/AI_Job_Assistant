package com.aijobassistant.app.model

/**
 * Represents a scraped job listing from the job discovery feature.
 * Maps to the job objects returned by scraper.py
 */
data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val link: String = "",
    val description: String = "",
    val salary: String = "",
    val contactPerson: String = "",
    val techStack: String = "",
    val score: Int = 0,
    val reason: String = "",
    val isSaved: Boolean = false
) {
    /** Unique signature for deduplication (matches Python logic) */
    val signature: String get() = "${company.lowercase()}|${title.lowercase()}"

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "title" to title,
        "company" to company,
        "location" to location,
        "link" to link,
        "description" to description,
        "salary" to salary,
        "contactPerson" to contactPerson,
        "techStack" to techStack,
        "score" to score,
        "reason" to reason,
        "isSaved" to isSaved
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Job = Job(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            company = map["company"] as? String ?: "",
            location = map["location"] as? String ?: "",
            link = map["link"] as? String ?: "",
            description = map["description"] as? String ?: "",
            salary = map["salary"] as? String ?: "",
            contactPerson = map["contactPerson"] as? String ?: map["contact_person"] as? String ?: "",
            techStack = map["techStack"] as? String ?: map["tech_stack"] as? String ?: "",
            score = (map["score"] as? Number)?.toInt() ?: 0,
            reason = map["reason"] as? String ?: "",
            isSaved = map["isSaved"] as? Boolean ?: false
        )
    }
}
