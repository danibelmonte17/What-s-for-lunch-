package com.whatsforlunch.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Sombra ambiental cálida ámbar-carbón para secciones y tarjetas
// (uxdocs/DESIGN.md §Ambient Shadow Character; RF-13 y RF-20 de la fidelidad visual).
fun Modifier.warmShadow(shape: Shape, elevation: Dp = 2.dp): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = WarmShadowAmbient,
    spotColor = WarmShadowSpot,
)
