package com.focusguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.state.EarnedItStore
import com.focusguard.ui.theme.EarnedColors

@Composable
fun ResultsScreen(onDone: () -> Unit) {
    val uiState by EarnedItStore.state.collectAsState()
    val session = uiState.lastSession
    val haptics = rememberHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = if (session?.success == true) EarnedColors.Focus.copy(alpha = 0.12f) else EarnedColors.Warning.copy(alpha = 0.12f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (session?.success == true) EarnedColors.Focus else EarnedColors.Warning,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            if (session?.success == true) "Session complete" else "Session ended",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            if (session?.success == true) "You earned ${session.pointsEarned} points and ${session.timeBankMinutesEarned}m of bank time." else "Keep focusing to earn points!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(28.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultMetric(
                    icon = Icons.Filled.Star,
                    label = "POINTS",
                    value = "+${session?.pointsEarned ?: 0}",
                    color = EarnedColors.Points
                )
                ResultMetric(
                    icon = Icons.Filled.Timer,
                    label = "FOCUS",
                    value = "${session?.focusScore ?: 0}%",
                    color = EarnedColors.Focus
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                haptics.confirm()
                onDone()
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
            Icon(Icons.Filled.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Back home", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ResultMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
