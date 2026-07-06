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
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

// Data classes for handling Compose state mapping from the API response
data class SectionItemState(
    val title: String,
    val subtitle: String,
    val date: String,
    var bullets: MutableState<String>
)

data class SectionState(
    val title: String,
    val items: List<SectionItemState>
)

data class TailoredDataState(
    var summary: MutableState<String>,
    val sections: List<SectionState>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumeTailorScreen(
    matchRate: Int = 0,
    keywordsToInclude: List<String> = emptyList(),
    missingKeywords: List<String> = emptyList(),
    isAnalyzing: Boolean = false,
    isGenerating: Boolean = false,
    tailoredData: Map<String, Any?>? = null,
    coverLetterText: String = "",
    onAnalyze: (jobDescription: String) -> Unit = {},
    onGenerateResumePdf: (editedData: Map<String, Any?>) -> Unit = {},
    onGenerateCoverLetterPdf: (editedText: String) -> Unit = {},
    onAddKeyword: (keyword: String) -> Unit = {},
    initialJobDescription: String = ""
) {
    var jobDescription by remember(initialJobDescription) { mutableStateOf(initialJobDescription) }
    var showResults by remember { mutableStateOf(false) }
    var editedCoverLetter by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Resume, 1 = Cover Letter
    
    var tailoredState by remember { mutableStateOf<TailoredDataState?>(null) }

    LaunchedEffect(coverLetterText) { editedCoverLetter = coverLetterText }
    LaunchedEffect(matchRate) { if (matchRate > 0) showResults = true }
    
    // Parse the JSON Map into our Compose State classes
    LaunchedEffect(tailoredData) {
        if (tailoredData != null) {
            val summaryStr = tailoredData["summary"] as? String ?: ""
            val sectionsList = tailoredData["sections"] as? List<Map<String, Any?>> ?: emptyList()
            val parsedSections = sectionsList.map { secMap ->
                val secTitle = secMap["title"] as? String ?: ""
                val itemsList = secMap["items"] as? List<Map<String, Any?>> ?: emptyList()
                val parsedItems = itemsList.map { itemMap ->
                    val iTitle = itemMap["title"] as? String ?: ""
                    val iSubtitle = itemMap["subtitle"] as? String ?: ""
                    val iDate = itemMap["date"] as? String ?: ""
                    val iBulletsList = itemMap["bullets"] as? List<*> ?: emptyList<Any>()
                    val iBullets = iBulletsList.filterIsInstance<String>().joinToString("\n")
                    SectionItemState(iTitle, iSubtitle, iDate, mutableStateOf(iBullets))
                }
                SectionState(secTitle, parsedItems)
            }
            tailoredState = TailoredDataState(mutableStateOf(summaryStr), parsedSections)
        }
    }

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
                    
                    val handleKeywordClick = { keyword: String -> 
                        // Find the Technical Skills section and append
                        tailoredState?.sections?.forEach { sec ->
                            sec.items.forEach { item ->
                                if (item.title.contains("Technical Skills", ignoreCase = true) || 
                                    item.title.contains("Skills", ignoreCase = true) ||
                                    sec.title.contains("SKILLS", ignoreCase = true)) {
                                    
                                    val currentText = item.bullets.value
                                    if (!currentText.contains(keyword, ignoreCase = true)) {
                                        if (currentText.isBlank()) {
                                            item.bullets.value = keyword
                                        } else if (currentText.endsWith(",")) {
                                            item.bullets.value = "$currentText $keyword"
                                        } else {
                                            item.bullets.value = "$currentText, $keyword"
                                        }
                                    }
                                }
                            }
                        }
                        onAddKeyword(keyword)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (keywordsToInclude.isEmpty()) {
                            Text("None identified", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        keywordsToInclude.forEach { keyword ->
                            SkillTag(
                                text = "$keyword +",
                                containerColor = AccentIndigoContainer,
                                textColor = AccentIndigoLight,
                                borderColor = AccentIndigoLight.copy(alpha = 0.3f),
                                modifier = Modifier.clickable { handleKeywordClick(keyword) }
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
                        if (missingKeywords.isEmpty()) {
                            Text("None identified", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        missingKeywords.forEach { keyword ->
                            SkillTag(
                                text = "$keyword +",
                                containerColor = StatusDangerContainer,
                                textColor = StatusDanger,
                                borderColor = StatusDanger.copy(alpha = 0.3f),
                                modifier = Modifier.clickable { handleKeywordClick(keyword) }
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
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cover Letter")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    0 -> {
                        // Editable tailored resume
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            tailoredState?.let { tState ->
                                Text(
                                    "Your Optimized Resume",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                Text(
                                    "Everything below has been tailored to the job description. Give it a final look and edit the text/skills if you'd like before generating.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Summary
                                Text(
                                    "Professional Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = tState.summary.value,
                                    onValueChange = { tState.summary.value = it },
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

                                // Dynamic Sections
                                tState.sections.forEach { section ->
                                    Divider(color = BorderColor, modifier = Modifier.padding(vertical = 12.dp))
                                    Text(
                                        section.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AccentIndigoLight,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    section.items.forEach { item ->
                                        Surface(
                                            color = CardBackgroundTranslucent,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                if (item.title.isNotBlank() && item.title != "null") {
                                                    Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                                }
                                                if (item.subtitle.isNotBlank() && item.subtitle != "null") {
                                                    Text(
                                                        "${item.subtitle}" + if(item.date.isNotBlank() && item.date != "null") " | ${item.date}" else "", 
                                                        color = TextMuted, 
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(bottom = 8.dp)
                                                    )
                                                }
                                                
                                                Text(
                                                    "Bullet Points (One per line)",
                                                    color = TextMuted,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                OutlinedTextField(
                                                    value = item.bullets.value,
                                                    onValueChange = { item.bullets.value = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(if (item.bullets.value.lines().size > 2) 100.dp else 60.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = TextPrimary,
                                                        unfocusedTextColor = TextPrimary,
                                                        focusedBorderColor = PrimaryBlue,
                                                        unfocusedBorderColor = BorderColor,
                                                        cursorColor = PrimaryBlue,
                                                        focusedContainerColor = CardBackground.copy(alpha = 0.3f),
                                                        unfocusedContainerColor = CardBackground.copy(alpha = 0.3f)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                GradientButton(
                                    text = "📄 Generate Resume PDF",
                                    onClick = { 
                                        val finalData = mapOf(
                                            "summary" to tState.summary.value,
                                            "sections" to tState.sections.map { sec ->
                                                mapOf(
                                                    "title" to sec.title,
                                                    "items" to sec.items.map { item ->
                                                        mapOf(
                                                            "title" to item.title,
                                                            "subtitle" to item.subtitle,
                                                            "date" to item.date,
                                                            "bullets" to item.bullets.value.split("\n").filter { it.isNotBlank() }
                                                        )
                                                    }
                                                )
                                            }
                                        )
                                        onGenerateResumePdf(finalData) 
                                    },
                                    isLoading = isGenerating,
                                    gradientColors = listOf(StatusSuccess, Color(0xFF059669))
                                )
                            } ?: run {
                                Text(
                                    "No resume data analyzed yet. Please paste a job description and click Analyze.",
                                    color = TextMuted,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    1 -> {
                        // Editable cover letter
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Your Cover Letter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(bottom = 16.dp)
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
                                text = "Generate Cover Letter PDF",
                                icon = Icons.Default.PictureAsPdf,
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
