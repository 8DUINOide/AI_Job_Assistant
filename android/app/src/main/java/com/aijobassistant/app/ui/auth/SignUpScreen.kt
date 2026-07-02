package com.aijobassistant.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aijobassistant.app.ui.components.*
import com.aijobassistant.app.ui.theme.*

@Composable
fun SignUpScreen(
    onSignUp: (email: String, password: String, firstName: String, lastName: String) -> Unit,
    onNavigateToLogin: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }

    // Validation states
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true

        if (firstName.isBlank()) {
            firstNameError = "First name is required"
            isValid = false
        } else firstNameError = null

        if (email.isBlank() || !email.contains("@")) {
            emailError = "Valid email is required"
            isValid = false
        } else emailError = null

        if (password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else passwordError = null

        if (password != confirmPassword) {
            confirmError = "Passwords do not match"
            isValid = false
        } else confirmError = null

        return isValid
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineLarge.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, AccentIndigo)
                    )
                ),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Start your AI-powered job search journey",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Sign-up form
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            firstNameError = null
                        },
                        label = "First Name",
                        modifier = Modifier.weight(1f),
                        isError = firstNameError != null,
                        errorMessage = firstNameError,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                        }
                    )
                    AppTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Last Name",
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextMuted)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email
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

                // Password
                AppTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = "Password",
                    isPassword = !showPassword,
                    isError = passwordError != null,
                    errorMessage = passwordError,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password
                AppTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        confirmError = null
                    },
                    label = "Confirm Password",
                    isPassword = !showPassword,
                    isError = confirmError != null,
                    errorMessage = confirmError,
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Terms checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            uncheckedColor = TextMuted,
                            checkmarkColor = TextPrimary
                        )
                    )
                    Text(
                        text = "I agree to the Terms of Service and Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Sign Up button
                GradientButton(
                    text = "Create Account",
                    onClick = {
                        if (validate() && agreedToTerms) {
                            onSignUp(email.trim(), password, firstName.trim(), lastName.trim())
                        }
                    },
                    isLoading = isLoading,
                    enabled = agreedToTerms
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
