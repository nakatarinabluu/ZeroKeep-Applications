package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.R
import com.vaultguard.app.ui.auth.AuthState
import com.vaultguard.app.ui.auth.AuthViewModel

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()
    
    // Determine UI state from ViewModel
    val attempts = when (val state = authState) {
        is AuthState.Error -> state.attemptsUsed
        else -> 0
    }
    
    val isWiped = authState is AuthState.Wiped
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
    
    // Trigger Biometrics ONCE on start
    LaunchedEffect(Unit) {
        triggerBiometrics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF102027)) // Deep Navy Background
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isError) 
                stringResource(R.string.msg_attempts_remaining, 7 - attempts) 
            else 
                stringResource(R.string.title_unlock), 
            color = if (attempts > 4) Color.Red else Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.label_master_password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = if (attempts > 4) Color.Red else Color(0xFF64B5F6), // Light Blue focus
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                viewModel.attemptUnlock(password)
            },
            colors = ButtonDefaults.buttonColors(containerColor = if (attempts > 4) Color.Red else Color(0xFF1976D2)), // Primary Blue
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_unlock), color = Color.White)
        }
        
        TextButton(onClick = { triggerBiometrics() }) {
            Text(stringResource(R.string.btn_biometrics), color = Color.Gray)
        }
    }
}
