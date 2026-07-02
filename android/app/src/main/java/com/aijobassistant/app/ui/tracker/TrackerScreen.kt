package com.aijobassistant.app.ui.tracker

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.model.Application
import com.aijobassistant.app.model.ApplicationStatus
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Application Tracker screen — replaces Google Sheets.
 * Full CRUD on job applications stored in Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    applications: List<Application> = emptyList(),
    isLoading: Boolean = false,
    onUpdateStatus: (Application, ApplicationStatus) -> Unit = { _, _ -> },
    onDeleteApplication: (Application) -> Unit = {},
    onOpenLink: (String) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    var showStatusDialog by remember { mutableStateOf<Application?>(null) }

    val filteredApps = if (selectedFilter != null) {
        applications.filter { it.status == selectedFilter }
    } else {
        applications
    }

    // Stats
    val statusCounts = applications.groupBy { it.status }.mapValues { it.value.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Application Tracker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${applications.size} total applications",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All (${applications.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryBlueContainer,
                    selectedLabelColor = PrimaryBlueLight
                )
            )
            FilterChip(
                selected = selectedFilter == ApplicationStatus.APPLIED,
                onClick = { selectedFilter = if (selectedFilter == ApplicationStatus.APPLIED) null else ApplicationStatus.APPLIED },
                label = { Text("Applied (${statusCounts[ApplicationStatus.APPLIED] ?: 0})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusSuccessContainer,
                    selectedLabelColor = StatusSuccess
                )
            )
            FilterChip(
                selected = selectedFilter == ApplicationStatus.PENDING,
                onClick = { selectedFilter = if (selectedFilter == ApplicationStatus.PENDING) null else ApplicationStatus.PENDING },
                label = { Text("Pending (${statusCounts[ApplicationStatus.PENDING] ?: 0})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StatusWarningContainer,
                    selectedLabelColor = StatusWarning
                )
            )
        }

        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }

        // Application list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(
                filteredApps,
                key = { _, app -> app.id.ifBlank { app.signature } }
            ) { index, application ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteApplication(application)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StatusDanger.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = StatusDanger
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true
                ) {
                    ApplicationRow(
                        index = index + 1,
                        application = application,
                        onClick = { showStatusDialog = application },
                        onOpenLink = onOpenLink
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Empty state
        if (filteredApps.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No applications yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        "Start searching for jobs to build your tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }

    // Status update dialog
    showStatusDialog?.let { app ->
        AlertDialog(
            onDismissRequest = { showStatusDialog = null },
            title = {
                Text(
                    "Update Status",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        "${app.jobTitle} at ${app.company}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    ApplicationStatus.entries.forEach { status ->
                        val isSelected = app.status == status
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryBlueContainer else android.graphics.Color.TRANSPARENT.let { androidx.compose.ui.graphics.Color.Transparent },
                            onClick = {
                                onUpdateStatus(app, status)
                                showStatusDialog = null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusDot(
                                    color = statusColor(status),
                                    size = 8.dp,
                                    animate = false
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    status.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) PrimaryBlueLight else TextSecondary
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = null }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = CardBackground,
            tonalElevation = 8.dp
        )
    }
}

@Composable
private fun ApplicationRow(
    index: Int,
    application: Application,
    onClick: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.width(24.dp)
            )

            // Job info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = application.company,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                if (application.dateApplied.isNotBlank()) {
                    Text(
                        text = application.dateApplied,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            // Status chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusColor(application.status).copy(alpha = 0.15f)
            ) {
                Text(
                    text = application.status.displayName,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor(application.status)
                )
            }

            // Link button
            if (application.jobLink.isNotBlank()) {
                IconButton(
                    onClick = { onOpenLink(application.jobLink) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open link",
                        tint = AccentIndigo,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun statusColor(status: ApplicationStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        ApplicationStatus.APPLIED -> StatusSuccess
        ApplicationStatus.REJECTED -> StatusDanger
        ApplicationStatus.DENIED -> StatusWarning
        ApplicationStatus.INTERVIEW -> AccentIndigoLight
        ApplicationStatus.OFFER -> StatusSuccess
        ApplicationStatus.PENDING -> StatusPending
        ApplicationStatus.WITHDRAWN -> TextMuted
    }
}
