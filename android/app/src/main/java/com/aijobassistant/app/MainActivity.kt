package com.aijobassistant.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.aijobassistant.app.data.auth.AuthRepository
import com.aijobassistant.app.data.profile.ProfileRepository
import com.aijobassistant.app.model.UserProfile
import com.aijobassistant.app.navigation.AppNavigation
import com.aijobassistant.app.ui.theme.AIJobAssistantTheme
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

        setContent {
            AIJobAssistantTheme {
                var isLoggedIn by remember { mutableStateOf(authRepository.isLoggedIn) }
                var isOnboardingComplete by remember { mutableStateOf(false) }
                var isAuthLoading by remember { mutableStateOf(false) }
                var authError by remember { mutableStateOf<String?>(null) }
                var userProfile by remember { mutableStateOf(UserProfile()) }

                // Observe auth state
                LaunchedEffect(Unit) {
                    authRepository.authStateFlow.collect { user ->
                        isLoggedIn = user != null
                        if (user != null) {
                            // Check onboarding status
                            isOnboardingComplete = authRepository.isOnboardingComplete()

                            // Load profile if onboarding complete
                            if (isOnboardingComplete) {
                                loadProfile(user.uid) { profile ->
                                    userProfile = profile
                                }
                            }
                        }
                    }
                }

                AppNavigation(
                    isLoggedIn = isLoggedIn,
                    isOnboardingComplete = isOnboardingComplete,
                    isAuthLoading = isAuthLoading,
                    authError = authError,
                    profile = userProfile,
                    onLogin = { email, password ->
                        isAuthLoading = true
                        authError = null
                        lifecycleScope.launch {
                            val result = authRepository.signIn(email, password)
                            isAuthLoading = false
                            result.onFailure { authError = it.localizedMessage }
                        }
                    },
                    onGoogleSignIn = {
                        isAuthLoading = true
                        authError = null
                        lifecycleScope.launch {
                            try {
                                val credentialManager = androidx.credentials.CredentialManager.create(this@MainActivity)
                                val googleIdOption = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(getString(R.string.default_web_client_id))
                                    .setAutoSelectEnabled(false)
                                    .build()

                                val request = androidx.credentials.GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()

                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = this@MainActivity
                                )

                                val credential = result.credential
                                if (credential is androidx.credentials.CustomCredential &&
                                    credential.type == com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                    val googleIdTokenCredential = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
                                    
                                    val signInResult = authRepository.signInWithGoogle(googleIdTokenCredential.idToken)
                                    signInResult.onFailure { authError = it.localizedMessage }
                                } else {
                                    authError = "Unexpected credential type"
                                }
                            } catch (e: Exception) {
                                authError = "Google Sign In Failed: ${e.localizedMessage}"
                            } finally {
                                isAuthLoading = false
                            }
                        }
                    },
                    onSignUp = { email, password, firstName, lastName ->
                        isAuthLoading = true
                        authError = null
                        lifecycleScope.launch {
                            val result = authRepository.signUp(email, password, firstName, lastName)
                            isAuthLoading = false
                            result.onFailure { authError = it.localizedMessage }
                        }
                    },
                    onForgotPassword = { email ->
                        lifecycleScope.launch {
                            authRepository.sendPasswordResetEmail(email)
                        }
                    },
                    onCompleteOnboarding = { resumeUri, portfolioUrl, onSuccess ->
                        lifecycleScope.launch {
                            isAuthLoading = true
                            try {
                                if (resumeUri != null) {
                                    val inputStream = contentResolver.openInputStream(resumeUri)
                                    val bytes = inputStream?.readBytes()
                                    inputStream?.close()
                                    
                                    if (bytes != null) {
                                        val result = profileRepository.buildProfileFromPdf(bytes, portfolioUrl)
                                        if (result.isSuccess && result.getOrNull() != null) {
                                            userProfile = result.getOrNull()!!
                                        } else {
                                            throw Exception(result.exceptionOrNull()?.message ?: "Failed to build profile")
                                        }
                                    }
                                }
                                authRepository.markOnboardingComplete()
                                isOnboardingComplete = true
                                onSuccess()
                            } catch (e: Exception) {
                                authError = e.localizedMessage
                            } finally {
                                isAuthLoading = false
                            }
                        }
                    },
                    onSignOut = {
                        authRepository.signOut()
                        userProfile = UserProfile()
                    },
                    onDeleteAccount = {
                        lifecycleScope.launch {
                            authRepository.deleteAccount()
                            userProfile = UserProfile()
                        }
                    }
                )
            }
        }
    }

    private fun loadProfile(uid: String, onLoaded: (UserProfile) -> Unit) {
        lifecycleScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("profile").document("master")
                    .get().await()

                if (doc.exists()) {
                    val data = doc.data
                    if (data != null) {
                        onLoaded(UserProfile.fromMap(data))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "job_alerts",
                "Job Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new high-match job opportunities"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
