package com.aijobassistant.app.ui.jobs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.model.Job
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Job Discovery screen — search and find matching jobs.
 * Converts the web dashboard's "Trigger Agent" + results flow.
 */
@Composable
fun JobDiscoveryScreen(
    jobs: List<Job> = emptyList(),
    isSearching: Boolean = false,
    searchProgress: String = "",
    onSearch: (keyword: String, location: String) -> Unit = { _, _ -> },
    onJobClick: (Job) -> Unit = {},
    onSaveJob: (Job) -> Unit = {},
    onTailorResume: (Job) -> Unit = {}
) {
    var searchKeyword by remember { mutableStateOf("") }
    var locationFilter by remember { mutableStateOf("Remote") }
    var showLocationDropdown by remember { mutableStateOf(false) }

    val locationOptions = listOf("Remote", "Philippines", "Manila", "Makati", "Taguig", "Quezon City")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Job Discovery",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI-powered job search across LinkedIn & Indeed",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Search bar
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = searchKeyword,
                onValueChange = { searchKeyword = it },
                label = "Job title or keywords",
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { showLocationDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(locationFilter, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
                    }
                    DropdownMenu(
                        expanded = showLocationDropdown,
                        onDismissRequest = { showLocationDropdown = false }
                    ) {
                        locationOptions.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    locationFilter = loc
                                    showLocationDropdown = false
                                }
                            )
                        }
                    }
                }

                // Search button
                Button(
                    onClick = {
                        onSearch(
                            searchKeyword.ifBlank { "Software Engineer" },
                            locationFilter
                        )
                    },
                    enabled = !isSearching,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.height(48.dp)
                ) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            }
        }

        // Search progress
        AnimatedVisibility(visible = isSearching) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = AccentIndigoContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentIndigoLight,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = searchProgress.ifBlank { "Searching across job boards..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentIndigoLight
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Results count
        if (jobs.isNotEmpty() && !isSearching) {
            Text(
                text = "${jobs.size} jobs found",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Job cards list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(jobs, key = { it.signature }) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job) },
                    onSave = { onSaveJob(job) },
                    onTailorResume = { onTailorResume(job) }
                )
            }

            // Bottom padding for nav bar
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Empty state
        if (jobs.isEmpty() && !isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔍", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Search for jobs to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        "AI will score each job based on your profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: Job,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onTailorResume: () -> Unit
) {
    val scoreColor = when {
        job.score >= 70 -> StatusSuccess
        job.score >= 50 -> StatusWarning
        job.score > 0 -> StatusDanger
        else -> TextMuted
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Header row: title + score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = job.company,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentIndigoLight
                )
            }

            // Score badge
            if (job.score > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = scoreColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${job.score}%",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Location & salary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.location.ifBlank { "Unknown" }, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            if (job.salary.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(job.salary, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
            }
        }

        // Tech stack tags
        if (job.techStack.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                job.techStack.split(",").take(4).forEach { skill ->
                    SkillTag(text = skill.trim())
                }
            }
        }

        // Match reason
        if (job.reason.isNotBlank() && job.score > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = job.reason,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = onTailorResume,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tailor Resume", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
