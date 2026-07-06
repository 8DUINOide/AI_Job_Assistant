package com.aijobassistant.app.ui.resume

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.theme.*

/**
 * Resume Builder screen — an editable form that mirrors the developer-resume-template.
 * Users can edit every field and download as PDF or DOCX.
 */
@Composable
fun ResumeBuilderScreen(
    state: ResumeBuilderState,
    onGeneratePdf: () -> Unit = {},
    onGenerateDocx: () -> Unit = {},
    onAddEducation: () -> Unit = {},
    onRemoveEducation: (Int) -> Unit = {},
    onAddExperience: () -> Unit = {},
    onRemoveExperience: (Int) -> Unit = {},
    onAddProject: () -> Unit = {},
    onRemoveProject: (Int) -> Unit = {},
    onAddActivity: () -> Unit = {},
    onRemoveActivity: (Int) -> Unit = {},
    onFieldChanged: () -> Unit = {},
    onClearMessages: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Resume Builder",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Edit your resume and download as PDF or DOCX",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Error message
        if (state.error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = StatusDanger, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(state.error, color = StatusDanger, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Success message
        if (state.successMessage != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StatusSuccessContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(state.successMessage, color = StatusSuccess, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading your profile...", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
            return
        }

        // === PERSONAL INFO SECTION ===
        SectionCard(title = "Personal Information", icon = Icons.Default.Person) {
            TemplateTextField(label = "Full Name", value = state.personalInfo.name,
                onValueChange = { state.personalInfo.name = it; onFieldChanged() })
            TemplateTextField(label = "Location", value = state.personalInfo.location,
                onValueChange = { state.personalInfo.location = it; onFieldChanged() })
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TemplateTextField(label = "Phone", value = state.personalInfo.phone,
                    onValueChange = { state.personalInfo.phone = it; onFieldChanged() },
                    modifier = Modifier.weight(1f))
                TemplateTextField(label = "Email", value = state.personalInfo.email,
                    onValueChange = { state.personalInfo.email = it; onFieldChanged() },
                    modifier = Modifier.weight(1f))
            }
            TemplateTextField(label = "LinkedIn URL", value = state.personalInfo.linkedin,
                onValueChange = { state.personalInfo.linkedin = it; onFieldChanged() })
            TemplateTextField(label = "GitHub / Portfolio URL", value = state.personalInfo.github,
                onValueChange = { state.personalInfo.github = it; onFieldChanged() })
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === EDUCATION SECTION ===
        SectionCard(
            title = "Education",
            icon = Icons.Default.School,
            onAdd = onAddEducation,
            addLabel = "Add Education"
        ) {
            state.education.forEachIndexed { index, edu ->
                ItemCard(
                    index = index,
                    onRemove = { onRemoveEducation(index) }
                ) {
                    TemplateTextField(label = "University", value = edu.university,
                        onValueChange = { edu.university = it; onFieldChanged() })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateTextField(label = "Degree", value = edu.degree,
                            onValueChange = { edu.degree = it; onFieldChanged() },
                            modifier = Modifier.weight(1f))
                        TemplateTextField(label = "Year", value = edu.year,
                            onValueChange = { edu.year = it; onFieldChanged() },
                            modifier = Modifier.weight(0.4f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateTextField(label = "Location", value = edu.location,
                            onValueChange = { edu.location = it; onFieldChanged() },
                            modifier = Modifier.weight(1f))
                    }
                    TemplateTextField(label = "Details (Major/Minor, GWA, Honors)", value = edu.details,
                        onValueChange = { edu.details = it; onFieldChanged() }, minLines = 2)
                    TemplateTextField(label = "Relevant Coursework", value = edu.coursework,
                        onValueChange = { edu.coursework = it; onFieldChanged() }, minLines = 2)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === WORK EXPERIENCE SECTION ===
        SectionCard(
            title = "Work Experience",
            icon = Icons.Default.Work,
            onAdd = onAddExperience,
            addLabel = "Add Experience"
        ) {
            state.experience.forEachIndexed { index, exp ->
                ItemCard(
                    index = index,
                    onRemove = { onRemoveExperience(index) }
                ) {
                    TemplateTextField(label = "Company", value = exp.company,
                        onValueChange = { exp.company = it; onFieldChanged() })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateTextField(label = "Job Title", value = exp.title,
                            onValueChange = { exp.title = it; onFieldChanged() },
                            modifier = Modifier.weight(1f))
                        TemplateTextField(label = "Location", value = exp.location,
                            onValueChange = { exp.location = it; onFieldChanged() },
                            modifier = Modifier.weight(0.7f))
                    }
                    TemplateTextField(label = "Date Range (e.g. Jan 2024 – Present)", value = exp.dateRange,
                        onValueChange = { exp.dateRange = it; onFieldChanged() })
                    TemplateTextField(
                        label = "Bullet Points (one per line)",
                        value = exp.bullets,
                        onValueChange = { exp.bullets = it; onFieldChanged() },
                        minLines = 4
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === PROJECTS SECTION ===
        SectionCard(
            title = "Projects",
            icon = Icons.Default.Code,
            onAdd = onAddProject,
            addLabel = "Add Project"
        ) {
            state.projects.forEachIndexed { index, proj ->
                ItemCard(
                    index = index,
                    onRemove = { onRemoveProject(index) }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateTextField(label = "Project Name", value = proj.name,
                            onValueChange = { proj.name = it; onFieldChanged() },
                            modifier = Modifier.weight(1f))
                        TemplateTextField(label = "Year", value = proj.year,
                            onValueChange = { proj.year = it; onFieldChanged() },
                            modifier = Modifier.weight(0.35f))
                    }
                    TemplateTextField(
                        label = "Bullet Points (one per line)",
                        value = proj.bullets,
                        onValueChange = { proj.bullets = it; onFieldChanged() },
                        minLines = 3
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === ACTIVITIES SECTION ===
        SectionCard(
            title = "Activities",
            icon = Icons.Default.Groups,
            onAdd = onAddActivity,
            addLabel = "Add Activity"
        ) {
            state.activities.forEachIndexed { index, act ->
                ItemCard(
                    index = index,
                    onRemove = { onRemoveActivity(index) }
                ) {
                    TemplateTextField(label = "Organization", value = act.organization,
                        onValueChange = { act.organization = it; onFieldChanged() })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TemplateTextField(label = "Role", value = act.role,
                            onValueChange = { act.role = it; onFieldChanged() },
                            modifier = Modifier.weight(1f))
                        TemplateTextField(label = "Location", value = act.location,
                            onValueChange = { act.location = it; onFieldChanged() },
                            modifier = Modifier.weight(0.7f))
                    }
                    TemplateTextField(label = "Date Range", value = act.dateRange,
                        onValueChange = { act.dateRange = it; onFieldChanged() })
                    TemplateTextField(
                        label = "Bullet Points (one per line)",
                        value = act.bullets,
                        onValueChange = { act.bullets = it; onFieldChanged() },
                        minLines = 3
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // === ADDITIONAL SECTION ===
        SectionCard(title = "Additional", icon = Icons.Default.Star) {
            TemplateTextField(
                label = "Technical Skills (comma-separated)",
                value = state.additional.technicalSkills,
                onValueChange = { state.additional.technicalSkills = it; onFieldChanged() },
                minLines = 2
            )
            TemplateTextField(
                label = "Certifications (comma-separated)",
                value = state.additional.certifications,
                onValueChange = { state.additional.certifications = it; onFieldChanged() },
                minLines = 2
            )
            TemplateTextField(
                label = "Languages (e.g. English (fluent), Filipino (native))",
                value = state.additional.languages,
                onValueChange = { state.additional.languages = it; onFieldChanged() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // === DOWNLOAD BUTTONS ===
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onGeneratePdf,
                enabled = !state.isGenerating,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download PDF", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = onGenerateDocx,
                enabled = !state.isGenerating,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp), tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download DOCX", fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                }
            }
        }

        // Bottom padding for nav bar
        Spacer(modifier = Modifier.height(100.dp))
    }
}

// === Reusable Components ===

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    onAdd: (() -> Unit)? = null,
    addLabel: String = "Add",
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PrimaryBlueContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (onAdd != null) {
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(addLabel, color = PrimaryBlue, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            content()
        }
    }
}

@Composable
private fun ItemCard(
    index: Int,
    onRemove: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = StatusDanger, modifier = Modifier.size(16.dp))
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        minLines = minLines,
        shape = RoundedCornerShape(10.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = BorderColor,
            focusedLabelColor = PrimaryBlue,
            cursorColor = PrimaryBlue
        )
    )
}
