package com.aijobassistant.app.ui.auth

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

/**
 * Onboarding screen with 3 steps:
 * 1. Upload Resume PDF
 * 2. Enter Portfolio URL (optional)
 * 3. AI processes and builds profile
 *
 * Shown after first sign-up. Can also be accessed from Profile to rebuild.
 */
@Composable
fun OnboardingScreen(
    onComplete: (resumeUri: Uri?, portfolioUrl: String) -> Unit,
    isLoading: Boolean = false,
    processingStep: String = "",
    errorMessage: String? = null
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedResumeUri by remember { mutableStateOf<Uri?>(null) }
    var selectedResumeFileName by remember { mutableStateOf<String?>(null) }
    var portfolioUrl by remember { mutableStateOf("") }
    val context = LocalContext.current

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedResumeUri = it
            // Extract filename from URI
            var filename = "resume.pdf"
            if (it.scheme == "content") {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            filename = cursor.getString(index)
                        }
                    }
                }
            }
            if (filename == "resume.pdf") {
                filename = it.lastPathSegment?.substringAfterLast('/') ?: "resume.pdf"
            }
            selectedResumeFileName = filename
        }
    }

    val steps = listOf("Upload Resume", "Portfolio Link", "Build Profile")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        AnimatedBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Header
            Text(
                text = "Set Up Your Profile",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, AccentIndigo)
                    )
                ),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let AI analyze your experience to find the best jobs for you",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Step indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    // Step circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    index < currentStep -> StatusSuccess
                                    index == currentStep -> PrimaryBlue
                                    else -> SurfaceElevated
                                }
                            )
                    ) {
                        if (index < currentStep) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (index == currentStep) TextPrimary else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Connector line
                    if (index < steps.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(
                                    if (index < currentStep) StatusSuccess
                                    else SurfaceElevated
                                )
                        )
                    }
                }
            }

            // Step label
            Text(
                text = steps[currentStep],
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryBlueLight,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Step content
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "onboarding_step"
                ) { step ->
                    when (step) {
                        0 -> StepUploadResume(
                            selectedFileName = selectedResumeFileName,
                            onPickFile = { filePickerLauncher.launch("application/pdf") }
                        )
                        1 -> StepPortfolioUrl(
                            portfolioUrl = portfolioUrl,
                            onUrlChange = { portfolioUrl = it }
                        )
                        2 -> StepBuildProfile(
                            isLoading = isLoading,
                            processingStep = processingStep,
                            errorMessage = errorMessage
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Back button
                if (currentStep > 0 && !isLoading) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Text("Back")
                    }
                }

                // Next / Complete button
                GradientButton(
                    text = when (currentStep) {
                        0 -> if (selectedResumeUri != null) "Next" else "Skip for Now"
                        1 -> "Next"
                        2 -> if (isLoading) "Processing..." else "Build My Profile"
                        else -> "Next"
                    },
                    onClick = {
                        when (currentStep) {
                            0 -> currentStep = 1
                            1 -> currentStep = 2
                            2 -> onComplete(selectedResumeUri, portfolioUrl.trim())
                        }
                    },
                    modifier = Modifier.weight(if (currentStep > 0 && !isLoading) 1f else 1f),
                    isLoading = isLoading,
                    gradientColors = if (currentStep == 2) {
                        listOf(StatusSuccess, AccentIndigoLight)
                    } else {
                        listOf(PrimaryBlue, AccentIndigo)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepUploadResume(
    selectedFileName: String?,
    onPickFile: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "📄",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Upload Your Resume",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Our AI will extract your skills, experience, and education to build your profile automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Upload area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 2.dp,
                    brush = if (selectedFileName != null) {
                        Brush.horizontalGradient(listOf(StatusSuccess, StatusSuccess))
                    } else {
                        Brush.horizontalGradient(listOf(PrimaryBlue.copy(alpha = 0.5f), AccentIndigo.copy(alpha = 0.5f)))
                    },
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    if (selectedFileName != null) StatusSuccessContainer
                    else Color(0x0DFFFFFF)
                )
                .clickable { onPickFile() },
            contentAlignment = Alignment.Center
        ) {
            if (selectedFileName != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = StatusSuccess,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedFileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tap to change",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = "Upload",
                        tint = PrimaryBlueLight,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to select PDF resume",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryBlueLight
                    )
                }
            }
        }

        Text(
            text = "Supported format: PDF",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun StepPortfolioUrl(
    portfolioUrl: String,
    onUrlChange: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "🌐",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Portfolio / LinkedIn",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Optional: Add your portfolio URL or LinkedIn profile for a richer profile analysis.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        AppTextField(
            value = portfolioUrl,
            onValueChange = onUrlChange,
            label = "Portfolio or LinkedIn URL (Optional)",
            leadingIcon = {
                Icon(Icons.Default.Link, contentDescription = null, tint = TextMuted)
            }
        )
    }
}

@Composable
private fun StepBuildProfile(
    isLoading: Boolean,
    processingStep: String,
    errorMessage: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isLoading) "⚙️" else "🚀",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isLoading) "Building Your Profile..." else "Ready to Launch",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = PrimaryBlue,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = processingStep,
                style = MaterialTheme.typography.bodyMedium,
                color = AccentIndigoLight,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = "AI will analyze your resume and portfolio to identify your skills, experience, and ideal job roles.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // What will happen list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                OnboardingCheckItem("✅", "Extract your skills & experience")
                OnboardingCheckItem("✅", "Identify your ideal job roles")
                OnboardingCheckItem("✅", "Build your master profile for job matching")
                OnboardingCheckItem("✅", "Enable AI-powered resume tailoring")
            }
        }

        // Error
        AnimatedVisibility(visible = errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = StatusDanger,
                modifier = Modifier.padding(top = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardingCheckItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}
