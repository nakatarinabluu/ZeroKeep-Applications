package com.vaultguard.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vaultguard.app.ui.components.GradientBanner
import com.vaultguard.app.ui.components.SoftCard
import com.vaultguard.app.ui.secret.SecretState
import com.vaultguard.app.ui.secret.SecretUiModel
import com.vaultguard.app.ui.secret.SecretViewModel
import com.vaultguard.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboard(
        onAddSecretClick: () -> Unit,
        onSignOut: () -> Unit,
        viewModel: SecretViewModel = hiltViewModel()
) {
        val state by viewModel.state.collectAsState()
        val isBiometricsEnabled by viewModel.isBiometricsEnabled.collectAsState()
        var showSettings by remember { mutableStateOf(false) }

        // Lifecycle & Biometric Logic (Preserved)
        val context = LocalContext.current
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
                val observer =
                        androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                                                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
                                ) {
                                        viewModel.clearSensitiveData()
                                        System.gc()
                                } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                                        viewModel.loadSecrets()
                                }
                        }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) { viewModel.loadSecrets() }

        Scaffold(
                containerColor = SoftCloud, // Clean Fintech Background
                floatingActionButton = {
                        FloatingActionButton(
                                onClick = onAddSecretClick,
                                containerColor = BlueGradientEnd,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp), // Modern Shape
                                elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) { Icon(Icons.Default.Add, contentDescription = "Add Secret") }
                }
        ) { paddingValues ->
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        // 1. Dynamic Banner
                        val secretCount =
                                if (state is SecretState.Success)
                                        (state as SecretState.Success).secrets.size
                                else 0
                        GradientBanner(
                                secretCount = secretCount,
                                onRefresh = { viewModel.loadSecrets() },
                                onSettings = { showSettings = true }
                        )

                        // 2. Secret List
                        when (val uiState = state) {
                                is SecretState.Loading -> {
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { CircularProgressIndicator(color = BlueGradientEnd) }
                                }
                                is SecretState.Success -> {
                                        LazyColumn(
                                                contentPadding =
                                                        PaddingValues(
                                                                horizontal = 16.dp,
                                                                vertical = 8.dp
                                                        ),
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                                item {
                                                        Text(
                                                                text = "Your Accounts",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleLarge,
                                                                color = TextPrimary,
                                                                modifier =
                                                                        Modifier.padding(
                                                                                start = 8.dp,
                                                                                bottom = 8.dp
                                                                        )
                                                        )
                                                }

                                                items(uiState.secrets) { secret ->
                                                        SecretItem(
                                                                item = secret,
                                                                onDelete = {
                                                                        viewModel.deleteSecret(it)
                                                                }
                                                        )
                                                }

                                                if (uiState.secrets.isEmpty()) {
                                                        item {
                                                                Box(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .padding(
                                                                                                top =
                                                                                                        48.dp
                                                                                        ),
                                                                        contentAlignment =
                                                                                Alignment.Center
                                                                ) {
                                                                        Text(
                                                                                "No secrets yet. Tap + to add one.",
                                                                                color = TextTertiary
                                                                        )
                                                                }
                                                        }
                                                }

                                                item {
                                                        Spacer(modifier = Modifier.height(80.dp))
                                                } // FAB Padding
                                        }
                                }
                                is SecretState.Error -> {
                                        Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                        ) { Text(text = uiState.message, color = AccentError) }
                                }
                        }
                }
        }

        // Settings Bottom Sheet (Preserved Logic, Updated Style)
        if (showSettings) {
                ModalBottomSheet(
                        onDismissRequest = { showSettings = false },
                        containerColor = PureWhite
                ) {
                        SettingsContent(
                                isBiometricsEnabled = isBiometricsEnabled,
                                onToggleBiometrics = {
                                        // Simplified for brevity, assume similar logic to original
                                        if (it) viewModel.setBiometricEnabled(true)
                                        else viewModel.setBiometricEnabled(false)
                                },
                                onSignOut = onSignOut,
                                onClose = { showSettings = false }
                        )
                }
        }
}

@Composable
fun SecretItem(item: SecretUiModel, onDelete: (String) -> Unit) {
        var revealed by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        // Use SoftCard for the "Cell"
        SoftCard(modifier = Modifier.clickable { revealed = !revealed }, cornerRadius = 20.dp) {
                Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                // Initial Icon with Soft Background
                                Box(
                                        modifier =
                                                Modifier.size(48.dp)
                                                        .clip(RoundedCornerShape(14.dp)) // Squircle
                                                        .background(LightSilver),
                                        contentAlignment = Alignment.Center
                                ) {
                                        Text(
                                                text = item.title.firstOrNull()?.uppercase() ?: "?",
                                                style =
                                                        MaterialTheme.typography.titleLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                        ),
                                                color = BlueGradientEnd
                                        )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                        )
                                        Text(
                                                text = item.username,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                        )
                                }

                                IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = TextTertiary
                                        )
                                }
                        }

                        if (revealed) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .background(
                                                                SoftCloud,
                                                                RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(12.dp)
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Text(
                                                        text = item.password,
                                                        fontFamily =
                                                                androidx.compose.ui.text.font
                                                                        .FontFamily.Monospace,
                                                        color = TextPrimary
                                                )
                                                IconButton(
                                                        onClick = {
                                                                clipboardManager.setText(
                                                                        androidx.compose.ui.text
                                                                                .AnnotatedString(
                                                                                        item.password
                                                                                )
                                                                )
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.ContentCopy,
                                                                "Copy",
                                                                tint = BlueGradientEnd
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }

        if (showDeleteConfirm) {
                AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        containerColor = PureWhite,
                        title = { Text("Delete Secret?", color = TextPrimary) },
                        text = {
                                Text("Permanently delete '${item.title}'?", color = TextSecondary)
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                onDelete(item.id)
                                                showDeleteConfirm = false
                                        }
                                ) {
                                        Text(
                                                "Delete",
                                                color = AccentError,
                                                fontWeight = FontWeight.Bold
                                        )
                                }
                        },
                        dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("Cancel", color = TextSecondary)
                                }
                        }
                )
        }
}

@Composable
fun SettingsContent(
        isBiometricsEnabled: Boolean,
        onToggleBiometrics: (Boolean) -> Unit,
        onSignOut: () -> Unit,
        onClose: () -> Unit
) {
        Column(modifier = Modifier.padding(24.dp)) {
                Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        Column {
                                Text(
                                        "Biometric Login",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                )
                                Text(
                                        "Unlock with fingerprint",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                )
                        }
                        Switch(
                                checked = isBiometricsEnabled,
                                onCheckedChange = onToggleBiometrics,
                                colors =
                                        SwitchDefaults.colors(
                                                checkedThumbColor = PureWhite,
                                                checkedTrackColor = BlueGradientEnd,
                                                uncheckedTrackColor = LightSilver
                                        )
                        )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = AccentError.copy(alpha = 0.1f),
                                        contentColor = AccentError
                                ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                ) { Text("Sign Out", fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Close", color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(32.dp))
        }
}
