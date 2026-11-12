package com.whatsforlunch.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import com.whatsforlunch.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pulsación larga para elegir una foto de la galería (Flujo B).
 *
 * Los botones de Material3 (`Button`, `FloatingActionButton`…) ya traen su propio
 * `clickable` interno, que escucha en el pass `Main` y consume los eventos antes de
 * que cualquier modificador externo pueda verlos: por eso `combinedClickable` no
 * funciona sobre ellos. Este detector observa en el pass `Initial`, que recorre el
 * árbol de arriba hacia abajo y SIEMPRE se ejecuta antes que cualquier handler de
 * `Main` (el del propio botón), sin consumir nada mientras la pulsación sea corta.
 *
 * Cuando la pulsación supera el umbral largo se lanza [onGalleryRequested] y, a
 * partir de ahí, se consume cada evento para que el `onClick` interno del botón se
 * cancele en lugar de dispararse: pulsar normal sigue abriendo la cámara y
 * mantener pulsado abre la galería. Si `enabled` es falso no se registra ningún
 * detector y el botón conserva su comportamiento estándar.
 */
@Composable
fun Modifier.longPressToGallery(
    enabled: Boolean = true,
    onGalleryRequested: () -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val label = stringResource(R.string.action_pick_from_gallery)
    val currentOnGalleryRequested by rememberUpdatedState(onGalleryRequested)
    return this
        .then(
            if (enabled) {
                Modifier.longPressDetector {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnGalleryRequested()
                }
            } else {
                Modifier
            },
        )
        .then(
            if (enabled) {
                Modifier.semantics {
                    onLongClick(label = label) {
                        currentOnGalleryRequested()
                        true
                    }
                }
            } else {
                Modifier
            },
        )
}

private fun Modifier.longPressDetector(onLongPress: () -> Unit): Modifier =
    pointerInput(onLongPress) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    // Initial llega antes que Main y solo exige un down sin consumir:
                    // así el clickable interno del botón empieza su gesto con normalidad.
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val down = event.changes.firstOrNull { it.changedToDown() } ?: continue
                    var longPressTriggered = false
                    val timer = launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        longPressTriggered = true
                        onLongPress()
                    }
                    // Seguimos la pulsación en Initial; si el umbral largo se cumple,
                    // consumimos el resto para que el gesto del botón se cancele.
                    while (true) {
                        val current = awaitPointerEvent(PointerEventPass.Initial)
                        if (longPressTriggered) current.changes.forEach { it.consume() }
                        if (current.changes.none { it.pressed }) break
                        if (!longPressTriggered && current.changes.any { it.isConsumed }) break
                    }
                    timer.cancel()
                }
            }
        }
    }
