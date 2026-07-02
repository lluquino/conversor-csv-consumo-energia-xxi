package com.energiaxxi.conversor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.energiaxxi.conversor.model.HourlyRecord

private val chartColors = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800),
    Color(0xFF9C27B0), Color(0xFFF44336), Color(0xFF00BCD4),
    Color(0xFFCDDC39), Color(0xFFFF5722), Color(0xFF607D8B),
    Color(0xFF795548), Color(0xFFE91E63), Color(0xFF3F51B5)
)

@Composable
fun ConsumptionChart(records: List<HourlyRecord>) {
    var selectedMode by remember { mutableStateOf("daily") }
    val grouped = records.groupBy { it.date }
    val dates = grouped.keys.sorted()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Gráfico de consumo",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Row {
                FilterChip(
                    selected = selectedMode == "daily",
                    onClick = { selectedMode = "daily" },
                    label = { Text("Diario", fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 4.dp)
                )
                FilterChip(
                    selected = selectedMode == "hourly",
                    onClick = { selectedMode = "hourly" },
                    label = { Text("Horario (promedio)", fontSize = 11.sp) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            if (selectedMode == "daily") {
                DailyChart(dates = dates, grouped = grouped)
            } else {
                HourlyChart(records = records)
            }
        }
    }
}

@Composable
private fun DailyChart(dates: List<String>, grouped: Map<String, List<HourlyRecord>>) {
    val dailyTotals = dates.map { date ->
        grouped[date]?.sumOf { it.consumptionWh } ?: 0.0
    }
    val maxKWh = (dailyTotals.maxOrNull() ?: 1.0) / 1000.0
    val barCount = dates.size
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        val barWidth = 60.dp
        val totalWidth = (barWidth * barCount.coerceAtLeast(1)) + 20.dp
        val chartHeight = 180.dp

        Box(modifier = Modifier.width(totalWidth).height(chartHeight + 40.dp)) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(chartHeight)
            ) {
                val w = size.width / barCount.coerceAtLeast(1)
                val h = size.height
                val baseline = h - 4.dp.toPx()
                val maxVal = maxKWh.coerceAtLeast(0.001)

                drawYAxis(h, maxVal)

                dates.forEachIndexed { idx, date ->
                    val value = dailyTotals[idx] / 1000.0
                    val barH = (value / maxVal) * (baseline - 10.dp.toPx())
                    val x = idx * w + w * 0.15f
                    val barW = w * 0.7f
                    val color = chartColors[idx % chartColors.size]

                    drawRect(
                        color = color,
                        topLeft = Offset(x, baseline - barH.toFloat()),
                        size = Size(barW.toFloat(), barH.toFloat())
                    )
                }
            }

            val textSize = 10.sp
            Row(
                modifier = Modifier
                    .offset(y = chartHeight)
                    .width(totalWidth)
                    .padding(top = 4.dp)
            ) {
                dates.forEachIndexed { idx, date ->
                    val w = size.width / barCount.coerceAtLeast(1)
                    val day = formatDateShort(date)
                    Text(
                        text = day,
                        fontSize = textSize,
                        modifier = Modifier.width(w),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Total: ${"%.1f".format(dailyTotals.sum() / 1000.0)} kWh · Máx: ${"%.1f".format(maxKWh)} kWh",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HourlyChart(records: List<HourlyRecord>) {
    val hourlyAvgs = (1..24).map { hour ->
        val matching = records.filter { extractHourNum(it.hourSlot) == hour }
        if (matching.isNotEmpty()) matching.sumOf { it.consumptionWh } / matching.size else 0.0
    }
    val maxKWh = (hourlyAvgs.maxOrNull() ?: 1.0) / 1000.0

    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        val totalWidth = (25 * 32).dp
        val chartHeight = 180.dp

        Box(modifier = Modifier.width(totalWidth).height(chartHeight + 40.dp)) {
            Canvas(
                modifier = Modifier
                    .width(totalWidth)
                    .height(chartHeight)
            ) {
                val h = size.height
                val baseline = h - 4.dp.toPx()
                val maxVal = maxKWh.coerceAtLeast(0.001)
                val barW = 20.dp.toPx()
                val gap = 8.dp.toPx()

                drawYAxis(h, maxVal)

                hourlyAvgs.forEachIndexed { idx, valueWh ->
                    val value = valueWh / 1000.0
                    val barH = (value / maxVal) * (baseline - 10.dp.toPx())
                    val x = idx * (barW + gap) + 4.dp.toPx()
                    val color = chartColors[idx % chartColors.size]

                    drawRect(
                        color = color,
                        topLeft = Offset(x.toFloat(), baseline - barH.toFloat()),
                        size = Size(barW.toFloat(), barH.toFloat())
                    )
                }
            }

            Row(
                modifier = Modifier
                    .offset(y = chartHeight)
                    .width(totalWidth)
                    .padding(top = 4.dp)
            ) {
                (1..24).forEach { h ->
                    Text(
                        text = h.toString(),
                        fontSize = 8.sp,
                        modifier = Modifier.width(28.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawYAxis(chartHeight: Float, maxVal: Double) {
    val baseline = chartHeight - 4.dp.toPx()
    val ySteps = 4
    for (i in 0..ySteps) {
        val y = baseline - (i.toFloat() / ySteps) * (baseline - 10.dp.toPx())
        val label = "%.1f".format(maxVal * i / ySteps)
        drawLine(
            color = Color.Gray.copy(alpha = 0.3f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5.dp.toPx()
        )
        drawContext.canvas.nativeCanvas.drawText(
            label,
            2.dp.toPx(),
            y - 2.dp.toPx(),
            android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 22f
            }
        )
    }
}

private fun extractHourNum(hourSlot: String): Int {
    val startHour = hourSlot.substringBefore("-").substringBefore(":").trim()
    return startHour.toInt() + 1
}

private fun formatDateShort(ymd: String): String {
    val parts = ymd.split("-")
    if (parts.size == 3) return "${parts[2]}/${parts[1]}"
    return ymd
}
