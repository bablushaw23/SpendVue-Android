package com.spendvue

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.spendvue.auth.AuthManager
import com.spendvue.ui.accounts.AccountsScreen
import com.spendvue.ui.accounts.AddEditAccountScreen
import com.spendvue.ui.auth.LoginScreen
import com.spendvue.ui.navigation.Screen
import com.spendvue.ui.onboarding.WelcomeScreen
import com.spendvue.ui.theme.SpendSenseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // FUTURE: Replace isSystemInDarkTheme() with user preference from DataStore
            SpendSenseTheme(darkTheme = isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Determine start destination based on auth state
                    val startDestination = if (authManager.isLoggedIn()) {
                        Screen.Accounts.route
                    } else {
                        Screen.Login.route
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        // ── Login ─────────────────────────────────────────────
                        composable(route = Screen.Login.route) {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate(Screen.Welcome.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ── Welcome / OnBoarding ──────────────────────────────
                        composable(route = Screen.Welcome.route) {
                            WelcomeScreen(
                                userName = authManager.getName() ?: "there",
                                onGetStarted = {
                                    navController.navigate(Screen.Accounts.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // ── Accounts List ─────────────────────────────────────
                        composable(route = Screen.Accounts.route) {
                            AccountsScreen(
                                onNavigateToAddAccount = {
                                    navController.navigate(Screen.AddEditAccount.passId(null))
                                },
                                onNavigateToEditAccount = { accountId ->
                                    navController.navigate(Screen.AddEditAccount.passId(accountId))
                                }
                            )
                        }

                        // ── Add / Edit Account ────────────────────────────────
                        composable(
                            route = Screen.AddEditAccount.route,
                            arguments = listOf(
                                navArgument("accountId") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) {
                            AddEditAccountScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
