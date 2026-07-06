package com.aijobassistant.app.ui.resume

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeTailorScreen(
    matchRate: Int?,
    keywordsToInclude: List<String>,
    missingKeywords: List<String>,
    isAnalyzing: Boolean,
    isGenerating: Boolean,
    tailoredData: Map<String, Any>?,
    coverLetterText: String?,
    initialJobDescription: String,
    onAnalyze: (String) -> Unit,
    onGenerateResumePdf: (Map<String, Any>) -> Unit,
    onGenerateCoverLetterPdf: (String) -> Unit,
    onAddKeyword: (String) -> Unit
) {
    var jobDescription by remember { mutableStateOf(initialJobDescription) }
    var activeTab by remember { mutableStateOf(0) }
    var editableData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var editableCoverLetter by remember { mutableStateOf(coverLetterText ?: "") }

    // Update editableData when tailoredData changes from API
    LaunchedEffect(tailoredData) {
        if (tailoredData != null) {
            editableData = tailoredData
        }
    }
    LaunchedEffect(coverLetterText) {
        if (coverLetterText != null) {
            editableCoverLetter = coverLetterText
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AnimatedBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Text(
                text = "Tailor Resume",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Optimize your resume for a specific job",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (tailoredData == null && !isAnalyzing) {
                // Step 1: Input JD
                OutlinedTextField(
                    value = jobDescription,
                    onValueChange = { jobDescription = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    placeholder = { Text("Paste the job description here...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = SurfaceElevated.copy(alpha = 0.5f),
                        unfocusedContainerColor = SurfaceElevated.copy(alpha = 0.3f),
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                GradientButton(
                    text = "Analyze & Tailor",
                    icon = Icons.Default.AutoFixHigh,
                    onClick = { onAnalyze(jobDescription) },
                    enabled = jobDescription.isNotBlank()
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else if (isAnalyzing) {
                // Analyzing State
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI is analyzing and tailoring your resume...", color = TextSecondary)
                    }
                }
            } else if (editableData != null) {
                // Step 2: Edit Tailored Data
                CustomTabRow(
                    tabs = listOf("Resume Form", "Cover Letter", "Analysis"),
                    selectedTabIndex = activeTab,
                    onTabSelected = { activeTab = it },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (activeTab == 0) {
                        // Editable Resume Form
                        Text("Personal Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val pInfo = (editableData?.get("personal_info") as? Map<String, String>) ?: emptyMap()
                        
                        var name by remember { mutableStateOf(pInfo["name"] ?: "") }
                        var email by remember { mutableStateOf(pInfo["email"] ?: "") }
                        var phone by remember { mutableStateOf(pInfo["phone"] ?: "") }
                        var link by remember { mutableStateOf(pInfo["link"] ?: "") }

                        OutlinedTextField(value = name, onValueChange = { name = it; editableData = updateMap(editableData, "personal_info", "name", it) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it; editableData = updateMap(editableData, "personal_info", "email", it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it; editableData = updateMap(editableData, "personal_info", "phone", it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = link, onValueChange = { link = it; editableData = updateMap(editableData, "personal_info", "link", it) }, label = { Text("Link") }, modifier = Modifier.fillMaxWidth())

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        var summary by remember { mutableStateOf((editableData?.get("summary") as? String) ?: "") }
                        OutlinedTextField(value = summary, onValueChange = { summary = it; editableData = updateTopLevel(editableData, "summary", it) }, modifier = Modifier.fillMaxWidth().height(120.dp))
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sections (Experience, Edu, etc.)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("We recommend making major edits in the master profile.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        GradientButton(
                            text = if (isGenerating) "Generating..." else "Save & Generate Resume PDF",
                            icon = Icons.Default.PictureAsPdf,
                            onClick = { editableData?.let { onGenerateResumePdf(it) } },
                            enabled = !isGenerating
                        )

                    } else if (activeTab == 1) {
                        // Cover Letter Edit
                        Text("Cover Letter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = editableCoverLetter,
                            onValueChange = { editableCoverLetter = it },
                            modifier = Modifier.fillMaxWidth().height(400.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GradientButton(
                            text = if (isGenerating) "Generating..." else "Generate Cover Letter PDF",
                            icon = Icons.Default.PictureAsPdf,
                            onClick = { onGenerateCoverLetterPdf(editableCoverLetter) },
                            enabled = !isGenerating
                        )
                    } else {
                        // Analysis
                        matchRate?.let {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(progress = { it / 100f }, color = PrimaryBlue, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Match Rate: $it%", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Keywords to Include:", fontWeight = FontWeight.Bold)
                        Text(missingKeywords.joinToString(", "))
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// Helper to update deeply nested map purely for the UI form binding
@Suppress("UNCHECKED_CAST")
fun updateMap(root: Map<String, Any>?, section: String, key: String, value: String): Map<String, Any> {
    val map = root?.toMutableMap() ?: mutableMapOf()
    val sub = (map[section] as? Map<String, Any>)?.toMutableMap() ?: mutableMapOf()
    sub[key] = value
    map[section] = sub
    return map
}

fun updateTopLevel(root: Map<String, Any>?, key: String, value: String): Map<String, Any> {
    val map = root?.toMutableMap() ?: mutableMapOf()
    map[key] = value
    return map
}
