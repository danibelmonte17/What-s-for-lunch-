package com.whatsforlunch.app.data.scanner

import com.whatsforlunch.app.domain.model.ScanDraftItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Contenedor en memoria de los chips del Escaneo todavía sin guardar (D3). Permite
// que la pantalla de Sugerencias los lea sin persistirlos y sin duplicar estado; se
// vacía al guardar o al salir del Escáner. No toca Room (RD06 se mantiene).
class ScanDraftHolder {
    private val _items = MutableStateFlow<List<ScanDraftItem>>(emptyList())
    val items: StateFlow<List<ScanDraftItem>> = _items.asStateFlow()

    fun update(items: List<ScanDraftItem>) {
        _items.value = items
    }

    fun clear() {
        _items.value = emptyList()
    }
}
