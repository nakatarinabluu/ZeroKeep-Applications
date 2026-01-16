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
import androidx.compose.material.icons.filled.Delete
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
    // Injected/Instantiated Utils
    val customClipboardManager = remember { com.vaultguard.app.utils.ClipboardManager(context) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // SECURE LIFECYCLE MANAGEMENT: Purge RAM & Clipboard on Background
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                // 1. Clear Clipboard immediately via Observer (Manual call redundant but safe)
                // customClipboardManager.clear() 
                
                // 2. Wipe Sensitive Data from RAM (ViewModel)
                viewModel.clearSensitiveData()
                
                // 3. Force Garbage Collection to remove artifacts
                System.gc()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        lifecycleOwner.lifecycle.addObserver(customClipboardManager) // Register Lifecycle-Aware Clipboard Manager
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            lifecycleOwner.lifecycle.removeObserver(customClipboardManager)
        }
    }

    // val prefs = remember { context.getSharedPreferences(...) } // REMOVED: Insecure
    val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsState()

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
                    viewModel.setBiometricEnabled(true)
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
                                    viewModel.setBiometricEnabled(false)
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
                    SecretItem(
                        item = secrets[index], 
                        clipboardManager = customClipboardManager,
                        onDelete = { id -> viewModel.deleteSecret(id) }
                    )
                }
            }
        }
    }
}
 
@Composable
fun SecretItem(
    item: SecretUiModel, 
    clipboardManager: com.vaultguard.app.utils.ClipboardManager,
    onDelete: (String) -> Unit
) {
    var revealed by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var ticks by remember { mutableIntStateOf(30) } // Countdown timer
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
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
            }
        }
        
        LaunchedEffect(Unit) {
            ticks = 30
            while (ticks > 0) {
                kotlinx.coroutines.delay(1000)
                ticks--
            }
            revealed = false
            System.gc()
        }
    }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Secret?") },
            text = { Text("Are you sure you want to delete '${item.title}'?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(item.id)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
            // Row 1: Header (Title, Username, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    // Username ALWAYS VISIBLE as requested
                    if (item.username.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.username,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                IconButton(onClick = { showDeleteConfirm = true }) {
                     Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            // Row 2: Password (Hidden/Revealed) + Copy + Timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Password",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    androidx.compose.animation.Crossfade(targetState = revealed, label = "PasswordReveal") { isRevealed ->
                        Text(
                            text = if (isRevealed) item.password else "••••••••••••",
                            color = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f), 
                            style = if (isRevealed) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                        )
                    }
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
                            clipboardManager.copyToClipboard("Secret", item.password)
                        }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                contentDescription = "Copy Password",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        // Show "Click to Reveal" hint icon or similar?
                        // For now just empty or verify if user wants anything.
                        // User said: "hidden word should only show password"
                    }
                }
            }
        }
    }
}
