package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale

import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.ui.secret.SecretViewModel
import com.vaultguard.app.ui.secret.SecretUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboard(
    onAddSecretClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: SecretViewModel = hiltViewModel()
) {
    // Real Data
    val secrets by viewModel.secrets.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSecrets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZeroKeep", color = Color.Black) }, // Minimal
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White), // White Bar
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = Color.Black)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSecretClick, containerColor = Color(0xFF000000)) { // Pure Black FAB
                Icon(Icons.Default.Add, contentDescription = "Add Secret", tint = Color.White)
            }
        }
    ) { padding ->
        if (secrets.isEmpty()) {
             Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F7)) // Apple Gray Background
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Vault is empty. Add a secret!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF102027)) // Deep Navy Background
                    .padding(padding)
            ) {
                items(secrets.size) { index ->
                    SecretItem(item = secrets[index])
                }
            }
        }
    }
}

@Composable
fun SecretItem(item: SecretUiModel) {
    var revealed by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (revealed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CardScale"
    )

    // RAM PURGE & CLIPBOARD CLEAR: Countdown and Auto-clear
    if (revealed) {
        DisposableEffect(Unit) {
            onDispose {
                // If the user scrolls away or the app pauses, HIDE immediately
                // and CLEAR the clipboard for safety.
                revealed = false
                if (clipboardManager.getText()?.text == item.password) {
                     clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(""))
                }
            }
        }
        
        LaunchedEffect(Unit) {
            ticks = 30
            while (ticks > 0) {
                kotlinx.coroutines.delay(1000)
                ticks--
            }
            revealed = false
            if (clipboardManager.getText()?.text == item.password) {
                 clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(""))
            }
            System.gc() // Suggest immediate memory reclamation
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .scale(scale)
            .clickable { 
                revealed = !revealed 
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White) // Clean White Card
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.title, color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) 
                if (revealed) {
                    IconButton(onClick = {
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.password))
                    }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                            contentDescription = "Copy Password",
                            tint = Color.Black
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.animation.Crossfade(targetState = revealed, label = "PasswordReveal") { isRevealed ->
                    Text(
                        text = if (isRevealed) item.password else "••••••••",
                        color = if (isRevealed) Color(0xFF2962FF) else Color.Gray, // Accent Blue when revealed
                        style = if (isRevealed) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                if (revealed) {
                     androidx.compose.animation.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut()
                    ) {
                        Text(
                            text = "Hiding in ${ticks}s", 
                            color = if (ticks < 10) Color(0xFFEF5350) else Color.Gray, 
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
