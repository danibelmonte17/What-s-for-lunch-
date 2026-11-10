package com.whatsforlunch.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

// Campos del sistema visual compartidos (RF-13): 0.75rem con relleno en
// surface-container-low y anillo de foco primary. Viven en theme/ para que los
// campos del formulario de Receta y el desplegable de unidades
// (ui/components/UnitDropdown.kt) compartan la misma estética.
val FieldShape: Shape = RoundedCornerShape(WarmRadius.md)

// Campos rellenos en surface-container-low con anillo de foco primary (RF-10, RF-12).
@Composable
fun warmFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerLow,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)

// Caja de texto del Paso con fondo blanco (redisenar-formulario-de-receta): el texto
// del Paso se escribe sobre blanco, conservando FieldShape y el anillo de foco.
@Composable
fun whiteFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    errorContainerColor = Color.White,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    cursorColor = MaterialTheme.colorScheme.primary,
)
