package com.focusguard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.ui.theme.EarnedColors

data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val EarnedBottomTabs = listOf(
    BottomTab("home", "Home", Icons.Filled.Home),
    BottomTab("insights", "Insights", Icons.Filled.BarChart),
    BottomTab("social", "Social", Icons.Filled.Groups),
    BottomTab("more", "More", Icons.Filled.MoreHoriz)
)

@Composable
fun EarnedBottomNav(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    val haptics = rememberHaptics()

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = EarnedColors.SidebarBg,
        contentColor = EarnedColors.SidebarFg,
        tonalElevation = 0.dp
    ) {
        EarnedBottomTabs.forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    haptics.select()
                    onTabSelected(tab.route)
                },
                icon = {
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.68f),
                    unselectedTextColor = Color.White.copy(alpha = 0.68f),
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                )
            )
        }
    }
}
