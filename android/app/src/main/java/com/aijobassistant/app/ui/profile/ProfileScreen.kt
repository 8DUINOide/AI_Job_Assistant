package com.aijobassistant.app.ui.profile

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile = UserProfile(),
    onUpdateDesiredRoles: (List<String>) -> Unit = {},
    onSignOut: () -> Unit = {},
    onRebuildProfile: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditRolesDialog by remember { mutableStateOf(false) }
    var editRolesText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Master Profile Vault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = "The agent uses this context to evaluate and apply for jobs.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Profile card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    color = PrimaryBlueContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val initials = "${profile.personalInfo.firstName.take(1)}${profile.personalInfo.lastName.take(1)}"
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
                    text = "Candidate",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = profile.personalInfo.fullName.ifBlank { "Set up your profile" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Roles
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrackChanges, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Target Roles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(
                    onClick = { 
                        editRolesText = profile.jobPreferences.desiredRoles.joinToString(", ")
                        showEditRolesDialog = true 
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Roles",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                profile.jobPreferences.desiredRoles.forEach { role ->
                    SkillTag(text = role)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Skills
        if (profile.skills.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Skills",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                FlowRow(
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

        // Actions
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Rebuild profile
            GradientButton(
                text = "Rebuild Profile from Resume",
                icon = Icons.Default.Refresh,
                onClick = onRebuildProfile
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

    // Edit Roles dialog
    if (showEditRolesDialog) {
        AlertDialog(
            onDismissRequest = { showEditRolesDialog = false },
            title = { Text("Edit Target Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Specify the roles you are looking for (e.g., Software Engineer, Backend Developer). Separate multiple roles with commas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = editRolesText,
                        onValueChange = { editRolesText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = false,
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Suggested Roles (Max 5)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val suggestedRoles = listOf(
                        "Software Engineer", "Backend Developer", "Frontend Developer", 
                        "Full Stack Developer", "Android Developer", "Data Scientist", 
                        "DevOps Engineer", "UI/UX Designer"
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedRoles.forEach { role ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CardBackground,
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.linearGradient(listOf(BorderColor, BorderColor))
                                ),
                                modifier = Modifier.clickable {
                                    val currentRoles = editRolesText.split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    
                                    if (!currentRoles.contains(role) && currentRoles.size < 5) {
                                        editRolesText = if (editRolesText.isBlank()) role else "$editRolesText, $role"
                                    }
                                }
                            ) {
                                Text(
                                    text = "+ $role",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val roles = editRolesText.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(5) // Enforce max 5 roles
                        onUpdateDesiredRoles(roles)
                        showEditRolesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditRolesDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBackground
        )
    }
}
