package com.energiaxxi.conversor.model

data class ConsumptionData(
    val cups: String,
    val fechaInicio: String,
    val fechaFin: String,
    val records: List<HourlyRecord>
)

data class HourlyRecord(
    val date: String,
    val hourSlot: String,
    val consumptionWh: Double
)

enum class MetodoObtencion(val code: String) {
    R("R"),
    E("E")
}
