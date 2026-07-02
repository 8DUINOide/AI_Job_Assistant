package com.aijobassistant.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijobassistant.app.model.UserProfile
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Profile screen — view/edit the master profile.
 * Converts the web dashboard's "Master Profile Vault" card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile = UserProfile(),
    onSignOut: () -> Unit = {},
    onRebuildProfile: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your master profile powers AI job matching",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
        )

        // Profile card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            // Avatar + name
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profile.isFromResume) {
                    SkillTag(
                        text = "Data Extracted from Resume",
                        containerColor = StatusSuccessContainer,
                        textColor = StatusSuccess,
                        borderColor = StatusSuccess.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    SkillTag(
                        text = "Basic Profile",
                        containerColor = StatusWarningContainer,
                        textColor = StatusWarning,
                        borderColor = StatusWarning.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                // Avatar circle with initials
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = PrimaryBlueContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = "${profile.personalInfo.firstName.firstOrNull() ?: ""}${profile.personalInfo.lastName.firstOrNull() ?: ""}"
                        Text(
                            text = initials.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = profile.personalInfo.fullName.ifBlank { "Set up your profile" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (profile.personalInfo.email.isNotBlank()) {
                    Text(
                        text = profile.personalInfo.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                if (profile.personalInfo.location.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profile.personalInfo.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Roles
        if (profile.jobPreferences.desiredRoles.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎯 Target Roles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.jobPreferences.desiredRoles.forEach { role ->
                        SkillTag(text = role)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Skills
        if (profile.skills.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💻 Skills",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    profile.skills.forEach { skill ->
                        SkillTag(
                            text = skill,
                            containerColor = AccentIndigoContainer,
                            textColor = AccentIndigoLight,
                            borderColor = AccentIndigoLight.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Experience
        if (profile.experience.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💼 Experience",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                profile.experience.forEach { exp ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = exp.company,
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentIndigoLight
                            )
                        }
                        Text(
                            text = "${exp.startDate} - ${exp.endDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    if (profile.experience.last() != exp) {
                        HorizontalDivider(color = BorderColor)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Education
        if (profile.education.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "🎓 Education",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                profile.education.forEach { edu ->
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = edu.university,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = edu.degree,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        Text(
                            text = edu.graduationYear,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Certifications
        if (profile.certifications.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "📜 Certifications",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                profile.certifications.forEach { cert ->
                    Text(
                        text = "• $cert",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Actions
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "⚙️ Account",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Rebuild profile
            GradientButton(
                text = "🔄  Rebuild Profile from Resume",
                onClick = onRebuildProfile,
                gradientColors = listOf(AccentIndigo, PrimaryBlue)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign out
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete account
            TextButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Delete Account",
                    color = StatusDanger,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Delete account dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account?", color = StatusDanger) },
            text = {
                Text(
                    "This will permanently delete your account and all data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAccount()
                    showDeleteDialog = false
                }) {
                    Text("Delete Forever", color = StatusDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBackground
        )
    }
}
