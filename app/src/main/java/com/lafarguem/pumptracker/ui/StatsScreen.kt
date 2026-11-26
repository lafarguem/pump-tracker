package com.lafarguem.pumptracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lafarguem.pumptracker.data.Bar
import com.lafarguem.pumptracker.data.Exercise
import com.lafarguem.pumptracker.data.Stats
import com.lafarguem.pumptracker.data.StatsRange
import java.time.format.DateTimeFormatter
import java.util.Locale

private val bestDayFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    exercise: Exercise,
    range: StatsRange,
    stats: Stats?,
    onExercise: (Exercise) -> Unit,
    onRange: (StatsRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Stats", style = MaterialTheme.typography.headlineMedium)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Exercise.entries.forEachIndexed { i, item ->
                SegmentedButton(
                    selected = item == exercise,
                    onClick = { onExercise(item) },
                    shape = SegmentedButtonDefaults.itemShape(i, Exercise.entries.size),
                    icon = {},
                ) { Text(stringResource(item.label)) }
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            StatsRange.entries.forEachIndexed { i, item ->
                SegmentedButton(
                    selected = item == range,
                    onClick = { onRange(item) },
                    shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size),
                    icon = {},
                ) { Text(item.label) }
            }
        }

        if (stats == null) return@Column

        // Tapping a bar pins its value in the card header; it resets when the selection changes.
        var selected by remember(exercise, range) { mutableStateOf<Int?>(null) }
        val selectedBar = selected?.let(stats.bars::getOrNull)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    selectedBar?.title ?: range.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    (selectedBar?.value ?: stats.periodTotal).toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                BarChart(
                    bars = stats.bars,
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(180.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("Daily average", String.format(Locale.getDefault(), "%.1f", stats.dailyAverage), range.title.lowercase(), Modifier.weight(1f))
            StatTile(
                "Current streak",
                if (stats.currentStreak == 1) "1 day" else "${stats.currentStreak} days",
                "in a row",
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                "Best day",
                stats.bestDay?.total?.toString() ?: "–",
                stats.bestDay?.date?.format(bestDayFormat) ?: "nothing logged yet",
                Modifier.weight(1f),
            )
            StatTile("All time", stats.allTimeTotal.toString(), "total reps", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BarChart(bars: List<Bar>, selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val currentSelected by rememberUpdatedState(selected)
    val max = bars.maxOf { it.value }.coerceAtLeast(1)

    Canvas(
        modifier.pointerInput(bars.size) {
            detectTapGestures { tap ->
                val index = (tap.x / (size.width.toFloat() / bars.size)).toInt().coerceIn(0, bars.lastIndex)
                onSelect(if (index == currentSelected) null else index)
            }
        }
    ) {
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val slot = size.width / bars.size
        val barWidth = (slot * 0.7f).coerceAtMost(36.dp.toPx())
        val radius = CornerRadius((barWidth / 2).coerceAtMost(6.dp.toPx()))

        bars.forEachIndexed { i, bar ->
            val center = slot * i + slot / 2
            val dimmed = selected != null && selected != i
            if (bar.value > 0) {
                val height = (chartHeight * bar.value / max).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = barColor.copy(alpha = if (dimmed) 0.35f else 1f),
                    topLeft = Offset(center - barWidth / 2, chartHeight - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            } else {
                // Rest days keep a faint stub so the time axis stays readable.
                val stub = 2.dp.toPx()
                drawRect(
                    color = emptyColor,
                    topLeft = Offset(center - barWidth / 2, chartHeight - stub),
                    size = Size(barWidth, stub),
                )
            }
            if (bar.label.isNotEmpty()) {
                val text = measurer.measure(bar.label, labelStyle)
                val x = (center - text.size.width / 2f).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))
                drawText(text, topLeft = Offset(x, size.height - text.size.height))
            }
        }
    }
}
