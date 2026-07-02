package com.aijobassistant.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.aijobassistant.app.model.UserProfile
import com.aijobassistant.app.ui.auth.LoginScreen
import com.aijobassistant.app.ui.auth.OnboardingScreen
import com.aijobassistant.app.ui.auth.SignUpScreen
import com.aijobassistant.app.ui.home.HomeScreen
import com.aijobassistant.app.ui.jobs.JobDiscoveryScreen
import com.aijobassistant.app.ui.profile.ProfileScreen
import com.aijobassistant.app.ui.resume.ResumeTailorScreen
import com.aijobassistant.app.ui.tracker.TrackerScreen
import com.aijobassistant.app.ui.theme.*

/**
 * Navigation routes for the app.
 */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Jobs : Screen("jobs")
    data object Resume : Screen("resume")
    data object Tracker : Screen("tracker")
    data object Profile : Screen("profile")
}

/**
 * Bottom navigation bar items.
 */
data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Home", Icons.Default.Home),
    BottomNavItem(Screen.Jobs, "Jobs", Icons.Default.Search),
    BottomNavItem(Screen.Resume, "Resume", Icons.Default.Description),
    BottomNavItem(Screen.Tracker, "Tracker", Icons.Default.Checklist),
    BottomNavItem(Screen.Profile, "Profile", Icons.Default.Person)
)

/**
 * Main navigation composable for the entire app.
 * Handles auth flow (login/signup/onboarding) and main app flow (bottom nav).
 */
@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    isOnboardingComplete: Boolean,
    isAuthLoading: Boolean = false,
    authError: String? = null,
    profile: UserProfile = UserProfile(),
    onLogin: (String, String) -> Unit,
    onGoogleSignIn: () -> Unit,
    onSignUp: (String, String, String, String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onCompleteOnboarding: (android.net.Uri?, String, () -> Unit) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val navController = rememberNavController()

    val startDestination = when {
        !isLoggedIn -> Screen.Login.route
        !isOnboardingComplete -> Screen.Onboarding.route
        else -> Screen.Home.route
    }

    LaunchedEffect(isLoggedIn, isOnboardingComplete) {
        if (isLoggedIn) {
            val currentRoute = navController.currentDestination?.route
            if (isOnboardingComplete) {
                if (currentRoute == Screen.Onboarding.route || currentRoute == Screen.Login.route || currentRoute == Screen.SignUp.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            } else {
                if (currentRoute == Screen.Login.route || currentRoute == Screen.SignUp.route) {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    // Determine if bottom nav should be visible
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomNav = currentDestination?.route in bottomNavItems.map { it.screen.route }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            if (showBottomNav) {
                NavigationBar(
                    containerColor = CardBackground,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) PrimaryBlue else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    color = if (selected) PrimaryBlue else TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                unselectedIconColor = TextMuted,
                                indicatorColor = PrimaryBlueContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth screens
            composable(Screen.Login.route) {
                LoginScreen(
                    onLogin = onLogin,
                    onGoogleSignIn = onGoogleSignIn,
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onForgotPassword = onForgotPassword,
                    isLoading = isAuthLoading,
                    errorMessage = authError
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUp = onSignUp,
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    isLoading = isAuthLoading,
                    errorMessage = authError
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = { uri, url ->
                        onCompleteOnboarding(uri, url) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    isLoading = isAuthLoading,
                    errorMessage = authError
                )
            }

            // Main app screens
            composable(Screen.Home.route) {
                val coroutineScope = rememberCoroutineScope()
                val trackerRepository = remember { com.aijobassistant.app.data.tracker.TrackerRepository() }
                val jobsRepository = remember { com.aijobassistant.app.data.jobs.JobsRepository() }
                val applications by trackerRepository.observeApplications().collectAsState(initial = emptyList())
                
                val totalApplications = applications.size
                val pendingJobs = applications.filter { it.status == com.aijobassistant.app.model.ApplicationStatus.PENDING }
                val pendingCount = pendingJobs.size
                val appliedCount = applications.count { it.status == com.aijobassistant.app.model.ApplicationStatus.APPLIED }
                val rejectedCount = applications.count { it.status == com.aijobassistant.app.model.ApplicationStatus.REJECTED }
                val recentApplications = applications.filter { it.status != com.aijobassistant.app.model.ApplicationStatus.PENDING }
                    .sortedByDescending { it.dateApplied }
                    .take(5)

                HomeScreen(
                    userName = profile.personalInfo.fullName,
                    totalApplications = totalApplications,
                    pendingCount = pendingCount,
                    appliedCount = appliedCount,
                    rejectedCount = rejectedCount,
                    pendingJobs = pendingJobs,
                    recentApplications = recentApplications,
                    onNavigateToJobs = {
                        navController.navigate(Screen.Jobs.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToResume = {
                        navController.navigate(Screen.Resume.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToTracker = {
                        navController.navigate(Screen.Tracker.route) {
                            launchSingleTop = true
                        }
                    },
                    onApproveJob = { app ->
                        coroutineScope.launch {
                            trackerRepository.updateStatus(app.id, com.aijobassistant.app.model.ApplicationStatus.APPLIED)
                        }
                    },
                    onDenyJob = { app ->
                        coroutineScope.launch {
                            trackerRepository.updateStatus(app.id, com.aijobassistant.app.model.ApplicationStatus.REJECTED)
                        }
                    },
                    onTriggerAgent = { log ->
                        log("Starting continuous job search...")
                        try {
                            val role = profile.jobPreferences.desiredRoles.firstOrNull() ?: "Software Engineer"
                            log("--- Searching for: '$role' ---")
                            
                            val searchResponse = jobsRepository.searchJobs(role)
                            if (searchResponse.isSuccess) {
                                val rawJobs = searchResponse.getOrNull() ?: emptyList()
                                log("Found ${rawJobs.size} raw jobs. Evaluating...")
                                
                                val evalResponse = jobsRepository.evaluateJobs(rawJobs.take(3).map { it.toMap() })
                                if (evalResponse.isSuccess) {
                                    val evaluated = evalResponse.getOrNull() ?: emptyList()
                                    var count = 0
                                    for (job in evaluated) {
                                        if (job.score >= 70) {
                                            log("=> MATCH! ${job.title} @ ${job.company} - Score: ${job.score}%")
                                            trackerRepository.addApplication(
                                                com.aijobassistant.app.model.Application(
                                                    company = job.company,
                                                    jobTitle = job.title,
                                                    status = com.aijobassistant.app.model.ApplicationStatus.PENDING,
                                                    jobLink = job.link ?: "",
                                                    techStack = job.techStack
                                                )
                                            )
                                            count++
                                        } else {
                                            log("=> Pass. ${job.title} @ ${job.company} - Score: ${job.score}%")
                                        }
                                    }
                                    log("Digest complete! Logged $count jobs as Pending.")
                                } else {
                                    log("Evaluation failed: ${evalResponse.exceptionOrNull()?.message}")
                                }
                            } else {
                                log("Search failed: ${searchResponse.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            log("Agent error: ${e.localizedMessage}")
                        }
                    }
                )
            }

            composable(Screen.Jobs.route) {
                JobDiscoveryScreen()
            }

            composable(Screen.Resume.route) {
                ResumeTailorScreen()
            }

            composable(Screen.Tracker.route) {
                TrackerScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    profile = profile,
                    onSignOut = {
                        onSignOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onRebuildProfile = {
                        navController.navigate(Screen.Onboarding.route) {
                            launchSingleTop = true
                        }
                    },
                    onDeleteAccount = {
                        onDeleteAccount()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
