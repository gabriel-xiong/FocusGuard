package com.focusguard.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.focusguard.R
import com.focusguard.session.SessionManager
import com.focusguard.ui.theme.EarnedColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val SetupTitleColor = Color(0xFF1F3D24)
private val SetupSubtitleColor = SetupTitleColor.copy(alpha = 0.65f)
private val SetupBorderColor = SetupTitleColor.copy(alpha = 0.10f)

private val QuickPickValues = listOf(30, 60, 90, 120)

data class AppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onBack: () -> Unit,
    onSessionStarted: (durationSeconds: Int) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val installedApps = remember {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val ai = resolveInfo.activityInfo ?: return@mapNotNull null
                val pkg = ai.packageName
                if (pkg == context.packageName) return@mapNotNull null
                if (pkg == "com.android.dialer") return@mapNotNull null
                if (pkg == "com.android.settings") return@mapNotNull null
                AppItem(pkg, ai.loadLabel(pm).toString(), ai.loadIcon(pm))
            }
            .sortedBy { it.label.lowercase() }
    }

    var selectedApps by remember { mutableStateOf(setOf<String>()) }
    var durationMinutes by remember { mutableIntStateOf(45) }
    var showCalibrationIntro by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    fun startSession() {
        val seconds = durationMinutes * 60
        SessionManager.startSession(
            durationSeconds = seconds,
            blacklistedApps = selectedApps.toList()
        )
        onSessionStarted(seconds)
    }

    Scaffold(
        containerColor = EarnedColors.LightBg,
        bottomBar = {
            if (!showCalibrationIntro) {
                BeginSessionBar(
                    durationMinutes = durationMinutes,
                    appsSelected = selectedApps.size,
                    onClick = {
                        haptics.confirm()
                        showCalibrationIntro = true
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 0.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TopBar(onBack = {
                        haptics.tap()
                        onBack()
                    })
                }
                item { Hero() }
                item {
                    DurationCard(
                        minutes = durationMinutes,
                        onMinutes = { newValue ->
                            if (newValue != durationMinutes) {
                                haptics.tick()
                                durationMinutes = newValue
                            }
                        }
                    )
                }
                item { AppsHeader(count = selectedApps.size) }
                items(installedApps, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        selected = app.packageName in selectedApps,
                        onToggle = {
                            haptics.select()
                            selectedApps = if (app.packageName in selectedApps)
                                selectedApps - app.packageName
                            else
                                selectedApps + app.packageName
                        }
                    )
                }
            }

            if (showCalibrationIntro) {
                CalibrationIntroOverlay(
                    durationMinutes = durationMinutes,
                    onBack = {
                        haptics.tap()
                        showCalibrationIntro = false
                    },
                    onNext = {
                        haptics.confirm()
                        startSession()
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SetupTitleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun Hero() {
    Column {
        Text(
            "Set your focus",
            fontFamily = FontFamily.Serif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = SetupTitleColor,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick how long, then choose what to lock.",
            fontSize = 15.sp,
            color = SetupSubtitleColor,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun CalibrationIntroOverlay(
    durationMinutes: Int,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EarnedColors.LightBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TopBar(onBack = onBack)
            }
            item {
                Column {
                    Text(
                        "Set up calibration",
                        fontFamily = FontFamily.Serif,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = SetupTitleColor,
                        lineHeight = 36.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Prop your phone where it can see you, then look at your normal work spot.",
                        fontSize = 15.sp,
                        color = SetupSubtitleColor,
                        lineHeight = 20.sp
                    )
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Image(
                        painter = painterResource(R.drawable.calibration_setup_guide),
                        contentDescription = "Phone positioned on a desk to calibrate focus tracking",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(390.dp)
                            .clip(RoundedCornerShape(30.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            item {
                CalibrationStepCard(
                    title = "Before you start",
                    steps = listOf(
                        "Place your phone upright beside your screen.",
                        "Keep your face and upper body in view.",
                        "Look at your work for a few seconds while it calibrates."
                    )
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clickable(onClick = onNext),
                    shape = RoundedCornerShape(20.dp),
                    color = EarnedColors.Primary,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Next: calibrate $durationMinutes-min session",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationStepCard(
    title: String,
    steps: List<String>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SetupBorderColor),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = SetupTitleColor
            )
            steps.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = EarnedColors.Focus.copy(alpha = 0.14f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EarnedColors.Focus
                            )
                        }
                    }
                    Text(
                        step,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = SetupSubtitleColor,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DurationCard(minutes: Int, onMinutes: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            EarnedColors.Primary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                "DURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = SetupSubtitleColor,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Crossfade(
                    targetState = minutes,
                    label = "durationValue"
                ) { mins ->
                    Text(
                        "$mins",
                        fontFamily = FontFamily.Serif,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = SetupTitleColor,
                        lineHeight = 64.sp
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "min",
                    fontSize = 18.sp,
                    color = SetupSubtitleColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Spacer(Modifier.weight(1f))
                EndsAt(minutes = minutes)
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = minutes.toFloat(),
                onValueChange = { onMinutes(it.toInt()) },
                valueRange = 5f..180f,
                steps = 34, // 5,10,...,180
                colors = SliderDefaults.colors(
                    thumbColor = EarnedColors.Primary,
                    activeTrackColor = EarnedColors.Primary,
                    inactiveTrackColor = EarnedColors.Primary.copy(alpha = 0.16f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickPickValues.forEach { value ->
                    QuickPick(
                        value = value,
                        selected = minutes == value,
                        modifier = Modifier.weight(1f),
                        onClick = { onMinutes(value) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EndsAt(minutes: Int) {
    val endLabel = remember(minutes) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, minutes)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = EarnedColors.Focus.copy(alpha = 0.12f)
    ) {
        Text(
            "Ends $endLabel",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = EarnedColors.Focus,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QuickPick(
    value: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) EarnedColors.Primary else Color.White,
        border = if (selected) null else BorderStroke(1.dp, SetupBorderColor),
        shadowElevation = if (selected) 3.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "$value",
                color = if (selected) Color.White else SetupTitleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AppsHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Lock these apps",
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = SetupTitleColor
        )
        if (count > 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = EarnedColors.Primary.copy(alpha = 0.14f)
            ) {
                Text(
                    "$count selected",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = EarnedColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: AppItem,
    selected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (selected) EarnedColors.Primary else SetupBorderColor
    val borderWidth = if (selected) 1.5.dp else 1.dp
    val containerColor = if (selected)
        EarnedColors.Primary.copy(alpha = 0.06f)
    else
        Color.White

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Text(
                app.label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SetupTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            CheckBadge(selected = selected)
        }
    }
}

@Composable
private fun CheckBadge(selected: Boolean) {
    if (selected) {
        Surface(
            modifier = Modifier.size(26.dp),
            shape = CircleShape,
            color = EarnedColors.Primary
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(SetupTitleColor.copy(alpha = 0.06f))
            )
        }
    }
}

@Composable
private fun BeginSessionBar(
    durationMinutes: Int,
    appsSelected: Int,
    onClick: () -> Unit
) {
    val enabled = appsSelected > 0
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        label = "ctaScale"
    )

    Surface(
        color = EarnedColors.LightBg,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                val released = tryAwaitRelease()
                                pressed = false
                                if (released) onClick()
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                color = if (enabled)
                    EarnedColors.Primary
                else
                    EarnedColors.Primary.copy(alpha = 0.35f),
                shadowElevation = if (enabled) 6.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (enabled) "Begin $durationMinutes-min session"
                        else "Pick at least one app to lock",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
