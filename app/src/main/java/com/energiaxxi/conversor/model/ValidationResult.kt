package com.energiaxxi.conversor.model

sealed class ValidationError {
    data class MissingDay(val date: String) : ValidationError()
    data class MissingHour(val date: String, val hour: Int) : ValidationError()
    data class DuplicateHour(val date: String, val hour: Int) : ValidationError()
    data object NoDataFound : ValidationError()
    data class InvalidFormat(val detail: String) : ValidationError()
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val cleanSuffixStart: String?,
    val cleanRecords: List<HourlyRecord>
)
