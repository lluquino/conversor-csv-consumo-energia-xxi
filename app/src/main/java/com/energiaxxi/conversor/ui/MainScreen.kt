package com.energiaxxi.conversor.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.energiaxxi.conversor.converter.CsvConverter
import com.energiaxxi.conversor.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var metodoObtencion by remember { mutableStateOf("R") }
    var parsedData by remember { mutableStateOf<ConsumptionData?>(null) }
    var validationErrors by remember { mutableStateOf<List<ValidationError>?>(null) }
    var cleanSuffixRecords by remember { mutableStateOf<List<HourlyRecord>?>(null) }
    var showValidationDialog by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }

    LaunchedEffect(Unit) {
        metodoObtencion = prefs.getString("metodo_obtencion", "R") ?: "R"
    }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            selectedFileName = getFileName(context, it)
            isProcessing = true
            errorMessage = null
            validationErrors = null
            cleanSuffixRecords = null
            parsedData = null

            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val content = readTextFromUri(context, it)
                        val (data, errors) = CsvConverter.parseAndValidate(content)
                        if (data != null) {
                            val (_, cleanRecords) = CsvConverter.findCleanSuffix(data.records)
                            Triple(data, errors, cleanRecords)
                        } else {
                            Triple(null, errors, emptyList())
                        }
                    } catch (e: Exception) {
                        Triple(null, listOf(ValidationError.InvalidFormat(e.message ?: "Error desconocido")), emptyList())
                    }
                }

                val (data, errors, cleanRecords) = result
                parsedData = data
                validationErrors = errors
                cleanSuffixRecords = cleanRecords

                if (data != null && errors.isNotEmpty()) {
                    showValidationDialog = true
                } else if (data == null && errors.isNotEmpty()) {
                    errorMessage = errors.firstOrNull()?.let {
                        formatError(context, it)
                    } ?: "Error desconocido"
                }

                isProcessing = false
            }
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val data = parsedData ?: return@launch
                    val recordsToUse = if (validationErrors.isNullOrEmpty()) {
                        data.records
                    } else {
                        cleanSuffixRecords ?: data.records
                    }
                    val cleanData = data.copy(records = recordsToUse)
                    val csvContent = CsvConverter.convertToOutput(cleanData, metodoObtencion)
                    withContext(Dispatchers.IO) {
                        writeTextToUri(context, uri, csvContent)
                    }
                    showSuccess = true
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Error al guardar"
                }
                isProcessing = false
            }
        }
    }

    fun changeLanguage(langCode: String) {
        showLanguageMenu = false
        LanguageManager.saveLanguage(context, langCode)
        (context as? Activity)?.recreate()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(com.energiaxxi.conversor.R.string.app_name)) },
                actions = {
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = context.getString(com.energiaxxi.conversor.R.string.lbl_language))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Español") },
                                onClick = { changeLanguage("es") }
                            )
                            DropdownMenuItem(
                                text = { Text("Català") },
                                onClick = { changeLanguage("ca") }
                            )
                            DropdownMenuItem(
                                text = { Text("Galego") },
                                onClick = { changeLanguage("gl") }
                            )
                            DropdownMenuItem(
                                text = { Text("Euskera") },
                                onClick = { changeLanguage("eu") }
                            )
                            DropdownMenuItem(
                                text = { Text("Leonés") },
                                onClick = { changeLanguage("ast") }
                            )
                            DropdownMenuItem(
                                text = { Text("Aranés") },
                                onClick = { changeLanguage("oc") }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedButton(
                onClick = { pickFileLauncher.launch(arrayOf("text/csv", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(context.getString(com.energiaxxi.conversor.R.string.btn_select_csv))
            }

            if (selectedFileName != null) {
                Text(
                    text = selectedFileName!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (parsedData != null && validationErrors.isNullOrEmpty()) {
                Text(
                    text = "✓ ${parsedData!!.records.size} registros, ${parsedData!!.records.groupBy { it.date }.size} días",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = context.getString(com.energiaxxi.conversor.R.string.lbl_method),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.selectableGroup()) {
                MetodoOption(
                    code = "R",
                    title = context.getString(com.energiaxxi.conversor.R.string.option_real),
                    description = context.getString(com.energiaxxi.conversor.R.string.desc_real),
                    selected = metodoObtencion == "R",
                    onClick = {
                        metodoObtencion = "R"
                        prefs.edit().putString("metodo_obtencion", "R").apply()
                    }
                )
                Spacer(Modifier.height(8.dp))
                MetodoOption(
                    code = "E",
                    title = context.getString(com.energiaxxi.conversor.R.string.option_estimated),
                    description = context.getString(com.energiaxxi.conversor.R.string.desc_estimated),
                    selected = metodoObtencion == "E",
                    onClick = {
                        metodoObtencion = "E"
                        prefs.edit().putString("metodo_obtencion", "E").apply()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text(context.getString(com.energiaxxi.conversor.R.string.msg_processing))
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (showSuccess) {
                Text(
                    text = context.getString(com.energiaxxi.conversor.R.string.msg_success),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val uri = selectedFileUri ?: run {
                        errorMessage = context.getString(com.energiaxxi.conversor.R.string.msg_select_file_first)
                        return@Button
                    }
                    errorMessage = null
                    showSuccess = false
                    val data = parsedData ?: return@Button
                    val recordsToUse = if (validationErrors.isNullOrEmpty()) {
                        data.records
                    } else {
                        val clean = cleanSuffixRecords
                        if (clean == null || clean.isEmpty()) {
                            errorMessage = context.getString(com.energiaxxi.conversor.R.string.error_no_data)
                            return@Button
                        }
                        clean
                    }
                    val cleanData = data.copy(records = recordsToUse)
                    val fileName = CsvConverter.generateFileName(cleanData)
                    saveFileLauncher.launch(fileName)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedFileUri != null && !isProcessing
            ) {
                Text(context.getString(com.energiaxxi.conversor.R.string.btn_convert))
            }
        }
    }

    if (showValidationDialog && parsedData != null) {
        val errors = validationErrors ?: emptyList()
        val cleanRecords = cleanSuffixRecords ?: emptyList()

        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = { Text(context.getString(com.energiaxxi.conversor.R.string.validation_title)) },
            text = {
                Column {
                    errors.take(20).forEach { err ->
                        Text(
                            text = formatError(context, err),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    if (errors.size > 20) {
                        Text(
                            text = "... y ${errors.size - 20} errores más",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (cleanRecords.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        val cleanDates = cleanRecords.groupBy { it.date }.keys.sorted()
                        val startDate = cleanDates.firstOrNull()?.let { formatDateDisplay(it) } ?: ""
                        val endDate = cleanDates.lastOrNull()?.let { formatDateDisplay(it) } ?: ""
                        Text(
                            text = context.getString(
                                com.energiaxxi.conversor.R.string.validation_clean_days,
                                startDate, endDate, cleanDates.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))
                        val allDates = parsedData!!.records.groupBy { it.date }.keys.sorted()
                        val removedDates = allDates.filter { it !in cleanDates }
                        if (removedDates.isNotEmpty()) {
                            Text(
                                text = "${context.getString(com.energiaxxi.conversor.R.string.validation_days_to_remove)} ${removedDates.joinToString(", ") { formatDateDisplay(it) }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${context.getString(com.energiaxxi.conversor.R.string.validation_days_to_keep)} ${cleanDates.joinToString(", ") { formatDateDisplay(it) }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (cleanRecords.isNotEmpty()) {
                    TextButton(onClick = {
                        showValidationDialog = false
                    }) {
                        Text(context.getString(com.energiaxxi.conversor.R.string.validation_continue))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showValidationDialog = false
                    selectedFileUri = null
                    selectedFileName = null
                    parsedData = null
                    validationErrors = null
                    cleanSuffixRecords = null
                }) {
                    Text(context.getString(com.energiaxxi.conversor.R.string.validation_cancel))
                }
            }
        )
    }
}

@Composable
private fun MetodoOption(
    code: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (selected) 2.dp else 0.dp,
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatError(context: Context, error: ValidationError): String {
    return when (error) {
        is ValidationError.MissingDay -> {
            context.getString(com.energiaxxi.conversor.R.string.validation_error_missing_day,
                formatDateDisplay(error.date))
        }
        is ValidationError.MissingHour -> {
            context.getString(com.energiaxxi.conversor.R.string.validation_error_missing_hour,
                formatDateDisplay(error.date), error.hour)
        }
        is ValidationError.DuplicateHour -> {
            context.getString(com.energiaxxi.conversor.R.string.validation_error_duplicate_hour,
                formatDateDisplay(error.date), error.hour)
        }
        is ValidationError.NoDataFound -> {
            context.getString(com.energiaxxi.conversor.R.string.error_no_data)
        }
        is ValidationError.InvalidFormat -> {
            context.getString(com.energiaxxi.conversor.R.string.error_parse) + ": ${error.detail}"
        }
    }
}

private fun formatDateDisplay(ymd: String): String {
    val parts = ymd.split("-")
    if (parts.size == 3) return "${parts[2]}/${parts[1]}/${parts[0]}"
    return ymd
}

private fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) it.getString(idx) else null
    }
}

private fun readTextFromUri(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
        ?: throw Exception("No se pudo abrir el fichero")
}

private fun writeTextToUri(context: Context, uri: Uri, content: String) {
    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
        ?: throw Exception("No se pudo guardar el fichero")
}
