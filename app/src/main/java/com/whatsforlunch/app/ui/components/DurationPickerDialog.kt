package com.whatsforlunch.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.whatsforlunch.app.R

// Identificadores estables del diálogo del reloj de duración (E2E).
object DurationPickerTestTags {
    const val DIALOG = "duration_picker_dialog"
    const val CONFIRM = "duration_picker_confirm"
    const val CANCEL = "duration_picker_cancel"
}

/**
 * Reloj de duración HH:MM de Material 3 en modo entrada (D1/D7): horas:minutos por
 * teclado en formato 24 h (00:00-23:59). Compartido por el Tiempo de preparación del
 * formulario de Receta y el temporizador de cada Paso; ambos traducen los valores a
 * minutos al confirmar. 00:00 = 0 min en preparación (D7); el temporizador de un
 * Paso lo trata como «Sin temporizador» (D10).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPickerDialog(
    title: String,
    initialMinutes: Int,
    onConfirm: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = (initialMinutes / 60).coerceIn(0, 23),
        initialMinute = (initialMinutes % 60).coerceIn(0, 59),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Modo entrada (teclado horas:minutos), fijo: el usuario teclea la
                // duración en lugar de girar el dial.
                TimeInput(state = state)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.hour * 60 + state.minute) },
                modifier = Modifier.testTag(DurationPickerTestTags.CONFIRM),
            ) {
                Text(text = stringResource(R.string.time_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(DurationPickerTestTags.CANCEL),
            ) {
                Text(text = stringResource(R.string.time_picker_cancel))
            }
        },
        modifier = Modifier.testTag(DurationPickerTestTags.DIALOG),
    )
}

// Duración en minutos como HH:MM (D7, 24 h): 65 -> "01:05".
fun formatDurationMinutes(minutes: Int): String {
    val clamped = minutes.coerceIn(0, 23 * 60 + 59)
    return "%02d:%02d".format(clamped / 60, clamped % 60)
}

// Estilo compartido del texto de los controles de reloj del formulario.
@Composable
fun durationPickerTextStyle() = MaterialTheme.typography.titleMedium
