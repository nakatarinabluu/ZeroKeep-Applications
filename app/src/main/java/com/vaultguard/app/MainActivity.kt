package com.vaultguard.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.vaultguard.app.ui.AddSecretScreen
import com.vaultguard.app.ui.AuthScreen
import com.vaultguard.app.ui.SetupScreen
import com.vaultguard.app.ui.VaultDashboard
import com.vaultguard.app.ui.theme.ZeroKeepTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @javax.inject.Inject lateinit var prefs: android.content.SharedPreferences

    @javax.inject.Inject lateinit var clipboardManager: com.vaultguard.app.utils.ClipboardManager

    @javax.inject.Inject lateinit var kdfGenerator: com.vaultguard.app.security.KdfGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. INTEGRITY CHECK (Root/Emulator Detection)
        // If device is compromised, we shutdown immediately to protect data.
        if (!BuildConfig.DEBUG &&
                        com.vaultguard.app.security.SecurityUtils.isDeviceCompromised(this)
        ) {
            // In a real banking app, we would show a Dialog explaining why.
            // For security, we crash/close silently or show a generic toast.
            android.util.Log.e("ZeroKeepOps", "❌ Integrity Check Failed! Device Compromised.")
            android.widget.Toast.makeText(
                            this,
                            "Security Violation: Unsupported Device Environment",
                            android.widget.Toast.LENGTH_LONG
                    )
                    .show()
            finishAffinity() // Close all activities
            System.exit(0) // Kill process
            return
        }

        // LIFECYCLE: Register Clipboard Limiter (Clears clipboard on background)
        lifecycle.addObserver(clipboardManager)

        // ANTI-FORENSICS: Global FLAG_SECURE to prevent screenshots and screen recording
        window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        )

        // CRASH REPORTING: Check if we have a pending crash from previous run
        checkAndUploadPendingCrash()

        setContent {
            ZeroKeepTheme {
                Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                ) {
                    val isSetupComplete = prefs.getBoolean("is_setup_complete", false)
                    android.util.Log.d(
                            "ZeroKeepOps",
                            "🚀 App Started. isSetupComplete: $isSetupComplete"
                    )
                    android.util.Log.d("ZeroKeepOps", "ℹ️ Integrity Check Passed.")
                    ZeroKeepApp(isSetupComplete, prefs, kdfGenerator)
                }
            }
        }
    }

    private fun checkAndUploadPendingCrash() {
        val pendingCrash = prefs.getString("pending_crash", null)
        if (pendingCrash != null) {
            android.util.Log.d("CrashReporter", "⚠️ Found pending crash report. Uploading...")

            // Use IO Thread for Network
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val json = JSONObject()
                    json.put("timestamp", System.currentTimeMillis())
                    json.put("device", android.os.Build.MODEL)
                    json.put("thread", "main") // Simplified
                    json.put("exception", "Crash Detected")
                    json.put("stacktrace", pendingCrash)

                    val body =
                            RequestBody.create(
                                    "application/json; charset=utf-8".toMediaTypeOrNull(),
                                    json.toString()
                            )

                    val request =
                            Request.Builder()
                                    .url("https://zerokeep.vercel.app/api/v1/logs")
                                    .post(body)
                                    .build()

                    // Create a one-off client (or inject if available, but this is robust enough)
                    val client = OkHttpClient()
                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        android.util.Log.i("CrashReporter", "✅ Crash Report Uploaded Successfully.")
                        prefs.edit().remove("pending_crash").apply()
                    } else {
                        android.util.Log.e(
                                "CrashReporter",
                                "❌ Failed to upload crash: ${response.code}"
                        )
                    }
                    response.close()
                } catch (e: Exception) {
                    android.util.Log.e("CrashReporter", "❌ Network error uploading crash", e)
                }
            }
        }
    }
}

// Extension to avoid import issues if not present
private fun String.toMediaTypeOrNull(): MediaType? {
    return try {
        MediaType.parse(this)
    } catch (_: Throwable) {
        null
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
            enterTransition = {
                androidx.compose.animation.fadeIn() +
                        androidx.compose.animation.slideInHorizontally()
            },
            exitTransition = {
                androidx.compose.animation.fadeOut() +
                        androidx.compose.animation.slideOutHorizontally()
            }
    ) {
        // Splash route removed to prevent "Triple Opening" glitch
        composable(
                route = "setup?restore={restore}",
                arguments =
                        listOf(
                                androidx.navigation.navArgument("restore") {
                                    type = androidx.navigation.NavType.BoolType
                                    defaultValue = false
                                }
                        )
        ) { backStackEntry ->
            val isRestore = backStackEntry.arguments?.getBoolean("restore") ?: false
            SetupScreen(
                    onSetupComplete = {
                        navController.navigate("auth") {
                            popUpTo("setup?restore={restore}") { inclusive = true }
                        }
                    },
                    initialRestoreMode = isRestore
            )
        }
        composable("auth") {
            AuthScreen(
                    onAuthenticated = {
                        navController.navigate("secret_graph") {
                            popUpTo("auth") { inclusive = true }
                        }
                    },
                    onReset = {
                        navController.navigate("setup?restore=true") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
            )
        }
        // Shared Graph for Secret Management
        navigation(startDestination = "dashboard", route = "secret_graph") {
            composable("dashboard") { entry ->
                // Scope ViewModel to the graph
                val parentEntry =
                        remember(entry) { navController.getBackStackEntry("secret_graph") }
                val secretViewModel =
                        hiltViewModel<com.vaultguard.app.ui.secret.SecretViewModel>(parentEntry)

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
                val parentEntry =
                        remember(entry) { navController.getBackStackEntry("secret_graph") }
                val secretViewModel =
                        hiltViewModel<com.vaultguard.app.ui.secret.SecretViewModel>(parentEntry)

                AddSecretScreen(
                        onBack = { navController.popBackStack() },
                        onSaved = {
                            // ViewModel already refreshed list internally via saveSecret ->
                            // loadSecrets
                            navController.popBackStack()
                        },
                        viewModel = secretViewModel // Pass shared instance
                )
            }
        }
    }
}
