package com.aijobassistant.app.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import com.aijobassistant.app.R

@Composable
fun LoginScreen(
    onLogin: (email: String, password: String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Animated background blobs
        AnimatedBackgroundBlobs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.height(180.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // App branding
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo_nobg),
                    contentDescription = "App Icon",
                    modifier = Modifier.requiredSize(380.dp).align(Alignment.Center)
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 16.dp)
                ) {
                    Text(
                        text = "AI Job Assistant",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your autonomous job hunting companion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login form card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sign in to continue your job search",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field
                AppTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = null
                    },
                    label = "Email Address",
                    isError = emailError != null,
                    errorMessage = emailError,
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextMuted)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                AppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    isPassword = !showPassword,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password",
                                tint = TextMuted
                            )
                        }
                    }
                )

                // Forgot password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentIndigoLight,
                        modifier = Modifier.clickable { showForgotDialog = true }
                    )
                }

                // Error message
                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusDanger,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Login button
                GradientButton(
                    text = "Sign In",
                    onClick = {
                        if (email.isBlank()) {
                            emailError = "Email is required"
                            return@GradientButton
                        }
                        if (password.isBlank()) {
                            return@GradientButton
                        }
                        onLogin(email.trim(), password)
                    },
                    isLoading = isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderColor
                    )
                    Text(
                        text = "  or  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = BorderColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In button
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardBackground,
                        contentColor = TextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Sign up link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Forgot Password Dialog
    if (showForgotDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("Reset Password", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Enter your email to receive a password reset link.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = "Email Address"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (resetEmail.isNotBlank()) {
                        onForgotPassword(resetEmail.trim())
                        showForgotDialog = false
                    }
                }) {
                    Text("Send Reset Link", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = CardBackground,
            tonalElevation = 8.dp
        )
    }
}
