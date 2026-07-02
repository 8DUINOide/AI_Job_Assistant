package com.aijobassistant.app.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijobassistant.app.model.Application
import com.aijobassistant.app.model.ApplicationStatus
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Home Dashboard screen — the main hub after login.
 * Converts the web dashboard's "Agent Command Center" to mobile.
 */
@Composable
fun HomeScreen(
    userName: String = "User",
    totalApplications: Int = 0,
    pendingCount: Int = 0,
    appliedCount: Int = 0,
    rejectedCount: Int = 0,
    pendingJobs: List<Application> = emptyList(),
    recentApplications: List<Application> = emptyList(),
    onNavigateToJobs: () -> Unit = {},
    onNavigateToResume: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onApproveJob: (Application) -> Unit = {},
    onDenyJob: (Application) -> Unit = {},
    onTriggerAgent: suspend (log: (String) -> Unit) -> Unit = {}
) {
    var isAgentRunning by remember { mutableStateOf(false) }
    var agentLogs by remember { mutableStateOf(listOf<String>()) }
    var displayPendingJobs by remember(pendingJobs) { mutableStateOf(pendingJobs) }
    val timeFormat = remember { java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()) }
    val currentTime: () -> String = { timeFormat.format(java.util.Date()) }

    LaunchedEffect(isAgentRunning) {
        if (isAgentRunning) {
            agentLogs = listOf("[${currentTime()}] Agent manual override initiated.")
            try {
                // Execute actual logic, passing a logger lambda
                onTriggerAgent { logMessage ->
                    agentLogs = agentLogs + "[${currentTime()}] $logMessage"
                }
                agentLogs = agentLogs + "[${currentTime()}] Agent run complete."
            } catch (e: Exception) {
                agentLogs = agentLogs + "[${currentTime()}] Error: ${e.localizedMessage}"
            } finally {
                kotlinx.coroutines.delay(3000)
                isAgentRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Greeting header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Welcome back,",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Stats Overview Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Apps",
                count = totalApplications,
                icon = Icons.Default.Work,
                color = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Pending",
                count = pendingCount + if(displayPendingJobs.size > pendingJobs.size) 1 else 0,
                icon = Icons.Default.HourglassEmpty,
                color = StatusPending,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Applied",
                count = appliedCount,
                icon = Icons.Default.CheckCircle,
                color = StatusSuccess,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Rejected",
                count = rejectedCount,
                icon = Icons.Default.Cancel,
                color = StatusDanger,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Search,
                title = "Find Jobs",
                subtitle = "AI-powered search",
                gradientColors = listOf(PrimaryBlue, PrimaryBlueDark),
                onClick = onNavigateToJobs,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            QuickActionCard(
                icon = Icons.Default.Description,
                title = "Tailor Resume",
                subtitle = "For a specific job",
                gradientColors = listOf(AccentIndigo, AccentIndigoLight),
                onClick = onNavigateToResume,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            QuickActionCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Track",
                subtitle = "Applications",
                gradientColors = listOf(StatusSuccess, Color(0xFF059669)),
                onClick = onNavigateToTracker,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Pending Approvals (if any)
        if (displayPendingJobs.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    StatusDot(color = StatusPending, animate = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Action Required: ${displayPendingJobs.size} Pending",
                        style = MaterialTheme.typography.titleSmall,
                        color = StatusPending,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                displayPendingJobs.take(3).forEach { job ->
                    PendingJobCard(
                        application = job,
                        onApprove = { 
                            val updatedList = displayPendingJobs.filter { it.id != job.id }
                            displayPendingJobs = updatedList
                            onApproveJob(job) 
                        },
                        onDeny = { 
                            val updatedList = displayPendingJobs.filter { it.id != job.id }
                            displayPendingJobs = updatedList
                            onDenyJob(job) 
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (displayPendingJobs.size > 3) {
                    TextButton(
                        onClick = onNavigateToTracker,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "View All ${displayPendingJobs.size} Pending →",
                            color = AccentIndigoLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // AI Agent Card
        GlassCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                StatusDot(color = StatusSuccess, animate = true)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI Agent",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "The AI agent scans for jobs matching your profile daily and sends you notifications when high-match jobs are found.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            GradientButton(
                text = if (isAgentRunning) "⏳ Agent is Running..." else "🔍  Trigger Agent Now",
                onClick = { 
                    if (!isAgentRunning) {
                        isAgentRunning = true
                    }
                },
                gradientColors = if (isAgentRunning) listOf(Color.Gray, Color.DarkGray) else listOf(PrimaryBlue, AccentIndigo)
            )

            if (isAgentRunning || agentLogs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x80000000))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        agentLogs.forEach { log ->
                            val color = when {
                                log.contains("MATCH") || log.contains("successfully") || log.contains("complete") -> StatusSuccess
                                log.contains("override") || log.contains("Searching") -> StatusWarning
                                else -> PrimaryBlueLight
                            }
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = color,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Applications
        if (recentApplications.isNotEmpty()) {
            Text(
                text = "Recent Applications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            recentApplications.take(5).forEach { app ->
                RecentApplicationRow(application = app)
                Spacer(modifier = Modifier.height(8.dp))
            }

            TextButton(
                onClick = onNavigateToTracker,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("View All Applications →", color = AccentIndigoLight)
            }
        }

        // Bottom spacing for nav bar
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(gradientColors.map { it.copy(alpha = 0.3f) }),
                    RoundedCornerShape(12.dp)
                )
                .padding(14.dp)
        ) {
            Column {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PendingJobCard(
    application: Application,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x0DFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = application.jobTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${application.company} • ${application.dateApplied}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusSuccess,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Applied", style = MaterialTheme.typography.labelSmall)
                }
                Button(
                    onClick = onDeny,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusDanger,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Denied", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun RecentApplicationRow(application: Application) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundTranslucent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when (application.status) {
                    ApplicationStatus.APPLIED -> StatusSuccessContainer
                    ApplicationStatus.REJECTED -> StatusDangerContainer
                    ApplicationStatus.DENIED -> StatusWarningContainer
                    ApplicationStatus.INTERVIEW -> AccentIndigoContainer
                    ApplicationStatus.OFFER -> StatusSuccessContainer
                    else -> PrimaryBlueContainer
                }
            ) {
                Text(
                    text = application.status.displayName,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (application.status) {
                        ApplicationStatus.APPLIED -> StatusSuccess
                        ApplicationStatus.REJECTED -> StatusDanger
                        ApplicationStatus.DENIED -> StatusWarning
                        ApplicationStatus.INTERVIEW -> AccentIndigoLight
                        ApplicationStatus.OFFER -> StatusSuccess
                        else -> PrimaryBlueLight
                    }
                )
            }
        }
    }
}
