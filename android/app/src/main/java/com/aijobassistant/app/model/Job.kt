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
    val isSaved: Boolean = false,
    val postedAt: String = "Posted 3 hours ago",
    val platform: String = "Indeed",
    val countryFlag: String = "🇺🇸"
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
        "isSaved" to isSaved,
        "postedAt" to postedAt,
        "platform" to platform,
        "countryFlag" to countryFlag
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Job {
            val parsedLocation = map["location"] as? String ?: ""
            val parsedTitle = map["title"] as? String ?: ""
            val searchStr = (parsedLocation + " " + parsedTitle).lowercase()
            
            val computedFlag = when {
                "\\\\b(philippines|manila|makati|taguig|quezon|pasig|alabang|bicol|mandaluyong|ortigas|cebu|ncr|muntinlupa|marikina|general trias|cavite|laguna|batangas|rizal|bulacan|pampanga|caloocan|valenzuela|navotas|malabon|san juan|pateros|paranaque|las pinas|antipolo)\\\\b".toRegex().containsMatchIn(searchStr) -> "🇵🇭"
                searchStr.contains("remote") || searchStr.contains("latam") || searchStr.contains("worldwide") -> "🌐"
                searchStr.contains("canada") || searchStr.contains("toronto") || searchStr.contains("vancouver") -> "🇨🇦"
                searchStr.contains("united kingdom") || "\\buk\\b".toRegex().containsMatchIn(searchStr) || searchStr.contains("london") -> "🇬🇧"
                searchStr.contains("united states") || "\\busa?\\b".toRegex().containsMatchIn(searchStr) || "\\b(ny|ca|tx|wa|fl|il|opt)\\b".toRegex().containsMatchIn(searchStr) -> "🇺🇸"
                searchStr.contains("australia") || searchStr.contains("sydney") || searchStr.contains("melbourne") -> "🇦🇺"
                searchStr.contains("india") || searchStr.contains("bangalore") || searchStr.contains("mumbai") -> "🇮🇳"
                searchStr.contains("germany") || searchStr.contains("berlin") -> "🇩🇪"
                else -> "🌐"
            }

            return Job(
                id = map["id"] as? String ?: "",
                title = parsedTitle,
                company = map["company"] as? String ?: "",
                location = parsedLocation,
                link = map["link"] as? String ?: "",
                description = map["description"] as? String ?: "",
                salary = map["salary"] as? String ?: "",
                contactPerson = map["contactPerson"] as? String ?: map["contact_person"] as? String ?: "",
                techStack = map["techStack"] as? String ?: map["tech_stack"] as? String ?: "",
                score = (map["score"] as? Number)?.toInt() ?: 0,
                reason = map["reason"] as? String ?: "",
                isSaved = map["isSaved"] as? Boolean ?: false,
                postedAt = map["postedAt"] as? String ?: map["posted_at"] as? String ?: "Posted 3 hours ago",
                platform = map["platform"] as? String ?: "Indeed",
                countryFlag = map["countryFlag"] as? String ?: map["country_flag"] as? String ?: computedFlag
            )
        }
    }
}
