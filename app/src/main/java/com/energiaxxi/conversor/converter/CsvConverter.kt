package com.energiaxxi.conversor.converter

import com.energiaxxi.conversor.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object CsvConverter {

    fun parseAndValidate(content: String): Pair<ConsumptionData?, List<ValidationError>> {
        val lines = content.lines().map { it.trimEnd() }
        if (lines.isEmpty()) return null to listOf(ValidationError.InvalidFormat("Fichero vacío"))

        val errors = mutableListOf<ValidationError>()

        val cups = extractMetaField(lines, "CUPS:")
            ?: return null to listOf(ValidationError.InvalidFormat("No se encontró el CUPS"))
        val fechaInicio = extractMetaField(lines, "Fecha inicio")
            ?: return null to listOf(ValidationError.InvalidFormat("No se encontró la fecha de inicio"))
        val fechaFin = extractMetaField(lines, "Fecha fin")
            ?: return null to listOf(ValidationError.InvalidFormat("No se encontró la fecha de fin"))

        val dataStartIndex = lines.indexOfFirst { it.contains("Consumo (Wh)") }
        if (dataStartIndex == -1) return null to listOf(ValidationError.NoDataFound)

        val rawRecords = mutableListOf<HourlyRecord>()
        for (i in (dataStartIndex + 1) until lines.size) {
            val lineNum = i + 1
            val line = lines[i].trim()
            if (line.isBlank()) continue
            if (line.startsWith("Total", ignoreCase = true)) continue

            val parts = line.split(" , ")
            if (parts.size < 3) continue

            val date = parts[0].trim()
            val hourSlot = parts[1].trim()
            val consumptionStr = parts[2].trim().replace(",", ".")

            val consumptionWh = consumptionStr.toDoubleOrNull()
            if (consumptionWh == null) {
                errors.add(ValidationError.InvalidFormat(
                    "Consumo inválido en línea $lineNum: «$consumptionStr»"
                ))
                continue
            }

            rawRecords.add(HourlyRecord(date, hourSlot, consumptionWh))
        }

        if (rawRecords.isEmpty()) {
            errors.add(ValidationError.NoDataFound)
            return ConsumptionData(cups, fechaInicio, fechaFin, emptyList()) to errors
        }

        val grouped = rawRecords.groupBy { it.date }
        val dates = grouped.keys.sortedBy { parseDateYMD(it) }

        for ((date, dayRecords) in grouped) {
            val hours = dayRecords.map { extractHourNumber(it.hourSlot) }

            val dups = hours.groupBy { it }.filter { it.value.size > 1 }
            for ((hour, _) in dups) {
                errors.add(ValidationError.DuplicateHour(date, hour))
            }

            val hourSet = hours.toSet()
            for (h in 1..24) {
                if (h !in hourSet) {
                    errors.add(ValidationError.MissingHour(date, h))
                }
            }
        }

        for (i in 1 until dates.size) {
            val prev = parseDateYMD(dates[i - 1])
            val curr = parseDateYMD(dates[i])
            val diff = ChronoUnit.DAYS.between(prev, curr)
            if (diff > 1L) {
                for (d in 1L until diff) {
                    val missing = prev.plusDays(d)
                    errors.add(ValidationError.MissingDay(missing.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                }
            }
        }

        val consumptionData = ConsumptionData(cups, fechaInicio, fechaFin, rawRecords)
        return consumptionData to errors
    }

    fun findCleanSuffix(records: List<HourlyRecord>): Pair<String?, List<HourlyRecord>> {
        val grouped = records.groupBy { it.date }
        val dates = grouped.keys.sortedBy { parseDateYMD(it) }
        if (dates.isEmpty()) return null to emptyList()

        var suffixStart: String? = null

        for (i in dates.indices.reversed()) {
            val date = dates[i]
            val dayRecords = grouped[date]!!
            val hours = dayRecords.map { extractHourNumber(it.hourSlot) }.toSet()
            val hasAllHours = (1..24).all { it in hours }

            if (!hasAllHours) break

            if (i < dates.size - 1) {
                val curr = parseDateYMD(date)
                val next = parseDateYMD(dates[i + 1])
                if (ChronoUnit.DAYS.between(curr, next) != 1L) break
            }

            suffixStart = date
        }

        if (suffixStart == null) return null to emptyList()

        val cleanRecords = records.filter { it.date >= suffixStart }
        return suffixStart to cleanRecords
    }

    fun convertToOutput(data: ConsumptionData, metodoObtencion: String): String {
        val sb = StringBuilder()
        sb.appendLine("CUPS;Fecha;Hora;Consumo_kWh;Metodo_obtencion")

        for (record in data.records) {
            val fechaDDMMYYYY = convertDateYMDtoDMY(record.date)
            val horaNum = extractHourNumber(record.hourSlot)
            val consumoKWh = record.consumptionWh / 1000.0
            sb.appendLine("${data.cups};$fechaDDMMYYYY;$horaNum;$consumoKWh;$metodoObtencion")
        }

        return sb.toString()
    }

    fun generateFileName(data: ConsumptionData): String {
        val fechaInicio = data.fechaInicio.trim()
        val fechaFin = data.fechaFin.trim()
        val inParts = fechaInicio.split("/")
        val finParts = fechaFin.split("/")
        val start = if (inParts.size == 3) "${inParts[2]}_${inParts[1]}_${inParts[0]}" else "unknown"
        val end = if (finParts.size == 3) "${finParts[2]}_${finParts[1]}_${finParts[0]}" else "unknown"
        return "consumption_${start}__${end}.csv"
    }

    private fun extractMetaField(lines: List<String>, prefix: String): String? {
        val line = lines.firstOrNull { it.trimStart().startsWith(prefix) } ?: return null
        val idx = line.lastIndexOf(", ")
        return if (idx >= 0) line.substring(idx + 2).trim() else line.substringAfter(":").trim()
    }

    private fun parseDateYMD(dateStr: String): LocalDate {
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun convertDateYMDtoDMY(dateStr: String): String {
        val date = parseDateYMD(dateStr)
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun extractHourNumber(hourSlot: String): Int {
        val startHour = hourSlot.substringBefore("-").substringBefore(":").trim()
        return startHour.toInt() + 1
    }
}
