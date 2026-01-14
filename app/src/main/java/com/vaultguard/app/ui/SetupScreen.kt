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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.vaultguard.app.security.MnemonicUtils

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102027))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo Icon
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = com.vaultguard.app.R.mipmap.ic_launcher),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to ZeroKeep",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Secure your digital life.",
            color = Color.Gray,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNewWallet,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Create New Vault", color = Color.White, fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRestoreWallet,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64B5F6)),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("I have a Recovery Phrase")
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
    onBack: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var recoveryInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102027))
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = instruction, color = Color.Gray, fontSize = 14.sp)
        
        // Show Regenerate Button if Creating Wallet
        if (!isRestore && mnemonic != null) {
            TextButton(onClick = onRegenerateMnemonic) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate New Phrase", color = Color(0xFF64B5F6))
            }
        } else if (isRestore) {
            Spacer(modifier = Modifier.height(16.dp))
        } else {
             Spacer(modifier = Modifier.height(16.dp))
        }

        if (!isRestore && mnemonic != null) {
            // SHOW MNEMONIC
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2D35))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    mnemonic.chunked(3).forEach { rowWords ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            rowWords.forEach { word ->
                                Text(text = word, color = Color(0xFF64B5F6), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(4.dp))
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
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF64B5F6),
                    unfocusedBorderColor = Color.Gray
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
            label = { Text("Set New Master Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible)
                     Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = null, tint = Color.Gray)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        // CONFIRM PASSWORD
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Master Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isRestore && recoveryInput.split(" ").size < 12) {
                     error = "Please enter all 12 words separated by spaces."
                } else if (password.length < 6) {
                    error = "Password must be at least 6 characters"
                } else if (password != confirmPassword) {
                    error = "Passwords do not match!"
                } else {
                    // Send ONLY password (Duress moved to settings)
                    onSetupComplete(password)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRestore) "Restore & Finish" else "Connect & Finish", color = Color.White)
        }
        
        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error!!, color = Color.Red)
        }
    }
}
