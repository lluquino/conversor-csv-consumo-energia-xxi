package com.energiaxxi.conversor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.energiaxxi.conversor.model.HourlyRecord

@Composable
fun DataPreview(
    records: List<HourlyRecord>,
    cups: String
) {
    var expanded by remember { mutableStateOf(false) }
    val grouped = records.groupBy { it.date }
    val dates = grouped.keys.sorted()
    val totalDays = dates.size
    val totalRecords = records.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Vista previa",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "$totalDays días · $totalRecords registros · CUPS: $cups",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalRecords > 0) {
                val maxConsumption = records.maxOf { it.consumptionWh } / 1000.0

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Máx: ${"%.3f".format(maxConsumption)} kWh · Media diaria: ${"%.1f".format(totalRecords / totalDays.toDouble())} registros/día",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar detalle" else "Ver detalle")
            }

            AnimatedVisibility(visible = expanded) {
                TablePreview(records = records)
            }
        }
    }
}

@Composable
private fun TablePreview(records: List<HourlyRecord>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        TableHeader()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(records) { index, record ->
                TableRow(index = index + 1, record = record)
                if (index < records.size - 1) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text("#", modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Fecha", modifier = Modifier.width(90.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        Text("Hora", modifier = Modifier.width(40.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
        Text("Consumo (kWh)", modifier = Modifier.width(100.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End)
    }
}

@Composable
private fun TableRow(index: Int, record: HourlyRecord) {
    val consumptionKWh = record.consumptionWh / 1000.0
    val fechaDDMM = formatDateShort(record.date)
    val hora = extractHourDisplay(record.hourSlot)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text("$index", modifier = Modifier.width(28.dp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(fechaDDMM, modifier = Modifier.width(90.dp), fontSize = 11.sp)
        Text(hora, modifier = Modifier.width(40.dp), fontSize = 11.sp, textAlign = TextAlign.Center)
        Text(
            text = "%.3f".format(consumptionKWh),
            modifier = Modifier.width(100.dp),
            fontSize = 11.sp,
            textAlign = TextAlign.End
        )
    }
}

private fun formatDateShort(ymd: String): String {
    val parts = ymd.split("-")
    if (parts.size == 3) return "${parts[2]}/${parts[1]}"
    return ymd
}

private fun extractHourDisplay(hourSlot: String): String {
    val startHour = hourSlot.substringBefore("-").substringBefore(":").trim()
    return (startHour.toInt() + 1).toString()
}
