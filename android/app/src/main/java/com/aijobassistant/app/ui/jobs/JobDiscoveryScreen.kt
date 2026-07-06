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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
    savedJobs: List<Job> = emptyList(),
    isSearching: Boolean = false,
    searchProgress: String = "",
    error: String? = null,
    onSearch: (keyword: String, location: String) -> Unit = { _, _ -> },
    onJobClick: (Job) -> Unit = {},
    onSaveJob: (Job) -> Unit = {},
    onUnsaveJob: (Job) -> Unit = {},
    onTailorResume: (Job) -> Unit = {}
) {
    var searchKeyword by remember { mutableStateOf("") }
    var locationFilter by remember { mutableStateOf("Remote") }
    var showLocationDropdown by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Discover, 1 = Saved

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
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        
        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusDanger.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = error,
                    color = StatusDanger,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            contentColor = PrimaryBlue,
            divider = {},
            indicator = { tabPositions ->
                if (activeTab < tabPositions.size) {
                    androidx.compose.material3.TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = PrimaryBlue
                    )
                }
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Discover", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                unselectedContentColor = TextMuted
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Saved", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
                unselectedContentColor = TextMuted
            )
        }

        if (activeTab == 0) {
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
        
        // End of first activeTab == 0 block
        }

        if (activeTab == 0) {
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
                    val isSaved = savedJobs.any { it.signature == job.signature }
                    JobCard(
                        job = job,
                        isSaved = isSaved,
                        onClick = { onJobClick(job) },
                        onSave = { onSaveJob(job) },
                        onUnsave = { onUnsaveJob(job) },
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
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(64.dp),
                            tint = PrimaryBlue
                        )
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
        } else {
            // Saved Jobs Tab
            if (savedJobs.isNotEmpty()) {
                Text(
                    text = "${savedJobs.size} saved jobs",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(savedJobs, key = { it.signature }) { job ->
                        JobCard(
                            job = job,
                            isSaved = true,
                            onClick = { onJobClick(job) },
                            onSave = { },
                            onUnsave = { onUnsaveJob(job) },
                            onTailorResume = { onTailorResume(job) }
                        )
                    }
                    // Bottom padding for nav bar
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = "Saved",
                            modifier = Modifier.size(64.dp),
                            tint = TextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No saved jobs yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: Job,
    isSaved: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
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
        // Top row: posted time, flag, menu
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(StatusDanger)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = job.postedAt,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.countryFlag, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

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
                    fontWeight = FontWeight.SemiBold
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

        // Platform & Link
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(job.platform, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
            }
            if (job.link.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Job", style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSaved) {
                OutlinedButton(
                    onClick = { onUnsave() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Saved", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                OutlinedButton(
                    onClick = { onSave() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", style = MaterialTheme.typography.labelSmall)
                }
            }
            Button(
                onClick = onTailorResume,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tailor Resume", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}
