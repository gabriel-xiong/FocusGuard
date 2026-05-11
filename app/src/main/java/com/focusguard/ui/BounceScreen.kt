package com.focusguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.session.SessionManager
import com.focusguard.ui.theme.EarnedColors
import kotlinx.coroutines.delay

@Composable
fun BounceScreen(onDismiss: () -> Unit) {
    val state by SessionManager.stateFlow.collectAsState()
    val blockedPkg = state.blockedPackageName
    val haptics = rememberHaptics()

    // Auto-dismiss after 3 seconds
    LaunchedEffect(blockedPkg) {
        if (blockedPkg != null) {
            delay(3000)
            SessionManager.clearBlockedApp()
            onDismiss()
        }
    }

    if (blockedPkg == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    LaunchedEffect(blockedPkg) {
        haptics.confirm()
    }

    val minutes = state.remainingSeconds / 60
    val seconds = state.remainingSeconds % 60
    val score = (state.attentionScore * 100f).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Blocked icon in a warm-red container
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = EarnedColors.Danger.copy(alpha = 0.1f),
            modifier = Modifier.size(100.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Block,
                    contentDescription = "Blocked",
                    tint = EarnedColors.Danger,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "App Blocked",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = EarnedColors.Danger
        )

        Spacer(Modifier.height(8.dp))

        // Try to get a friendly name for the package
        Text(
            blockedPkg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "This app is locked during your focus session.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Stats card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$score",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            score >= 70 -> EarnedColors.Focus
                            score >= 40 -> EarnedColors.Warning
                            else -> EarnedColors.Danger
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Vertical divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "%02d:%02d".format(minutes, seconds),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                haptics.confirm()
                SessionManager.clearBlockedApp()
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EarnedColors.Primary,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Back to Studying",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "Auto-returning in 3 seconds...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
