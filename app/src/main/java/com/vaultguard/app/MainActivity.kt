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

    @javax.inject.Inject
    lateinit var kdfGenerator: com.vaultguard.app.security.KdfGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. INTEGRITY CHECK (Root/Emulator Detection)
        // If device is compromised, we shutdown immediately to protect data.
        if (com.vaultguard.app.security.SecurityUtils.isDeviceCompromised(this)) {
            // In a real banking app, we would show a Dialog explaining why.
            // For security, we crash/close silently or show a generic toast.
            android.widget.Toast.makeText(this, "Security Violation: Unsupported Device Environment", android.widget.Toast.LENGTH_LONG).show()
            finishAffinity() // Close all activities
            System.exit(0)   // Kill process
            return
        }

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
                    ZeroKeepApp(isSetupComplete, prefs, kdfGenerator)
                }
            }
        }
    }
}

@Composable
fun ZeroKeepApp(
    isSetupComplete: Boolean, 
    prefs: android.content.SharedPreferences,
    kdfGenerator: com.vaultguard.app.security.KdfGenerator
) {
    val navController = rememberNavController()

    val startDestination = if (isSetupComplete) "auth" else "setup"
    
    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally() },
        exitTransition = { androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutHorizontally() }
    ) {
        // Splash route removed to prevent "Triple Opening" glitch
        composable("setup") {

            SetupScreen(onSetupComplete = { password ->
                
                // Save MASTER PASSWORD HASH (Zero Knowledge)
                val passwordHash = kdfGenerator.hashPassword(password)
                
                val editor = prefs.edit()
                    .putBoolean("is_setup_complete", true)
                    .putString("master_password_hash", passwordHash)
                    .remove("master_password") // Security: Migrate old plaintext password if exists
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
