package com.lafarguem.pumptracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lafarguem.pumptracker.R
import com.lafarguem.pumptracker.data.Entry
import com.lafarguem.pumptracker.data.Exercise
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val headerFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

@Composable
fun TodayScreen(
    date: LocalDate,
    entries: List<Entry>,
    onAdd: (Exercise, Int) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = remember(entries) {
        entries.groupingBy { it.exercise }.fold(0) { sum, entry -> sum + entry.amount }
    }
    var customFor by rememberSaveable { mutableStateOf<Exercise?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Today", style = MaterialTheme.typography.headlineMedium)
                Text(
                    date.format(headerFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(Exercise.entries, key = { it.name }) { exercise ->
            ExerciseCard(
                exercise = exercise,
                total = totals[exercise] ?: 0,
                onAdd = { onAdd(exercise, it) },
                onCustom = { customFor = exercise },
            )
        }
        if (entries.isNotEmpty()) {
            item {
                Text(
                    "Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(entries, key = { it.id }) { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        Instant.ofEpochMilli(entry.timestamp).atZone(ZoneId.systemDefault()).format(timeFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Text(
                        "+${entry.amount} ${stringResource(entry.exercise.label).lowercase()}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDelete(entry.id) }) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    customFor?.let { exercise ->
        CustomAmountDialog(
            exercise = exercise,
            onConfirm = {
                onAdd(exercise, it)
                customFor = null
            },
            onDismiss = { customFor = null },
        )
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, total: Int, onAdd: (Int) -> Unit, onCustom: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(exercise.label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    total.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tight = PaddingValues(horizontal = 4.dp)
                exercise.quickSteps.forEach { step ->
                    FilledTonalButton(
                        onClick = { onAdd(step) },
                        modifier = Modifier.weight(1f),
                        contentPadding = tight,
                    ) { Text("+$step") }
                }
                OutlinedButton(
                    onClick = onCustom,
                    modifier = Modifier.weight(1f),
                    contentPadding = tight,
                ) { Text("Other") }
            }
        }
    }
}

@Composable
private fun CustomAmountDialog(exercise: Exercise, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val amount = text.toIntOrNull()?.takeIf { it in 1..9999 }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${stringResource(exercise.label).lowercase()}") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(4) },
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { amount?.let(onConfirm) }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = { amount?.let(onConfirm) }, enabled = amount != null) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
