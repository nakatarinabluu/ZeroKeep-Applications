package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vaultguard.app.security.MnemonicUtils

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.*

@Composable
fun SetupScreen(onSetupComplete: (String) -> Unit) {
    var step by remember { mutableStateOf(SetupStep.WELCOME) }

    // Handle System Back Press? (Optional, but UI Back button is requested)
    
    when (step) {
        SetupStep.WELCOME -> WelcomeContent(
            onNewWallet = { step = SetupStep.CREATE },
            onRestoreWallet = { step = SetupStep.RESTORE }
        )
        SetupStep.CREATE -> CreateWalletContent(
            onSetupComplete = onSetupComplete,
            onBack = { step = SetupStep.WELCOME }
        )
        SetupStep.RESTORE -> RestoreWalletContent(
            onSetupComplete = onSetupComplete,
            onBack = { step = SetupStep.WELCOME }
        )
    }
}

enum class SetupStep { WELCOME, CREATE, RESTORE }

@Composable
fun WelcomeContent(onNewWallet: () -> Unit, onRestoreWallet: () -> Unit) {
    // Animation States
    val transitionState = remember { androidx.compose.animation.core.MutableTransitionState(false) }
    transitionState.targetState = true
    
    val transition = androidx.compose.animation.core.updateTransition(transitionState, label = "WelcomeEntrance")

    val logoScale by transition.animateFloat(
        transitionSpec = { androidx.compose.animation.core.tween(durationMillis = 800, easing = androidx.compose.animation.core.FastOutSlowInEasing) },
        label = "LogoScale"
    ) { if (it) 1f else 0.5f }

    val logoAlpha by transition.animateFloat(
        transitionSpec = { androidx.compose.animation.core.tween(durationMillis = 800) },
        label = "LogoAlpha"
    ) { if (it) 1f else 0f }
    
    val contentAlpha by transition.animateFloat(
        transitionSpec = { androidx.compose.animation.core.tween(durationMillis = 1000, delayMillis = 300) },
        label = "ContentAlpha"
    ) { if (it) 1f else 0f }

    val contentOffsetY by transition.animateDp(
        transitionSpec = { androidx.compose.animation.core.tween(durationMillis = 1000, delayMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing) },
        label = "ContentOffset"
    ) { if (it) 0.dp else 50.dp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon with Animation & Styling
        Box(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer(
                    scaleX = logoScale,
                    scaleY = logoScale,
                    alpha = logoAlpha
                )
                .shadow(elevation = 16.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary)
                .background(MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.vaultguard.app.R.mipmap.ic_launcher),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(120.dp) // Fill the box
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Column(
            modifier = Modifier.graphicsLayer(
                alpha = contentAlpha,
                translationY = contentOffsetY.value
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to ZeroKeep",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Secure your digital life with\nmilitary-grade encryption.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onNewWallet,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Create New Vault", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onRestoreWallet,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("I have a Recovery Phrase", fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CreateWalletContent(onSetupComplete: (String) -> Unit, onBack: () -> Unit) {
    // START WITH RANDOM MNEMONIC
    var mnemonic by remember { mutableStateOf(MnemonicUtils.generate()) }
    
    SetupForm(
        title = stringResource(com.vaultguard.app.R.string.title_setup),
        instruction = "Write down these words. You will need them to recover your account.",
        mnemonic = mnemonic,
        onRegenerateMnemonic = { mnemonic = MnemonicUtils.generate() },
        isRestore = false,
        onSetupComplete = onSetupComplete,
        onBack = onBack
    )
}

@Composable
fun RestoreWalletContent(onSetupComplete: (String) -> Unit, onBack: () -> Unit) {
    // In a real app, we would validate these words against BIP39 list
    
    SetupForm(
        title = "Restore Vault",
        instruction = "Enter your 12-word recovery phrase to restore your access.",
        mnemonic = null, // No mnemonic to show, we input it
        onRegenerateMnemonic = {},
        isRestore = true,
        onSetupComplete = onSetupComplete,
        onBack = onBack
    )
}

@Composable
fun SetupForm(
    title: String,
    instruction: String,
    mnemonic: List<String>?,
    onRegenerateMnemonic: () -> Unit,
    isRestore: Boolean,
    onSetupComplete: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SetupViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // State Observation
    val setupState by viewModel.setupState.collectAsState()
    var displayError by remember { mutableStateOf<String?>(null) }
    val isLoading = setupState is SetupState.Loading

    LaunchedEffect(setupState) {
        when (val state = setupState) {
            is SetupState.Success -> onSetupComplete(state.password)
            is SetupState.Error -> displayError = state.message
            else -> {}
        }
    }

    // Set displayError if local validation fails, clear it otherwise
    // Note: This needs to coexist with ViewModel errors. 
    // Simplified: local validation sets displayError, VM error overwrites it.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ... Header ...
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = instruction, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        
        // Show Regenerate Button if Creating Wallet
        if (!isRestore && mnemonic != null) {
            TextButton(onClick = onRegenerateMnemonic) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate New Phrase", color = MaterialTheme.colorScheme.primary)
            }
        } else if (isRestore) {
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Spacer(modifier = Modifier.height(16.dp))
        }

        if (!isRestore && mnemonic != null) {
            // SHOW MNEMONIC
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    mnemonic.chunked(3).forEach { rowWords ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowWords.forEach { word ->
                                Text(text = word, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // INPUT MNEMONIC
            OutlinedTextField(
                value = recoveryInput,
                onValueChange = { recoveryInput = it },
                label = { Text("Recovery Phrase (12 words)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 4
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MASTER PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { 
                Text(if (isRestore) "Enter Original Master Password" else "Set New Master Password") 
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // ANTI-FORENSICS: Disable AutoCorrect and use Text type to evade some Autofill crawlers
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                autoCorrect = false,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text, 
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
            trailingIcon = {
                val image = if (passwordVisible)
                     Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // CONFIRM PASSWORD (Only for New Vault)
        if (!isRestore) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Re-enter to Confirm") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        } else {
             Spacer(modifier = Modifier.height(24.dp))
        }

        Button(
            onClick = {
                if (isRestore && recoveryInput.split("\\s+".toRegex()).size != 12) {
                     displayError = "Please enter exactly 12 words."
                } else if (password.length < 6) {
                    displayError = "Password must be at least 6 characters"
                } else if (!isRestore && password != confirmPassword) {
                    displayError = "Passwords do not match!"
                } else {
                    // Start Verification Flow
                    displayError = null
                    
                    val words = if (isRestore) {
                        recoveryInput.trim().split("\\s+".toRegex())
                    } else {
                        mnemonic ?: emptyList()
                    }
                    
                    viewModel.completeSetup(password, words, isRestore)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isRestore) "Restore & Finish" else "Connect & Finish", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        
        if (displayError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = displayError!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
