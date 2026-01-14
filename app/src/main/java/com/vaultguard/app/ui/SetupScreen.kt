package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.res.stringResource

@Composable
fun SetupScreen(onSetupComplete: (String) -> Unit) {
    var step by remember { mutableStateOf(SetupStep.WELCOME) }

    when (step) {
        SetupStep.WELCOME -> WelcomeContent(
            onNewWallet = { step = SetupStep.CREATE },
            onRestoreWallet = { step = SetupStep.RESTORE }
        )
        SetupStep.CREATE -> CreateWalletContent(onSetupComplete)
        SetupStep.RESTORE -> RestoreWalletContent(onSetupComplete)
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
fun CreateWalletContent(onSetupComplete: (String) -> Unit) {
    val mnemonic = remember {
        listOf("witch", "collapse", "practice", "feed", "shame", "open", "despair", "creek", "road", "again", "ice", "least")
    }
    SetupForm(
        title = stringResource(com.vaultguard.app.R.string.title_setup),
        instruction = "Write down these words. You will need them to recover your account.",
        mnemonic = mnemonic,
        isRestore = false,
        onSetupComplete = onSetupComplete
    )
}

@Composable
fun RestoreWalletContent(onSetupComplete: (String) -> Unit) {
    var inputPhrase by remember { mutableStateOf("") }
    // In a real app, we would validate these words against BIP39 list
    
    SetupForm(
        title = "Restore Vault",
        instruction = "Enter your 12-word recovery phrase to restore your access.",
        mnemonic = null, // No mnemonic to show, we input it
        isRestore = true,
        onSetupComplete = onSetupComplete
    )
}

@Composable
fun SetupForm(
    title: String,
    instruction: String,
    mnemonic: List<String>?,
    isRestore: Boolean,
    onSetupComplete: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var duressPassword by remember { mutableStateOf("") }
    var recoveryInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102027))
            .padding(24.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()), // Enable scrolling
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = instruction, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

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
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DURESS PASSWORD (Optional)
        OutlinedTextField(
            value = duressPassword,
            onValueChange = { duressPassword = it },
            label = { Text("Set Panic Password (Optional)") },
            placeholder = { Text("e.g. 0000") },
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF64B5F6),
                unfocusedBorderColor = Color.Red // Distinctive Color for Danger
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "⚠️ Entering this will wipe all data.",
            color = Color(0xFFE57373),
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isRestore && recoveryInput.split(" ").size < 12) {
                     error = "Please enter all 12 words separated by spaces."
                } else if (password.length < 6) {
                    error = "Password must be at least 6 characters"
                } else if (duressPassword.isNotEmpty() && duressPassword == password) {
                    error = "Panic Password cannot be the same as Master Password!"
                } else {
                    // Combine passwords with delimiter for simple passing: "master|duress"
                    val payload = if (duressPassword.isNotEmpty()) "$password|$duressPassword" else password
                    onSetupComplete(payload)
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
