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
        // Login Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo / Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(com.vaultguard.app.ui.theme.BackgroundLight, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = null,
                        tint = com.vaultguard.app.ui.theme.BrandPurple,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = com.vaultguard.app.ui.theme.TextPrimary
                )
                
                Text(
                    text = "Enter your master password to unlock.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.vaultguard.app.ui.theme.TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master Password") },
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
                        val image = if (passwordVisible) androidx.compose.material.icons.Icons.Filled.Visibility else androidx.compose.material.icons.Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null, tint = com.vaultguard.app.ui.theme.TextSecondary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Unlock Button
                Button(
                    onClick = { viewModel.verifyPassword(password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.vaultguard.app.ui.theme.BrandBlue),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Unlock Vault", fontSize = 18.sp, fontWeight = FontWeight.Bold)
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

}
