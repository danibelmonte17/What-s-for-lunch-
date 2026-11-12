package com.whatsforlunch.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Icono de sartén compartido (uxdocs/DESIGN.md): lo usa el badge de momento
// «Almuerzo» en Mi recetario y la CTA «Empezar Modo Cocina» del Detalle de Receta.
val SkilletIcon: ImageVector = ImageVector.Builder(
    name = "Skillet",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3.75f, 7.5f)
        lineTo(16.25f, 7.5f)
        curveTo(17.49f, 7.5f, 18.5f, 8.51f, 18.5f, 9.75f)
        lineTo(18.5f, 11.5f)
        curveTo(18.5f, 15.09f, 15.59f, 18f, 12f, 18f)
        lineTo(8f, 18f)
        curveTo(4.41f, 18f, 1.5f, 15.09f, 1.5f, 11.5f)
        lineTo(1.5f, 9.75f)
        curveTo(1.5f, 8.51f, 2.51f, 7.5f, 3.75f, 7.5f)
        close()
        moveTo(18.5f, 8.5f)
        lineTo(20.5f, 8.5f)
        lineTo(23f, 11f)
        lineTo(21.5f, 12.5f)
        lineTo(19.5f, 10.5f)
        lineTo(18.5f, 10.5f)
        close()
    }
}.build()
