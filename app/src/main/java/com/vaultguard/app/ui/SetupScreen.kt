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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultguard.app.security.MnemonicUtils

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
                .shadow(elevation = 16.dp, shape = androidx.compose.foundation.shape.CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                .background(MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.CircleShape)
                .clip(androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
             Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.vaultguard.app.R.drawable.ic_launcher_foreground),
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
                text = "Secure your digital world with\nZero Knowledge protection.",
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupForm(
    title: String,
    instruction: String,
    mnemonic: List<String>?,
    onRegenerateMnemonic: () -> Unit,
    isRestore: Boolean,
    onSetupComplete: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordMismatch by remember { mutableStateOf(false) }
    
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

    // Modern Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        com.vaultguard.app.ui.theme.BrandPurple,
                        com.vaultguard.app.ui.theme.BrandBlue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Back Button + Title)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = if (isRestore) "Restore Vault" else "Create Vault",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main Content Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    
                    if (displayError != null) {
                        Text(
                            text = displayError!!,
                            color = com.vaultguard.app.ui.theme.AccentError,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (isRestore) {
                        Text(
                            text = "Enter Recovery Phrase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.vaultguard.app.ui.theme.TextPrimary
                        )
                        Text(
                            text = "Enter your 12-word recovery phrase separated by spaces.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.vaultguard.app.ui.theme.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = recoveryInput,
                            onValueChange = { recoveryInput = it },
                            label = { Text("Recovery Phrase") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                                unfocusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                                focusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                                unfocusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                                cursorColor = com.vaultguard.app.ui.theme.BrandBlue,
                                focusedBorderColor = com.vaultguard.app.ui.theme.BrandBlue,
                                unfocusedBorderColor = Color.Transparent, 
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 4
                        )
                    } else {
                        // NEW VAULT CREATION
                        Text(
                            text = "Your Recovery Phrase",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.vaultguard.app.ui.theme.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mnemonic Display Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.vaultguard.app.ui.theme.BackgroundLight, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                           if (mnemonic != null) {
                               FlowRow(
                                   modifier = Modifier.fillMaxWidth(),
                                   horizontalArrangement = Arrangement.spacedBy(8.dp),
                                   verticalArrangement = Arrangement.spacedBy(8.dp)
                               ) {
                                   mnemonic.forEachIndexed { index, word ->
                                       AssistChip(
                                           onClick = {},
                                           label = { 
                                               Text(
                                                   "${index + 1}. $word", 
                                                   fontWeight = FontWeight.Medium,
                                                   color = com.vaultguard.app.ui.theme.TextPrimary
                                               ) 
                                           },
                                           colors = AssistChipDefaults.assistChipColors(containerColor = Color.White),
                                           border = null
                                       )
                                   }
                               }
                           } else {
                               CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                           }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                         TextButton(onClick = onRegenerateMnemonic) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Regenerate Phrase", color = com.vaultguard.app.ui.theme.BrandBlue)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Box(
                             modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) // Red-50
                                .padding(12.dp)
                        ) {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = com.vaultguard.app.ui.theme.AccentError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Write this down! It's the ONLY way to recover your vault.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.vaultguard.app.ui.theme.AccentError
                                )
                             }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Master Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.vaultguard.app.ui.theme.TextPrimary
                    )
                     Text(
                        text = "This password encrypts your vault locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.vaultguard.app.ui.theme.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Inputs
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Set Master Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                            unfocusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                            focusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                            unfocusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                            cursorColor = com.vaultguard.app.ui.theme.BrandBlue,
                            focusedBorderColor = com.vaultguard.app.ui.theme.BrandBlue,
                            unfocusedBorderColor = Color.Transparent,
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        trailingIcon = {
                             val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                             IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                 Icon(imageVector = image, contentDescription = null, tint = com.vaultguard.app.ui.theme.TextSecondary)
                             }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (!isRestore) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                passwordMismatch = password != it
                            },
                            label = { Text("Confirm Password") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            isError = passwordMismatch,
                             colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                                unfocusedTextColor = com.vaultguard.app.ui.theme.TextPrimary,
                                focusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                                unfocusedContainerColor = com.vaultguard.app.ui.theme.BackgroundLight,
                                cursorColor = com.vaultguard.app.ui.theme.BrandBlue,
                                focusedBorderColor = if (passwordMismatch) com.vaultguard.app.ui.theme.AccentError else com.vaultguard.app.ui.theme.BrandBlue,
                                unfocusedBorderColor = Color.Transparent,
                                errorBorderColor = com.vaultguard.app.ui.theme.AccentError
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (passwordMismatch) {
                            Text(
                                text = "Passwords do not match",
                                color = com.vaultguard.app.ui.theme.AccentError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

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
                                if (isRestore) {
                                    viewModel.restoreVault(password, recoveryInput)
                                } else {
                                    viewModel.createVault(password, mnemonic ?: emptyList())
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                         colors = ButtonDefaults.buttonColors(containerColor = com.vaultguard.app.ui.theme.BrandBlue),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                         if (isLoading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text(if (isRestore) "Restore Vault" else "Create Vault", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = instruction, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        
        // Show Regenerate Button if Creating Wallet
        if (!isRestore && mnemonic != null) {
            TextButton(onClick = onRegenerateMnemonic) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate New Phrase", color = MaterialTheme.colorScheme.secondary)
            }
        } else if (isRestore) {
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Spacer(modifier = Modifier.height(16.dp))
        }

        if (!isRestore && mnemonic != null) {
            // SHOW MNEMONIC: EXECUTIVE CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), // Surface Variant for contrast
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) { // More padding inside
                    mnemonic.chunked(3).forEachIndexed { i, rowWords ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            rowWords.forEachIndexed { j, word ->
                                val index = (i * 3) + j + 1
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(100.dp)) {
                                    Text(
                                        text = "$index.", 
                                        style = MaterialTheme.typography.labelSmall, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f),
                                        modifier = Modifier.width(20.dp)
                                    )
                                    Text(
                                        text = word, 
                                        color = MaterialTheme.colorScheme.onSurface, 
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        if (i < 3) Spacer(modifier = Modifier.height(12.dp))
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(140.dp),
                maxLines = 4
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // MASTER PASSWORD
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { 
                Text(if (isRestore) "Enter Original Master Password" else "Set New Master Password") 
            },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            // ANTI-FORENSICS
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                autoCorrect = false,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Password, 
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
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFF475569),
                cursorColor = Color(0xFF3B82F6),
                focusedLabelColor = Color(0xFF3B82F6),
                unfocusedLabelColor = Color(0xFF94A3B8)
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // CONFIRM PASSWORD (Only for New Vault)
        if (!isRestore) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    passwordMismatch = password != it
                },
                label = { Text("Re-enter to Confirm", color = Color(0xFF94A3B8)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = if (passwordMismatch) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF475569),
                    cursorColor = Color(0xFF3B82F6),
                    focusedLabelColor = if (passwordMismatch) Color(0xFFEF4444) else Color(0xFF3B82F6),
                    unfocusedLabelColor = Color(0xFF94A3B8)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordMismatch) {
                Text(
                    text = "Passwords do not match",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else {
             Spacer(modifier = Modifier.height(24.dp))
        }

        com.vaultguard.app.ui.components.VaultButton(
            text = if (isRestore) "Restore & Finish" else "Connect & Finish",
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
                        recoveryInput.trim().lowercase().split("\\s+".toRegex())
                    } else {
                        mnemonic ?: emptyList()
                    }

                    // Strict BIP39 Word Validation
                    val invalidWords = words.filter { !MnemonicUtils.isValidWord(it) }
                    
                    if (invalidWords.isNotEmpty()) {
                         displayError = "Invalid words found: ${invalidWords.joinToString(", ")}"
                    } else if (!MnemonicUtils.validateMnemonic(words)) {
                         displayError = "Invalid Checksum! The order of words is incorrect or words are invalid."
                    } else {
                        viewModel.completeSetup(password, words, isRestore)
                    }
                }
            },
            enabled = !isLoading,
            isLoading = isLoading
        )
        
        if (displayError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = displayError!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
