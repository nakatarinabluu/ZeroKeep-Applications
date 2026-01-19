package com.vaultguard.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.R
import com.vaultguard.app.ui.auth.AuthState
import com.vaultguard.app.ui.auth.AuthViewModel
import com.vaultguard.app.ui.components.VaultButton
import com.vaultguard.app.ui.components.VaultTextField

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    onReset: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    
    // Determine UI state from ViewModel
    val attempts = when (val state = authState) {
        is AuthState.Error -> state.attemptsUsed
        else -> 0
    }
    
    val isError = attempts > 0
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Biometric Logic (Extracted)
    val triggerBiometrics = {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            context as androidx.fragment.app.FragmentActivity,
            executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.biometricUnlockSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancels, we just stay on password screen
                }
            }
        )

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use Password")
            .build()
        // Check if hardware available before asking
        try {
           biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
           // Ignore if hardware unavailable
        }
    }

    // Side Effects
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onAuthenticated()
            viewModel.resetState()
        }
        if (authState is AuthState.Wiped) {
             throw SecurityException((authState as AuthState.Wiped).reason)
        }
    }
    
    // Trigger Biometrics ONCE on start (IF ENABLED and NOT skipped)
    LaunchedEffect(Unit) {
        if (viewModel.isBiometricEnabled && viewModel.shouldAutoPromptBiometrics) {
            triggerBiometrics()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Obsidian Navy
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 400.dp) // Constrain width on tablets
        ) {
            // Logo Area
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Placeholder or actual resource
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logo_large), // Ensure resource exists or fall back
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isError) 
                    stringResource(R.string.msg_attempts_remaining, 7 - attempts) 
                else 
                    stringResource(R.string.title_unlock), 
                style = MaterialTheme.typography.headlineMedium,
                color = if (attempts > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            VaultTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.label_master_password),
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = if (attempts > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
                // Note: VaultTextField needs VisualTransformation support update or we use raw here for password specifics?
                // Let's stick to raw OutlinedTextField here if we need specific password features not in VaultTextField yet,
                // OR update VaultTextField. For now, let's use raw to ensure PasswordVisualTransformation works securely.
            )
            
            // Actually, let's use the explicit OutlinedTextField for Password to ensure we control the VisualTransformation perfectly
            // replacing the VaultTextField call above for correctness in this specific security context.
        }
        
        // Re-composition with Raw Field for Password specifics
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 400.dp)
        ) {
             // Logo Area
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.scale(1.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

             Text(
                text = if (isError) 
                    stringResource(R.string.msg_attempts_remaining, 7 - attempts) 
                else 
                    stringResource(R.string.title_unlock), 
                style = MaterialTheme.typography.headlineSmall,
                color = if (attempts > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.label_master_password)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, // Corrected type
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = if (attempts > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            VaultButton(
                text = stringResource(R.string.btn_unlock),
                onClick = { viewModel.attemptUnlock(password) },
                containerColor = if (attempts > 4) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            
            if (viewModel.isBiometricEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                IconButton(
                    onClick = { triggerBiometrics() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_lock_idle_lock), // Stock lock icon as fallback or specific bio icon
                        contentDescription = "Biometrics",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Reset / Switch Account Logic
            var showResetConfirm by remember { mutableStateOf(false) }
            TextButton(onClick = { showResetConfirm = true }) {
                Text("Switch Account / Reset Wallet", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f), style = MaterialTheme.typography.bodySmall)
            }

            if (showResetConfirm) {
                AlertDialog(
                    onDismissRequest = { showResetConfirm = false },
                    title = { Text("Reset Wallet?") },
                    text = { Text("This will remove the current account from this device. You can Restore it later using your Recovery Phrase. Proceed?", color = MaterialTheme.colorScheme.onSurface) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.wipeLocalData()
                                onReset() 
                            }
                        ) {
                            Text("Reset", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetConfirm = false }) {
                            Text("Cancel")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}
