package com.vaultguard.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vaultguard.app.ui.theme.BlueGradientEnd
import com.vaultguard.app.ui.theme.PureWhite

@Composable
fun SoftCard(
        modifier: Modifier = Modifier,
        cornerRadius: Dp = 24.dp,
        elevation: Dp = 8.dp,
        content: @Composable () -> Unit
) {
    Surface(
            modifier =
                    modifier.fillMaxWidth()
                            .shadow(
                                    elevation = elevation,
                                    shape = RoundedCornerShape(cornerRadius),
                                    spotColor =
                                            BlueGradientEnd.copy(
                                                    alpha = 0.25f
                                            ), // Blue-tinted shadow
                                    ambientColor = BlueGradientEnd.copy(alpha = 0.1f)
                            ),
            shape = RoundedCornerShape(cornerRadius),
            color = PureWhite,
    ) { Box(modifier = Modifier.padding(16.dp)) { content() } }
}
