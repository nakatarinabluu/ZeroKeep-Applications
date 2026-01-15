package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale

import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.ui.secret.SecretViewModel
import com.vaultguard.app.ui.secret.SecretUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboard(
    onAddSecretClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SecretViewModel = hiltViewModel()
) {
    // Real Data
    val secrets by viewModel.secrets.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("vault_guard_prefs", android.content.Context.MODE_PRIVATE) }
    var isBiometricsEnabled by remember { mutableStateOf(prefs.getBoolean("biometrics_enabled", false)) }

    // Biometric Verification Logic
    val verifyBiometrics = {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // SUCCESS: Enable and Save
                    isBiometricsEnabled = true
                    prefs.edit().putBoolean("biometrics_enabled", true).apply()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Failed: Do nothing (remains disabled)
                }
            }
        )

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify Identity")
            .setSubtitle("Confirm biometrics to enable this feature")
            .setNegativeButtonText("Cancel")
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        viewModel.loadSecrets()
    }
    
    // SETTINGS DIALOG
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(text = "Settings") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Enable Biometric Login")
                        Switch(
                            checked = isBiometricsEnabled,
                            onCheckedChange = { shouldEnable ->
                                if (shouldEnable) {
                                    // REQUIRE VERIFICATION to Turn ON
                                    verifyBiometrics()
                                } else {
                                    // Turn OFF immediately
                                    isBiometricsEnabled = false
                                    prefs.edit().putBoolean("biometrics_enabled", false).apply()
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { onSignOut() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out", color = Color.Red)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "DANGER ZONE", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Divider(color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    var showPanicConfirm by remember { mutableStateOf(false) }
                    var panicToken by remember { mutableStateOf("") }

                    Button(
                        onClick = { showPanicConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚠️ WIPE VAULT", color = MaterialTheme.colorScheme.onError, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }

                    if (showPanicConfirm) {
                        AlertDialog(
                            onDismissRequest = { showPanicConfirm = false },
                            title = { Text("CONFIRM DESTRUCTION") },
                            text = {
                                Column {
                                    Text("This will permanently delete ALL data on the server and this device. This cannot be undone.", color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = panicToken,
                                        onValueChange = { panicToken = it },
                                        label = { Text("Enter Wipe Token") },
                                        singleLine = true
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.wipeVault(panicToken)
                                        showPanicConfirm = false
                                        showSettings = false
                                        onSignOut() // Exit app
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("DESTROY EVERYTHING", color = MaterialTheme.colorScheme.onError)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPanicConfirm = false }) {
                                    Text("Cancel")
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZeroKeep", color = MaterialTheme.colorScheme.onSurface) }, // Dark Text on Surface
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface), // Use Surface Color
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSecretClick, containerColor = MaterialTheme.colorScheme.primary) { 
                Icon(Icons.Default.Add, contentDescription = "Add Secret", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (secrets.isEmpty()) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) 
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Vault is empty. Add a secret!", color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) // Deep Navy Background
                    .padding(padding)
            ) {
                items(secrets.size) { index ->
                    SecretItem(item = secrets[index])
                }
            }
        }
    }
}
 
@Composable
fun SecretItem(item: SecretUiModel) {
    var revealed by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var ticks by remember { mutableIntStateOf(30) } // Countdown timer
    
    val scale by animateFloatAsState(
        targetValue = if (revealed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CardScale"
    )

    // RAM PURGE & CLIPBOARD CLEAR: Countdown and Auto-clear
    if (revealed) {
        DisposableEffect(Unit) {
            onDispose {
                revealed = false
                if (clipboardManager.getText()?.text == item.password) {
                     clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(""))
                }
            }
        }
        
        LaunchedEffect(Unit) {
            ticks = 30
            while (ticks > 0) {
                kotlinx.coroutines.delay(1000)
                ticks--
            }
            revealed = false
            if (clipboardManager.getText()?.text == item.password) {
                 clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(""))
            }
            System.gc()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .scale(scale)
            .clickable { 
                revealed = !revealed 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Row 1: Title and Username
            Column {
                Text(
                    text = item.title, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                if (item.username.isNotEmpty()) {
                    Text(
                        text = item.username,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Row 2: Password (Hidden/Revealed) + Copy + Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.animation.Crossfade(targetState = revealed, label = "PasswordReveal") { isRevealed ->
                    Text(
                        text = if (isRevealed) item.password else "••••••••••••",
                        color = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f), 
                        style = if (isRevealed) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (revealed) {
                        Text(
                            text = "${ticks}s", 
                            color = if (ticks < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, 
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        IconButton(onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.password))
                            // Optional: Show toast?
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
