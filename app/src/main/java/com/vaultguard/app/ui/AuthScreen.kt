package com.vaultguard.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.R
import com.vaultguard.app.ui.auth.AuthState
import com.vaultguard.app.ui.auth.AuthViewModel

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
    val isLoading = authState is AuthState.Loading
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val shakeOffset by remember { mutableFloatStateOf(0f) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Modern Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        com.vaultguard.app.ui.theme.BrandPurple,
                        com.vaultguard.app.ui.theme.BrandBlue
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Enter your master password to unlock.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0),
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Master Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                   val image = if (passwordVisible)
                       Icons.Filled.Visibility
                   else
                       Icons.Filled.VisibilityOff

                   IconButton(onClick = { passwordVisible = !passwordVisible }) {
                       Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                   }
                },
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.9f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                    errorContainerColor = Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (isError) {
                Text(
                    text = "Incorrect password. Attempts remaining: ${7 - attempts}",
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.attemptUnlock(password) },
                enabled = !isLoading && password.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = com.vaultguard.app.ui.theme.BrandPurple,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f),
                    disabledContentColor = com.vaultguard.app.ui.theme.BrandPurple.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                 if (isLoading) {
                     CircularProgressIndicator(
                         modifier = Modifier.size(24.dp),
                         color = com.vaultguard.app.ui.theme.BrandPurple,
                         strokeWidth = 2.dp
                     )
                 } else {
                     Text("Unlock Vault", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                 }
            }


                Spacer(modifier = Modifier.height(16.dp))

                // Reset Button
                TextButton(onClick = onReset) {
                    Text("Forgot Password? Reset Vault", color = com.vaultguard.app.ui.theme.AccentError)
                }

        }
    }

}
