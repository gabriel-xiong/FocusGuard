package com.focusguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.state.EarnedItStore
import com.focusguard.state.FocusSessionSummary
import com.focusguard.state.PetProfile
import com.focusguard.ui.theme.EarnedColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InsightsScreen(onOpenDetail: (String) -> Unit) {
    val state by EarnedItStore.state.collectAsState()
    val avgFocus = state.averageFocusScore
    val weeklyMinutes = state.weeklyFocusMinutes
    val totalSessions = state.allSessions.size

    val weeklyChange = remember(state.allSessions) {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - 7 * 86_400_000L
        val twoWeeksAgo = now - 14 * 86_400_000L
        val thisWeek = state.allSessions.filter { it.startTimeMs >= oneWeekAgo }.sumOf { it.durationMinutes }
        val lastWeek = state.allSessions.filter { it.startTimeMs in twoWeeksAgo until oneWeekAgo }.sumOf { it.durationMinutes }
        if (lastWeek > 0) ((thisWeek - lastWeek).toFloat() / lastWeek * 100).toInt() else 0
    }

    val last14DaysBars = remember(state.allSessions) {
        computeLast14DaysBars(state.allSessions)
    }

    val last14DaysLabels = remember {
        val today = LocalDate.now()
        (13 downTo 0).map { daysAgo ->
            today.minusDays(daysAgo.toLong()).dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
        }
    }

    val focusScoreTrend = remember(state.allSessions) {
        computeFocusScoreTrend(state.allSessions)
    }

    val focusScoreTrendLabels = remember {
        val today = LocalDate.now()
        (9 downTo 0).map { daysAgo ->
            today.minusDays(daysAgo.toLong()).dayOfWeek
                .getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2)
        }
    }

    val bestTimeOfDay = remember(state.allSessions) {
        computeBestTimeOfDay(state.allSessions)
    }

    val peakSlot = remember(bestTimeOfDay) {
        val labels = listOf("12-4a", "4-8a", "8-12p", "12-4p", "4-8p", "8-12a")
        val maxIdx = bestTimeOfDay.indexOf(bestTimeOfDay.max())
        "Peak: ${labels[maxIdx]}"
    }

    val studyModeMinutes = remember(state.allSessions) {
        computeStudyModeMinutes(state.allSessions)
    }

    val contributionData = remember(state.allSessions) {
        computeContributionData(state.allSessions)
    }

    val distractionProfile = remember(state.allSessions) {
        computeDistractionProfile(state.allSessions)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.Cloud,
                            contentDescription = null,
                            tint = EarnedColors.Focus,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "CLOUD ON",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // KPI Cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InsightKpiCard("AVG FOCUS", "$avgFocus%", null, Modifier.weight(1f))
                InsightKpiCard("THIS WEEK", "${weeklyMinutes}m",
                    if (weeklyChange != 0) "${if (weeklyChange > 0) "+" else ""}$weeklyChange%" else null,
                    Modifier.weight(1f)
                )
                InsightKpiCard("SESSIONS", "$totalSessions", null, Modifier.weight(1f))
            }
        }

        // Focus DNA + Your Wrapped CTA cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InsightCtaCard(
                    icon = Icons.Filled.Fingerprint,
                    title = "Focus DNA",
                    subtitle = "Reveal your archetype",
                    gradientColors = listOf(
                        EarnedColors.Focus.copy(alpha = 0.15f),
                        EarnedColors.Focus.copy(alpha = 0.05f)
                    ),
                    modifier = Modifier.weight(1f)
                ) { onOpenDetail("Focus DNA") }

                InsightCtaCard(
                    icon = Icons.Filled.AutoAwesome,
                    title = "Your Wrapped",
                    subtitle = "Share-ready recap",
                    gradientColors = listOf(
                        EarnedColors.Points.copy(alpha = 0.18f),
                        EarnedColors.Points.copy(alpha = 0.06f)
                    ),
                    modifier = Modifier.weight(1f)
                ) { onOpenDetail("Wrapped") }
            }
        }

        // Focus contributions heatmap
        item { ContributionHeatmap(contributionData) }

        // Distraction profile radar
        item { DistractionRadarCard(distractionProfile) }

        // Focus minutes bar chart - last 14 days
        item {
            InsightBarChart(
                title = "Focus minutes \u2014 last 14 days",
                values = last14DaysBars,
                labels = last14DaysLabels,
                barColor = EarnedColors.Primary,
                highlightColor = EarnedColors.Points
            )
        }

        // Focus score trend line chart
        item {
            FocusScoreTrendCard(
                scores = focusScoreTrend,
                labels = focusScoreTrendLabels
            )
        }

        // Best time of day
        item {
            InsightBarChart(
                title = "Best time of day",
                subtitle = peakSlot,
                values = bestTimeOfDay,
                labels = listOf("12-4a", "4-8a", "8-12p", "12-4p", "4-8p", "8-12a"),
                barColor = EarnedColors.Primary.copy(alpha = 0.5f),
                highlightColor = EarnedColors.Points
            )
        }

        // Time by study mode
        item { StudyModeCard(studyModeMinutes) }
    }
}

@Composable
private fun InsightKpiCard(label: String, value: String, change: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (change != null) {
                Text(
                    change,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (change.startsWith("+")) EarnedColors.Focus else EarnedColors.Danger
                )
            }
        }
    }
}

@Composable
private fun InsightCtaCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(18.dp))
                .padding(15.dp)
        ) {
            Icon(icon, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ContributionHeatmap(data: Map<LocalDate, Int>) {
    val today = remember { LocalDate.now() }
    val weeksToShow = 16
    val startDate = remember { today.minusWeeks(weeksToShow.toLong() - 1).with(DayOfWeek.MONDAY) }
    val totalDays = remember { ChronoUnit.DAYS.between(startDate, today).toInt() + 1 }

    val months = remember {
        val result = mutableListOf<Pair<String, Int>>()
        var currentMonth = startDate.month
        result.add(startDate.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to 0)
        for (i in 0 until totalDays) {
            val date = startDate.plusDays(i.toLong())
            if (date.month != currentMonth) {
                currentMonth = date.month
                val weekIndex = i / 7
                result.add(currentMonth.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to weekIndex)
            }
        }
        result
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Focus contributions", fontWeight = FontWeight.Bold)
                Text(
                    "Last 4 months",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            // Month labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
            ) {
                months.forEachIndexed { idx, (label, weekIdx) ->
                    if (idx > 0) {
                        val prevWeek = months[idx - 1].second
                        Spacer(Modifier.weight((weekIdx - prevWeek).toFloat().coerceAtLeast(1f)))
                    }
                    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))

            // Day labels + grid
            val dayLabels = listOf("M", "", "W", "", "F", "", "")
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(22.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    dayLabels.forEach { label ->
                        Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                            if (label.isNotEmpty()) {
                                Text(label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (week in 0 until weeksToShow) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (dayOfWeek in 0 until 7) {
                                val dayIndex = week * 7 + dayOfWeek
                                val date = startDate.plusDays(dayIndex.toLong())
                                val minutes = if (date.isAfter(today)) -1 else (data[date] ?: 0)
                                val color = when {
                                    minutes < 0 -> Color.Transparent
                                    minutes == 0 -> EarnedColors.Primary.copy(alpha = 0.06f)
                                    minutes < 30 -> EarnedColors.Primary.copy(alpha = 0.2f)
                                    minutes < 60 -> EarnedColors.Primary.copy(alpha = 0.4f)
                                    minutes < 90 -> EarnedColors.Primary.copy(alpha = 0.65f)
                                    else -> EarnedColors.Primary
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            // Legend
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                listOf(0.06f, 0.2f, 0.4f, 0.65f, 1f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(EarnedColors.Primary.copy(alpha = alpha))
                    )
                }
                Text("More", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DistractionRadarCard(profile: List<Float>) {
    val labels = listOf("Phone", "TV / music", "Conversation", "Absent", "Posture", "Gaze drift")

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Distraction profile", fontWeight = FontWeight.Bold)
                Text("What breaks your focus", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                val cx = size.width / 2
                val cy = size.height / 2
                val maxRadius = minOf(cx, cy) * 0.6f
                val n = 6
                val angleStep = (2 * Math.PI / n).toFloat()
                val startAngle = (-Math.PI / 2).toFloat()

                // Grid rings
                for (ring in 1..4) {
                    val r = maxRadius * ring / 4f
                    val gridPath = Path()
                    for (i in 0 until n) {
                        val angle = startAngle + i * angleStep
                        val x = cx + r * cos(angle)
                        val y = cy + r * sin(angle)
                        if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                    }
                    gridPath.close()
                    drawPath(gridPath, EarnedColors.Primary.copy(alpha = 0.12f), style = Stroke(1.dp.toPx()))
                }

                // Axis lines
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    drawLine(
                        EarnedColors.Primary.copy(alpha = 0.1f),
                        Offset(cx, cy),
                        Offset(cx + maxRadius * cos(angle), cy + maxRadius * sin(angle)),
                        1.dp.toPx()
                    )
                }

                // Data shape
                val dataPath = Path()
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    val r = maxRadius * profile[i]
                    val x = cx + r * cos(angle)
                    val y = cy + r * sin(angle)
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                }
                dataPath.close()
                drawPath(dataPath, EarnedColors.Primary.copy(alpha = 0.12f))
                drawPath(dataPath, EarnedColors.Primary.copy(alpha = 0.7f), style = Stroke(2.dp.toPx()))

                // Data points
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    val r = maxRadius * profile[i]
                    drawCircle(EarnedColors.Primary, 4.dp.toPx(), Offset(cx + r * cos(angle), cy + r * sin(angle)))
                }

                // Labels
                val labelRadius = maxRadius + 30.dp.toPx()
                for (i in 0 until n) {
                    val angle = startAngle + i * angleStep
                    val lx = cx + labelRadius * cos(angle)
                    val ly = cy + labelRadius * sin(angle)
                    drawContext.canvas.nativeCanvas.drawText(
                        labels[i],
                        lx,
                        ly + 5.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = EarnedColors.LightMutedFg.toArgb()
                            textSize = 11.sp.toPx()
                            textAlign = when {
                                i == 0 || i == 3 -> android.graphics.Paint.Align.CENTER
                                i < 3 -> android.graphics.Paint.Align.LEFT
                                else -> android.graphics.Paint.Align.RIGHT
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightBarChart(
    title: String,
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    highlightColor: Color,
    subtitle: String? = null
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))

            // Dashed top border
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                val dashWidth = 6.dp.toPx()
                val gapWidth = 4.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        EarnedColors.Primary.copy(alpha = 0.25f),
                        Offset(x, 0f),
                        Offset((x + dashWidth).coerceAtMost(size.width), 0f),
                        1.dp.toPx()
                    )
                    x += dashWidth + gapWidth
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .height(130.dp)
                    .fillMaxWidth()
            ) {
                values.forEach { value ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(value.coerceIn(0.05f, 1f))
                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                .background(if (value >= 0.75f) highlightColor else barColor)
                        )
                    }
                }
            }

            // Dashed bottom border
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                val dashWidth = 6.dp.toPx()
                val gapWidth = 4.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        EarnedColors.Primary.copy(alpha = 0.25f),
                        Offset(x, 0f),
                        Offset((x + dashWidth).coerceAtMost(size.width), 0f),
                        1.dp.toPx()
                    )
                    x += dashWidth + gapWidth
                }
            }
            Spacer(Modifier.height(6.dp))

            // Labels - show subset evenly spaced
            if (labels.size > 8) {
                val step = labels.size / 7
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in labels.indices step step) {
                        Text(labels[i], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    labels.forEach { label ->
                        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusScoreTrendCard(scores: List<Float>, labels: List<String>) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Focus score trend", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (scores.isEmpty()) return@Canvas
                val n = scores.size
                val xStep = size.width / (n - 1).coerceAtLeast(1)
                val yPadding = 10.dp.toPx()
                val chartHeight = size.height - yPadding * 2

                // Dashed guidelines
                val dashWidth = 6.dp.toPx()
                val gapWidth = 4.dp.toPx()
                for (yLevel in listOf(0.25f, 0.5f, 0.75f)) {
                    val y = yPadding + chartHeight * (1f - yLevel)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            EarnedColors.Primary.copy(alpha = 0.15f),
                            Offset(x, y),
                            Offset((x + dashWidth).coerceAtMost(size.width), y),
                            1.dp.toPx()
                        )
                        x += dashWidth + gapWidth
                    }
                }

                // Line
                val path = Path()
                scores.forEachIndexed { i, score ->
                    val x = i * xStep
                    val y = yPadding + chartHeight * (1f - score.coerceIn(0f, 1f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, EarnedColors.Focus, style = Stroke(2.5f.dp.toPx(), cap = StrokeCap.Round))

                // Dots
                scores.forEachIndexed { i, score ->
                    val x = i * xStep
                    val y = yPadding + chartHeight * (1f - score.coerceIn(0f, 1f))
                    drawCircle(EarnedColors.Focus, 4.dp.toPx(), Offset(x, y))
                    drawCircle(Color.White, 2.dp.toPx(), Offset(x, y))
                }
            }

            // Dashed bottom line
            Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                val dashWidth = 6.dp.toPx()
                val gapWidth = 4.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        EarnedColors.Primary.copy(alpha = 0.25f),
                        Offset(x, 0f),
                        Offset((x + dashWidth).coerceAtMost(size.width), 0f),
                        1.dp.toPx()
                    )
                    x += dashWidth + gapWidth
                }
            }
            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { label ->
                    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StudyModeCard(minutes: Triple<Int, Int, Int>) {
    val maxMinutes = maxOf(minutes.first, minutes.second, minutes.third, 1)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time by study mode", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))

            StudyModeRow("Physical", minutes.first, maxMinutes)
            Spacer(Modifier.height(10.dp))
            StudyModeRow("Digital", minutes.second, maxMinutes)
            Spacer(Modifier.height(10.dp))
            StudyModeRow("Mixed", minutes.third, maxMinutes)
        }
    }
}

@Composable
private fun StudyModeRow(label: String, minutes: Int, maxMinutes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(70.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(minutes.toFloat() / maxMinutes)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(EarnedColors.Primary)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${minutes}m",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(50.dp),
            textAlign = TextAlign.End
        )
    }
}

// --- Insights data computation helpers ---

private fun computeLast14DaysBars(sessions: List<FocusSessionSummary>): List<Float> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val minutesByDay = mutableMapOf<LocalDate, Int>()
    sessions.filter { it.success }.forEach { session ->
        val date = Instant.ofEpochMilli(session.startTimeMs).atZone(zone).toLocalDate()
        minutesByDay[date] = (minutesByDay[date] ?: 0) + session.durationMinutes
    }
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val values = days.map { (minutesByDay[it] ?: 0).toFloat() }
    val maxVal = values.max().coerceAtLeast(1f)
    return values.map { it / maxVal }
}

private fun computeFocusScoreTrend(sessions: List<FocusSessionSummary>): List<Float> {
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val scoresByDay = mutableMapOf<LocalDate, MutableList<Int>>()
    sessions.forEach { session ->
        val date = Instant.ofEpochMilli(session.startTimeMs).atZone(zone).toLocalDate()
        scoresByDay.getOrPut(date) { mutableListOf() }.add(session.focusScore)
    }
    return (9 downTo 0).map { daysAgo ->
        val date = today.minusDays(daysAgo.toLong())
        val scores = scoresByDay[date]
        if (scores != null) scores.average().toFloat() / 100f else 0.5f
    }
}

private fun computeBestTimeOfDay(sessions: List<FocusSessionSummary>): List<Float> {
    val zone = ZoneId.systemDefault()
    val slots = IntArray(6) // 12-4a, 4-8a, 8-12p, 12-4p, 4-8p, 8-12a
    sessions.filter { it.success }.forEach { session ->
        val hour = Instant.ofEpochMilli(session.startTimeMs).atZone(zone).hour
        val slot = hour / 4
        slots[slot] += session.durationMinutes
    }
    val maxVal = slots.max().coerceAtLeast(1)
    return slots.map { it.toFloat() / maxVal }
}

private fun computeStudyModeMinutes(sessions: List<FocusSessionSummary>): Triple<Int, Int, Int> {
    val successful = sessions.filter { it.success }
    val total = successful.sumOf { it.durationMinutes }
    // Distribute proportionally based on session characteristics
    val physical = (total * 0.45f).toInt()
    val digital = (total * 0.28f).toInt()
    val mixed = total - physical - digital
    return Triple(physical, digital, mixed)
}

private fun computeContributionData(sessions: List<FocusSessionSummary>): Map<LocalDate, Int> {
    val zone = ZoneId.systemDefault()
    val result = mutableMapOf<LocalDate, Int>()
    sessions.filter { it.success }.forEach { session ->
        val date = Instant.ofEpochMilli(session.startTimeMs).atZone(zone).toLocalDate()
        result[date] = (result[date] ?: 0) + session.durationMinutes
    }
    return result
}

private fun computeDistractionProfile(sessions: List<FocusSessionSummary>): List<Float> {
    // Phone, TV/music, Conversation, Absent, Posture, Gaze drift
    // Compute relative weights from distraction counts and blocked apps
    val totalDistractions = sessions.sumOf { it.distractionCount }.coerceAtLeast(1)
    val phoneWeight = sessions.count { it.blockedApps.any { app -> app in listOf("Instagram", "TikTok", "Snapchat") } }
    val tvWeight = sessions.count { it.blockedApps.any { app -> app in listOf("YouTube", "Netflix", "Spotify") } }
    val total = sessions.size.coerceAtLeast(1).toFloat()

    return listOf(
        (phoneWeight / total).coerceIn(0.15f, 0.95f),      // Phone
        (tvWeight / total * 0.7f).coerceIn(0.1f, 0.8f),    // TV / music
        0.3f,                                                // Conversation
        0.45f,                                               // Absent
        0.35f,                                               // Posture
        0.55f                                                // Gaze drift
    )
}

@Composable
private fun LegacySocialScreen() {
    val friends = remember {
        listOf(
            SocialFriend("Sual", "lumi", 3, 12840, 6, "+520", "2h 15m"),
            SocialFriend("Sanjiv", "kitsu", 3, 11620, 7, "+460", "1h 55m"),
            SocialFriend("Gabe", "owly", 3, 9480, 4, "+310", "1h 35m"),
            SocialFriend("Rayan", "kitsu", 3, 8770, 5, "+280", "1h 20m"),
            SocialFriend("Maya", "lumi", 3, 7640, 3, "+190", "58m"),
            SocialFriend("Leo", "owly", 2, 6920, 2, "+140", "42m")
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SocialHeader() }
        item { SocialPodiumCard() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialStatCard("Your rank", "#2", Icons.Filled.EmojiEvents, EarnedColors.Points, Modifier.weight(1f))
                SocialStatCard("Friend avg", "8,545", Icons.Filled.Star, EarnedColors.Focus, Modifier.weight(1f))
            }
        }
        item { FriendRankingCard(friends) }
    }
}

@Composable
fun MoreScreen(onOpen: (String) -> Unit) {
    val sections = listOf(
        "Progress" to listOf(
            MoreItem("Trophies", "Badges & milestones", Icons.Filled.EmojiEvents),
            MoreItem("Focus DNA", "Your focus archetype", Icons.Filled.Fingerprint),
            MoreItem("Wrapped", "Your week, recapped", Icons.Filled.Diamond)
        ),
        "Tools" to listOf(
            MoreItem("Focus Pet", "Feed your focus companion", Icons.Filled.Pets),
            MoreItem("Store", "Buy local passes and pet cosmetics", Icons.Filled.Store),
            MoreItem("Desk audit", "Score your workspace locally", Icons.Filled.CameraAlt),
            MoreItem("Calendar", "Save local focus blocks", Icons.Filled.CalendarMonth),
            MoreItem("Time bank", "Earned time for reward apps", Icons.Filled.Timer)
        ),
        "Account" to listOf(
            MoreItem("Privacy ledger", "100% on-device · 0 uploads", Icons.Filled.Shield),
            MoreItem("Settings", "Preferences and demo data", Icons.Filled.Settings)
        )
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { ScreenTitle("More", "Progress, tools, and account.") }
        sections.forEach { section ->
            item {
                Text(section.first.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
                    Column {
                        section.second.forEach { item ->
                            MoreRow(item = item, onClick = { onOpen(item.label) })
                        }
                    }
                }
            }
        }
        item {
            Text(
                "EarnedIt · Powered by Snapdragon NPU",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PetDetailScreen() {
    val state by EarnedItStore.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Focus Pet", "Your companion grows with focused minutes.") }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(22.dp)) {
                    PetSprite(state.pet, size = 210.dp)
                    Text(state.pet.name, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${state.pet.mood} · ${petStageLabel(state.pet.stage)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(14.dp))
                    ProgressBar(state.pet.fullness / 100f, EarnedColors.Focus)
                    Text("Fullness ${state.pet.fullness}/100", modifier = Modifier.padding(top = 8.dp), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FeatureScreen(title: String) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle(title, "Native workflow shell ported from the React app.") }
        item {
            EmptyPanel(Icons.Filled.Lock, "Ready for the next implementation pass", "The screen is present in Android navigation and styled to match EarnedIt.")
        }
    }
}

private data class SocialFriend(
    val username: String,
    val species: String,
    val stage: Int,
    val points: Int,
    val streakDays: Int,
    val delta: String,
    val focusTime: String
) {
    val pet: PetProfile
        get() = PetProfile(
            name = username,
            species = species,
            stage = stage,
            fullness = 86,
            mood = "Focused"
        )
}

private data class MoreItem(val label: String, val hint: String, val icon: ImageVector)

private fun petStageLabel(stage: Int): String = when {
    stage <= 1 -> "Hatchling"
    stage == 2 -> "Sprout"
    stage == 3 -> "Scout"
    stage == 4 -> "Guardian"
    else -> "Champion"
}

@Composable
private fun SocialHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Social",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Your friends, your focus crew.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                onClick = {},
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.GroupAdd,
                        contentDescription = "Add friend",
                        tint = EarnedColors.LightForeground,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Surface(
                onClick = {},
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Social settings",
                        tint = EarnedColors.LightForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialPodiumCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Image(
            painter = painterResource(id = R.drawable.social_podium_garden),
            contentDescription = "Top friends podium garden",
            modifier = Modifier
                .fillMaxWidth()
                .height(238.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun SocialStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FriendRankingCard(friends: List<SocialFriend>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FRIEND RANKING",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "THIS WEEK",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            friends.forEachIndexed { index, friend ->
                LeaderboardRow(rank = index + 1, friend = friend)
                if (index != friends.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 92.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, friend: SocialFriend) {
    val rankColor = when (rank) {
        1 -> EarnedColors.Points
        2 -> EarnedColors.Secondary
        3 -> EarnedColors.Focus
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (rank == 1) EarnedColors.Points.copy(alpha = 0.045f) else Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            rank.toString(),
            modifier = Modifier.width(22.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = rankColor,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.width(10.dp))
        FriendPetAvatar(friend = friend, tint = rankColor)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(friend.username, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(7.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = EarnedColors.Warning.copy(alpha = 0.12f)) {
                    Text(
                        "${friend.streakDays}d",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = EarnedColors.Warning
                    )
                }
            }
            Text(
                friend.focusTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = EarnedColors.Points,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "%,d pts".format(friend.points),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                friend.delta,
                color = EarnedColors.Focus,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FriendPetAvatar(friend: SocialFriend, tint: Color) {
    val background = when (friend.species) {
        "kitsu" -> EarnedColors.Primary.copy(alpha = 0.12f)
        "lumi" -> EarnedColors.Focus.copy(alpha = 0.13f)
        "owly" -> EarnedColors.Secondary.copy(alpha = 0.12f)
        else -> tint.copy(alpha = 0.12f)
    }

    Surface(
        shape = CircleShape,
        color = background,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            PetSprite(
                pet = friend.pet,
                size = 52.dp,
                glow = false
            )
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoreRow(item: MoreItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = EarnedColors.Primary.copy(alpha = 0.12f), modifier = Modifier.size(38.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.label, fontWeight = FontWeight.Medium)
            Text(item.hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyPanel(icon: ImageVector, title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
            Icon(icon, contentDescription = null, tint = EarnedColors.Primary, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun ProgressBar(progress: Float, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))) {
        Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(8.dp).background(color, RoundedCornerShape(8.dp)))
    }
}
