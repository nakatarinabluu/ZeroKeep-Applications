package com.vaultguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vaultguard.app.ui.AddSecretScreen
import com.vaultguard.app.ui.AuthScreen
import com.vaultguard.app.ui.SetupScreen
import com.vaultguard.app.ui.VaultDashboard
import com.vaultguard.app.ui.theme.ZeroKeepTheme

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    val prefs = getSharedPreferences("vault_guard_prefs", MODE_PRIVATE)
                    val isSetupComplete = prefs.getBoolean("is_setup_complete", false)
                    ZeroKeepApp(isSetupComplete)
                }
            }
        }
    }
}

@Composable
fun ZeroKeepApp(isSetupComplete: Boolean) {
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
            val context = androidx.compose.ui.platform.LocalContext.current
            SetupScreen(onSetupComplete = { payload ->
                val prefs = context.getSharedPreferences("vault_guard_prefs", android.content.Context.MODE_PRIVATE)
                
                val parts = payload.split("|")
                val masterPassword = parts[0]
                val duressPassword = if (parts.size > 1) parts[1] else null
                
                val editor = prefs.edit()
                    .putBoolean("is_setup_complete", true)
                    .putString("master_password", masterPassword)

                if (duressPassword != null) {
                    editor.putString("duress_password", duressPassword)
                }

                editor.apply()
                
                navController.navigate("auth") {
                    popUpTo("setup") { inclusive = true }
                }
            })
        }
        composable("auth") {
            AuthScreen(onAuthenticated = {
                navController.navigate("dashboard") {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("dashboard") {
            VaultDashboard(
                onAddSecretClick = { navController.navigate("add_secret") },
                onSignOut = {
                    navController.navigate("auth") {
                        popUpTo("dashboard") { inclusive = true }
                    }
                }
            )
        }
        composable("add_secret") {
            AddSecretScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
    }
}
