package com.vaultguard.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                // RELOAD: If returning to foreground, ensure data is present
                viewModel.loadSecrets()
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
                         Text(
                            text = "ZeroKeep",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(onClick = { viewModel.loadSecrets() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Welcome / Stats
                    Text(
                        text = "Your Vault",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = when (state) {
                            is SecretState.Success -> "${(state as SecretState.Success).secrets.size} Secrets Secured"
                            else -> "Loading..."
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Secret List (Overlapping the header slightly)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (-20).dp) // Overlap effect
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(BackgroundLight)
                    .padding(horizontal = 16.dp)
            ) {
                when (val uiState = state) {
                    is SecretState.Loading -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = BrandBlue)
                        }
                    }
                    is SecretState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(uiState.secrets) { secret ->
                                SecretCard(item = secret, onDelete = { viewModel.deleteSecret(it) })
                            }
                        }
                    }
                    is SecretState.Error -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSecretClick,
                containerColor = BrandPurple,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Secret")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        // 2. Secret List (Overlapping the header slightly)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .offset(y = (-20).dp) // Overlap effect
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(BackgroundLight)
                .padding(horizontal = 16.dp)
        ) {
            when (val uiState = state) {
                is SecretState.Loading -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                }
                is SecretState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.secrets) { secret ->
                            SecretCard(item = secret, onDelete = { viewModel.deleteSecret(it) })
                        }
                    }
                }
                is SecretState.Error -> {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    
    // Bottom Sheet for Settings
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Settings", 
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Biometric Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Enable Biometric Login", color = TextPrimary)
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

                // Sign Out Option
                TextButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Out", color = AccentError)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { showSettings = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SecretCard(item: SecretUiModel, onDelete: (String) -> Unit) { 
    var revealed by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Modern White Card
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { revealed = !revealed },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon Placeholder
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = BackgroundLight,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.title.take(1).uppercase(),
                                color = BrandPurple,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (item.username.isNotEmpty()) {
                            Text(
                                text = item.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
                
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                     if (revealed) {
                        Text(
                            text = item.password,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = TextPrimary
                        )
                    } else {
                         Text(
                            text = "••••••••••••",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    
                    if (revealed) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.password))
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                             Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = BrandBlue
                            )
                        }
                    } else {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = TextSecondary.copy(alpha=0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Secret?", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${item.title}'?", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(item.id); showDeleteConfirm = false }) {
                    Text("Delete", color = AccentError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.White
        )
    }
}
```
