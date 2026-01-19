package com.vaultguard.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.security.MnemonicUtils
import com.vaultguard.app.ui.theme.*

@Composable
fun SetupScreen(
        onSetupComplete: () -> Unit,
        initialRestoreMode: Boolean = false,
        viewModel: SetupViewModel = hiltViewModel()
) {
        val state by viewModel.setupState.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        // Steps: 0 = Welcome/Choice, 1 = Form (Create or Restore)
        // If initialRestoreMode is true, we might want to skip directly or show Restore selected.
        // User requested "2 choices at start", so we enforce Step 0 unless we really want to skip.
        // However, if coming from "Reset", maybe stick to immediate restore?
        // Let's stick to the Welcome Page as requested for "First Time", but for Reset maybe
        // immediate?
        // User said "running for the first time... straight to login".
        // Let's implement the Welcome Page as default entry.

        var currentStep by remember { mutableStateOf(if (initialRestoreMode) 1 else 0) }
        var isRestoreMode by remember { mutableStateOf(initialRestoreMode) }

        // reset setup flag when entering this screen to fix the loop issue
        LaunchedEffect(Unit) { viewModel.prepareNewSetup() }

        var recoveryInput by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        val generatedMnemonic = remember { MnemonicUtils.generate() }
        val scrollState = rememberScrollState()

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .background(SoftCloud)
                                .padding(24.dp)
                                .verticalScroll(scrollState),
                contentAlignment = Alignment.Center
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                ) {
                        // -- LOGO --
                        Box(
                                modifier =
                                        Modifier.size(80.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(
                                                        brush =
                                                                Brush.linearGradient(
                                                                        colors =
                                                                                listOf(
                                                                                        BlueGradientStart,
                                                                                        BlueGradientEnd
                                                                                )
                                                                )
                                                ),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        painter =
                                                androidx.compose.ui.res.painterResource(
                                                        id = com.vaultguard.app.R.drawable.app_logo
                                                ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(56.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // -- TITLE --
                        Text(
                                text = "ZeroKeep",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = "Secure. Private. Yours.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // -- STEP 0: WELCOME / CHOICES --
                        if (currentStep == 0) {
                                // Create New Vault Button
                                Button(
                                        onClick = {
                                                isRestoreMode = false
                                                currentStep = 1
                                        },
                                        modifier = Modifier.fillMaxWidth().height(60.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = BlueGradientEnd,
                                                        contentColor = Color.White
                                                ),
                                        elevation = ButtonDefaults.buttonElevation(4.dp)
                                ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                                text = "Create New Vault",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Restore Button
                                OutlinedButton(
                                        onClick = {
                                                isRestoreMode = true
                                                currentStep = 1
                                        },
                                        modifier = Modifier.fillMaxWidth().height(60.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(2.dp, BlueGradientEnd),
                                        colors =
                                                ButtonDefaults.outlinedButtonColors(
                                                        contentColor = BlueGradientEnd
                                                )
                                ) {
                                        Icon(
                                                Icons.Default.Restore,
                                                contentDescription = null
                                        ) // Requires material-icons-extended, use Refresh if
                                        // missing
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                                text = "Restore Existing Vault",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        }
                        // -- STEP 1: FORM --
                        else {
                                // ... (Existing Card Logic) ...
                                // Back Button (Small)
                                TextButton(onClick = { currentStep = 0 }) {
                                        Text("< Back to Options", color = TextSecondary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                        colors =
                                                CardDefaults.cardColors(containerColor = PureWhite),
                                        elevation =
                                                CardDefaults.cardElevation(defaultElevation = 2.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                ) {
                                        Column(modifier = Modifier.padding(24.dp)) {
                                                // Header
                                                Text(
                                                        text =
                                                                if (isRestoreMode) "Restore Vault"
                                                                else "New Vault Setup",
                                                        style =
                                                                MaterialTheme.typography
                                                                        .titleMedium,
                                                        color = BlueGradientEnd,
                                                        fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))

                                                if (state is SetupState.Idle ||
                                                                state is SetupState.Error
                                                ) {
                                                        if (!isRestoreMode) {
                                                                // CREATE FLOW
                                                                Text(
                                                                        "Recovery Phrase",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleSmall,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                        "Save these words safely.",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color = TextSecondary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )

                                                                @OptIn(ExperimentalLayoutApi::class)
                                                                FlowRow(
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        ),
                                                                        verticalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        )
                                                                ) {
                                                                        generatedMnemonic
                                                                                .forEachIndexed {
                                                                                        index,
                                                                                        word ->
                                                                                        SuggestionChip(
                                                                                                onClick = {
                                                                                                },
                                                                                                label = {
                                                                                                        Text(
                                                                                                                "${index + 1}. $word"
                                                                                                        )
                                                                                                },
                                                                                                colors =
                                                                                                        SuggestionChipDefaults
                                                                                                                .suggestionChipColors(
                                                                                                                        containerColor =
                                                                                                                                SoftCloud
                                                                                                                )
                                                                                        )
                                                                                }
                                                                }
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                OutlinedButton(
                                                                        onClick = {
                                                                                val text =
                                                                                        generatedMnemonic
                                                                                                .joinToString(
                                                                                                        " "
                                                                                                )
                                                                                clipboardManager
                                                                                        .setText(
                                                                                                androidx.compose
                                                                                                        .ui
                                                                                                        .text
                                                                                                        .AnnotatedString(
                                                                                                                text
                                                                                                        )
                                                                                        )
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                ) { Text("Copy Phrase") }
                                                        } else {
                                                                // RESTORE FLOW
                                                                Text(
                                                                        "Enter Recovery Phrase",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .titleSmall,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                        "12 words separated by spaces",
                                                                        style =
                                                                                MaterialTheme
                                                                                        .typography
                                                                                        .bodySmall,
                                                                        color = TextSecondary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                )
                                                                OutlinedTextField(
                                                                        value = recoveryInput,
                                                                        onValueChange = {
                                                                                recoveryInput = it
                                                                        },
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .height(
                                                                                                120.dp
                                                                                        ),
                                                                        placeholder = {
                                                                                Text(
                                                                                        "abandon ability..."
                                                                                )
                                                                        },
                                                                        shape =
                                                                                RoundedCornerShape(
                                                                                        12.dp
                                                                                )
                                                                )
                                                        }

                                                        Divider(
                                                                modifier =
                                                                        Modifier.padding(
                                                                                vertical = 24.dp
                                                                        ),
                                                                color = LightSilver
                                                        )

                                                        // PASSWORDS
                                                        OutlinedTextField(
                                                                value = password,
                                                                onValueChange = { password = it },
                                                                label = { Text("Master Password") },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                visualTransformation =
                                                                        if (passwordVisible)
                                                                                VisualTransformation
                                                                                        .None
                                                                        else
                                                                                PasswordVisualTransformation(),
                                                                trailingIcon = {
                                                                        IconButton(
                                                                                onClick = {
                                                                                        passwordVisible =
                                                                                                !passwordVisible
                                                                                }
                                                                        ) {
                                                                                Icon(
                                                                                        if (passwordVisible
                                                                                        )
                                                                                                Icons.Default
                                                                                                        .Visibility
                                                                                        else
                                                                                                Icons.Default
                                                                                                        .VisibilityOff,
                                                                                        null
                                                                                )
                                                                        }
                                                                },
                                                                singleLine = true,
                                                                shape = RoundedCornerShape(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        OutlinedTextField(
                                                                value = confirmPassword,
                                                                onValueChange = {
                                                                        confirmPassword = it
                                                                },
                                                                label = {
                                                                        Text("Confirm Password")
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                visualTransformation =
                                                                        if (passwordVisible)
                                                                                VisualTransformation
                                                                                        .None
                                                                        else
                                                                                PasswordVisualTransformation(),
                                                                singleLine = true,
                                                                shape = RoundedCornerShape(12.dp)
                                                        )

                                                        Spacer(modifier = Modifier.height(32.dp))

                                                        Button(
                                                                onClick = {
                                                                        if (isRestoreMode) {
                                                                                val words =
                                                                                        recoveryInput
                                                                                                .trim()
                                                                                                .lowercase()
                                                                                                .split(
                                                                                                        Regex(
                                                                                                                "\\s+"
                                                                                                        )
                                                                                                )
                                                                                if (words.size ==
                                                                                                12 &&
                                                                                                password.isNotEmpty() &&
                                                                                                password ==
                                                                                                        confirmPassword
                                                                                ) {
                                                                                        viewModel
                                                                                                .restoreVault(
                                                                                                        password,
                                                                                                        words
                                                                                                )
                                                                                }
                                                                        } else {
                                                                                if (password.isNotEmpty() &&
                                                                                                password ==
                                                                                                        confirmPassword
                                                                                ) {
                                                                                        viewModel
                                                                                                .createVault(
                                                                                                        password,
                                                                                                        generatedMnemonic
                                                                                                )
                                                                                }
                                                                        }
                                                                },
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .height(56.dp),
                                                                shape = RoundedCornerShape(16.dp),
                                                                colors =
                                                                        ButtonDefaults.buttonColors(
                                                                                containerColor =
                                                                                        BlueGradientEnd,
                                                                                contentColor =
                                                                                        Color.White
                                                                        )
                                                        ) {
                                                                Text(
                                                                        if (isRestoreMode)
                                                                                "Restore Vault"
                                                                        else "Finish Setup",
                                                                        fontSize = 16.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                )
                                                        }
                                                } else if (state is SetupState.Loading) {
                                                        Box(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                CircularProgressIndicator(
                                                                        color = BlueGradientEnd
                                                                )
                                                        }
                                                } else if (state is SetupState.Success) {
                                                        LaunchedEffect(Unit) { onSetupComplete() }
                                                }

                                                if (state is SetupState.Error) {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Text(
                                                                (state as SetupState.Error).message,
                                                                color = AccentError,
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
