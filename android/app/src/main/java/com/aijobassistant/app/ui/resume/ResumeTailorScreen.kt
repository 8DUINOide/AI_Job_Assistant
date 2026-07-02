package com.aijobassistant.app.ui.resume

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Resume Tailor screen — paste a JD, analyze keywords, edit, and generate PDF.
 * Converts the web dashboard's "Resume Tailor" section.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeTailorScreen(
    matchRate: Int = 0,
    keywordsToInclude: List<String> = emptyList(),
    missingKeywords: List<String> = emptyList(),
    isAnalyzing: Boolean = false,
    isGenerating: Boolean = false,
    summaryText: String = "",
    coverLetterText: String = "",
    onAnalyze: (jobDescription: String) -> Unit = {},
    onGenerateResumePdf: (editedSummary: String) -> Unit = {},
    onGenerateCoverLetterPdf: (editedText: String) -> Unit = {},
    onAddKeyword: (keyword: String) -> Unit = {}
) {
    var jobDescription by remember { mutableStateOf("") }
    var showResults by remember { mutableStateOf(false) }
    var editedSummary by remember { mutableStateOf("") }
    var editedCoverLetter by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Resume, 1 = Cover Letter

    // Sync from props
    LaunchedEffect(summaryText) { editedSummary = summaryText }
    LaunchedEffect(coverLetterText) { editedCoverLetter = coverLetterText }
    LaunchedEffect(matchRate) { if (matchRate > 0) showResults = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Header
        Text(
            text = "Resume Tailor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "AI-powered resume optimization for any job",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Job description input
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "📄 Job Description",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Paste the job description to analyze keywords and generate a tailored resume.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                label = { Text("Paste job description here...", color = TextMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = BorderColor,
                    cursorColor = PrimaryBlue,
                    focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                    unfocusedContainerColor = CardBackground.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            GradientButton(
                text = "✨ Analyze & Tailor Resume",
                onClick = {
                    if (jobDescription.isNotBlank()) {
                        onAnalyze(jobDescription)
                    }
                },
                isLoading = isAnalyzing,
                gradientColors = listOf(AccentIndigo, PrimaryBlue)
            )
        }

        // Analysis Results
        AnimatedVisibility(visible = showResults && !isAnalyzing) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                // Match Rate
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Analysis Results",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = StatusSuccessContainer
                        ) {
                            Text(
                                "Match: ${matchRate}%",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Keywords to include
                    Text(
                        "Keywords to Include",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentIndigoLight,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        keywordsToInclude.forEach { keyword ->
                            SkillTag(
                                text = "$keyword +",
                                containerColor = AccentIndigoContainer,
                                textColor = AccentIndigoLight,
                                borderColor = AccentIndigoLight.copy(alpha = 0.3f),
                                modifier = Modifier.clickable { onAddKeyword(keyword) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Missing keywords
                    Text(
                        "Missing Keywords",
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusDanger,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        missingKeywords.forEach { keyword ->
                            SkillTag(
                                text = "$keyword +",
                                containerColor = StatusDangerContainer,
                                textColor = StatusDanger,
                                borderColor = StatusDanger.copy(alpha = 0.3f),
                                modifier = Modifier.clickable { onAddKeyword(keyword) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab selector: Resume / Cover Letter
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = CardBackgroundTranslucent,
                    contentColor = PrimaryBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = PrimaryBlue
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("📄 Resume") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("✉️ Cover Letter") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    0 -> {
                        // Editable summary
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Professional Summary",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = editedSummary,
                                onValueChange = { editedSummary = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor,
                                    cursorColor = PrimaryBlue,
                                    focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                                    unfocusedContainerColor = CardBackground.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GradientButton(
                                text = "📄 Generate Resume PDF",
                                onClick = { onGenerateResumePdf(editedSummary) },
                                isLoading = isGenerating,
                                gradientColors = listOf(StatusSuccess, Color(0xFF059669))
                            )
                        }
                    }
                    1 -> {
                        // Editable cover letter
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Cover Letter",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = editedCoverLetter,
                                onValueChange = { editedCoverLetter = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = BorderColor,
                                    cursorColor = PrimaryBlue,
                                    focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                                    unfocusedContainerColor = CardBackground.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            GradientButton(
                                text = "✉️ Generate Cover Letter PDF",
                                onClick = { onGenerateCoverLetterPdf(editedCoverLetter) },
                                isLoading = isGenerating,
                                gradientColors = listOf(AccentIndigoLight, AccentIndigo)
                            )
                        }
                    }
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(80.dp))
    }
}

/**
 * Simple FlowRow that wraps items to the next line.
 * A basic implementation since Compose provides this in newer versions.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
    }
}
