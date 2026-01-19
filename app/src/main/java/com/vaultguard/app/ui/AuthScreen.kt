package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.ui.theme.BlueGradientEnd
import com.vaultguard.app.ui.theme.BlueGradientStart
import com.vaultguard.app.ui.theme.SoftCloud
import com.vaultguard.app.utils.BiometricHelper

@Composable
fun AuthScreen(
        onAuthenticated: () -> Unit,
        onReset: () -> Unit,
        viewModel: com.vaultguard.app.ui.auth.AuthViewModel = hiltViewModel()
) {
        val state by viewModel.authState.collectAsState()
        var password by remember { mutableStateOf("") }

        // Biometric Logic
        val context = LocalContext.current
        val activity = remember { context as? androidx.fragment.app.FragmentActivity }
        val biometricHelper = remember(activity) { activity?.let { BiometricHelper(it) } }

        val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

        // Logic to auto-trigger biometric if enabled
        LaunchedEffect(Unit) {
                if (isBiometricEnabled && biometricHelper?.canAuthenticate() == true) {
                        biometricHelper.showBiometricPrompt(
                                onSuccess = { viewModel.loginWithBiometrics() },
                                onError = { /* Ignore or show toast */}
                        )
                }
        }

        // Effect for successful login (nav)
        LaunchedEffect(state) {
                if (state is com.vaultguard.app.ui.auth.AuthState.Success) {
                        onAuthenticated()
                }
        }

        Box(
                modifier = Modifier.fillMaxSize().background(SoftCloud),
                contentAlignment = Alignment.Center
        ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                ) {
                        // App Logo
                        Box(
                                modifier =
                                        Modifier.size(100.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        Brush.linearGradient(
                                                                colors =
                                                                        listOf(
                                                                                BlueGradientStart,
                                                                                BlueGradientEnd
                                                                        )
                                                        )
                                                )
                                                .padding(24.dp),
                                contentAlignment = Alignment.Center
                        ) {
                                Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                                text = "Welcome Back",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = "Secure Vault Access",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Master Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                        OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BlueGradientEnd,
                                                unfocusedBorderColor = Color.LightGray,
                                                focusedLabelColor = BlueGradientEnd
                                        )
                        )

                        if (state is com.vaultguard.app.ui.auth.AuthState.Error) {
                                Text(
                                        text = "Incorrect Password", // simplified error msg
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp)
                                )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                                onClick = { viewModel.login(password) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = BlueGradientEnd
                                        )
                        ) {
                                Text(
                                        text = "Unlock Vault",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold
                                )
                        }

                        if (biometricHelper?.canAuthenticate() == true) {
                                Spacer(modifier = Modifier.height(16.dp))

                                IconButton(
                                        onClick = {
                                                biometricHelper.showBiometricPrompt(
                                                        onSuccess = {
                                                                viewModel.loginWithBiometrics()
                                                        },
                                                        onError = { /* Error toast? */}
                                                )
                                        },
                                        modifier = Modifier.size(64.dp)
                                ) {
                                        Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometric Login",
                                                tint = BlueGradientEnd,
                                                modifier = Modifier.size(48.dp)
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = onReset) {
                                Text("Forgot Password? Reset Vault", color = Color.Gray)
                        }
                }
        }
}
