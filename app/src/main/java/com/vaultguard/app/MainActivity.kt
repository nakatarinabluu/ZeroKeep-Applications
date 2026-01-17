package com.vaultguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.ui.AddSecretScreen
import com.vaultguard.app.ui.AuthScreen
import com.vaultguard.app.ui.SetupScreen
import com.vaultguard.app.ui.VaultDashboard
import com.vaultguard.app.ui.theme.ZeroKeepTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @javax.inject.Inject
    lateinit var prefs: android.content.SharedPreferences

    @javax.inject.Inject
    lateinit var clipboardManager: com.vaultguard.app.utils.ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // LIFECYCLE: Register Clipboard Limiter (Clears clipboard on background)
        lifecycle.addObserver(clipboardManager)

        // ANTI-FORENSICS: Global FLAG_SECURE to prevent screenshots and screen recording
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            ZeroKeepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isSetupComplete = prefs.getBoolean("is_setup_complete", false)
                    ZeroKeepApp(isSetupComplete, prefs)
                }
            }
        }
    }
}

@Composable
fun ZeroKeepApp(isSetupComplete: Boolean, prefs: android.content.SharedPreferences) {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = "splash",
        enterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally() },
        exitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally() }
    ) {
        composable("splash") {
            com.vaultguard.app.ui.SplashScreen(onSplashFinished = {
                val next = if (isSetupComplete) "auth" else "setup"
                navController.navigate(next) {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        composable("setup") {

            SetupScreen(onSetupComplete = { password ->
                
                // Save MASTER PASSWORD
                val editor = prefs.edit()
                    .putBoolean("is_setup_complete", true)
                    .putString("master_password", password)
                    .remove("duress_password") // Clear any old duress password (setup now excludes it)

                editor.apply()
                
                navController.navigate("auth") {
                    popUpTo("setup") { inclusive = true }
                }
            })
        }
        composable("auth") {
            AuthScreen(
                onAuthenticated = {
                    navController.navigate("secret_graph") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                onReset = {
                    navController.navigate("setup") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        // Shared Graph for Secret Management
        navigation(startDestination = "dashboard", route = "secret_graph") {
            composable("dashboard") { entry ->
                // Scope ViewModel to the graph
                val parentEntry = remember(entry) { navController.getBackStackEntry("secret_graph") }
                val secretViewModel = hiltViewModel<com.vaultguard.app.ui.secret.SecretViewModel>(parentEntry)
                
                VaultDashboard(
                    onAddSecretClick = { navController.navigate("add_secret") },
                    onSignOut = {
                        navController.navigate("auth") {
                            popUpTo("secret_graph") { inclusive = true }
                        }
                    },
                    viewModel = secretViewModel // Pass shared instance
                )
            }
            composable("add_secret") { entry ->
                // Scope ViewModel to the graph
                val parentEntry = remember(entry) { navController.getBackStackEntry("secret_graph") }
                val secretViewModel = hiltViewModel<com.vaultguard.app.ui.secret.SecretViewModel>(parentEntry)
                
                AddSecretScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { 
                        // ViewModel already refreshed list internally via saveSecret -> loadSecrets
                        navController.popBackStack() 
                    },
                    viewModel = secretViewModel // Pass shared instance
                )
            }
        }
    }
}
