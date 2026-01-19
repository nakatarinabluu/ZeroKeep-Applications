package com.vaultguard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaultguard.app.ui.theme.*

@Composable
fun GradientBanner(secretCount: Int, onRefresh: () -> Unit, onSettings: () -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(16.dp)
                            .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    spotColor = BlueGradientEnd.copy(alpha = 0.4f),
                                    ambientColor = BlueGradientEnd.copy(alpha = 0.2f)
                            )
                            .background(
                                    brush =
                                            Brush.horizontalGradient(
                                                    colors =
                                                            listOf(
                                                                    BlueGradientStart,
                                                                    BlueGradientEnd
                                                            )
                                            ),
                                    shape = RoundedCornerShape(28.dp)
                            )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Top Row (Title + Actions)
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Vault Protection",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.9f)
                )

                Row {
                    IconButton(onClick = onRefresh) {
                        Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = Color.White
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Balance / Count
            Text(
                    text = "Secured Assets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                        text = "$secretCount",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                        text = "Secrets",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
