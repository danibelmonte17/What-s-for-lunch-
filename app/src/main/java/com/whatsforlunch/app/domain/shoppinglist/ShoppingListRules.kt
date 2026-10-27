package com.whatsforlunch.app.domain.shoppinglist

import com.whatsforlunch.app.domain.model.Aisle
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.RecipeIngredient
import com.whatsforlunch.app.domain.model.ShoppingItemSource
import com.whatsforlunch.app.domain.model.ShoppingListItem
import com.whatsforlunch.app.domain.text.normalizeName
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Reglas puras de la Lista de la Compra (RD18/RD19/RD20): Faltantes por nombre
 * normalizado, fusión idempotente de aportaciones por fuente, alta manual de
 * ÍtemLista (upsert por nombre normalizado) y agrupación/progreso de la Lista.
 *
 * Cada fuente (una Receta o una Propuesta del Pinche) suma su cantidad una sola vez;
 * repetir la acción no duplica filas ni dobla la suma. Sin dependencias de Android
 * ni de la UI.
 */
object ShoppingListRules {

    /**
     * RD15: ingredientes obligatorios ausentes de la Despensa, sin duplicados
     * (primera aparición) y en el orden de la receta. La comparación usa
     * `normalizeName`.
     */
    fun missingRequiredIngredients(
        ingredients: List<RecipeIngredient>,
        pantryItems: List<PantryItem>,
    ): List<RecipeIngredient> {
        val available = pantryItems.mapTo(mutableSetOf()) { normalizeName(it.name) }
        val seen = mutableSetOf<String>()
        return ingredients
            .filter { !it.optional }
            .filter { ingredient ->
                val normalized = normalizeName(ingredient.name)
                normalized.isNotBlank() && normalized !in available && seen.add(normalized)
            }
    }

    /**
     * Id estable de un ÍtemLista a partir del nombre normalizado: borrar y volver a
     * añadir el mismo ingrediente recrea la misma fila lógica (RD18).
     */
    fun itemIdFor(name: String): String = UUID
        .nameUUIDFromBytes("shopping:${normalizeName(name)}".toByteArray(Charsets.UTF_8))
        .toString()

    /**
     * RD18: fusiona una aportación completa de una fuente sobre los ítems actuales.
     * - Un ítem por nombre normalizado (primera aparición de la aportación).
     * - Si la fuente ya aportó a ese ítem, no cambia nada.
     * - Las cantidades se suman solo cuando la unidad normalizada coincide.
     * - El Origen conserva todas las fuentes que aportaron.
     */
    fun addContributions(
        existing: List<ShoppingListItem>,
        sourceKey: String,
        sourceTitle: String,
        ingredients: List<RecipeIngredient>,
    ): List<ShoppingListItem> {
        val result = existing.toMutableList()
        val seenNames = mutableSetOf<String>()
        ingredients.forEach { ingredient ->
            val normalized = normalizeName(ingredient.name)
            if (normalized.isBlank() || !seenNames.add(normalized)) return@forEach

            val index = result.indexOfFirst { normalizeName(it.name) == normalized }
            if (index < 0) {
                result += ShoppingListItem(
                    id = itemIdFor(ingredient.name),
                    name = ingredient.name.trim(),
                    quantity = ingredient.quantity.takeIf { it > 0.0 },
                    unit = ingredient.unit.trim(),
                    aisle = null,
                    completed = false,
                    sources = listOf(ShoppingItemSource(sourceKey, sourceTitle)),
                )
            } else {
                val item = result[index]
                if (item.sources.any { it.sourceKey == sourceKey }) return@forEach
                result[index] = item.mergedWith(
                    ingredient = ingredient,
                    source = ShoppingItemSource(sourceKey, sourceTitle),
                )
            }
        }
        return result
    }

    private fun ShoppingListItem.mergedWith(
        ingredient: RecipeIngredient,
        source: ShoppingItemSource,
    ): ShoppingListItem {
        val incomingQuantity = ingredient.quantity.takeIf { it > 0.0 }
        val sameUnit = normalizeName(unit) == normalizeName(ingredient.unit)
        val merged = when {
            // Un ítem sin cantidad adopta la primera cantidad entrante si la unidad
            // no contradice la que ya tuviera (D10: sin conversiones).
            quantity == null && incomingQuantity != null && (unit.isBlank() || sameUnit) ->
                copy(quantity = incomingQuantity, unit = ingredient.unit.trim())
            // Misma unidad: las fuentes distintas acumulan (200 g + 500 g = 700 g).
            quantity != null && incomingQuantity != null && sameUnit ->
                copy(quantity = quantity + incomingQuantity)
            // Unidades distintas o cantidad 0: se conserva la cantidad/unidad actual.
            else -> this
        }
        return merged.copy(sources = sources + source)
    }

    // --- Alta manual (RD19) ---------------------------------------------------

    /**
     * RD19: el nombre del alta manual no puede quedar vacío tras normalizar.
     */
    fun isManualNameValid(name: String): Boolean = normalizeName(name).isNotBlank()

    /**
     * RD19: la cantidad manual es opcional (null) y, si se escribe, debe ser un
     * decimal finito mayor que 0, sin tope 99 (RD18).
     */
    fun isManualQuantityValid(quantity: Double?): Boolean =
        quantity == null || (quantity.isFinite() && quantity > 0.0)

    /**
     * RD19: cantidad escrita por el usuario, aceptando coma o punto decimal
     * (D6). Devuelve null para el texto vacío (sin cantidad) y para lo que no
     * sea un número.
     */
    fun parseManualQuantity(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.replace(',', '.').toDoubleOrNull()
    }

    /**
     * RD19: alta manual de un ÍtemLista sobre los ítems actuales (upsert por
     * nombre normalizado, D3):
     * - Con `id` de un ítem existente lo edita (p. ej. desde el icono de edición).
     * - Sin `id`, un nombre normalizado existente se edita conservando su nombre
     *   y sus fuentes (D3/D7); si no existe, se crea uno nuevo sin fuentes
     *   (badge «Despensa baja», D5).
     * - Solo se reemplazan cantidad, unidad y Pasillo.
     * Los datos inválidos no cambian la lista (RF-AIL5).
     */
    fun saveManualItem(
        existing: List<ShoppingListItem>,
        id: String?,
        name: String,
        quantity: Double?,
        unit: String,
        aisle: Aisle?,
    ): List<ShoppingListItem> {
        if (!isManualNameValid(name) || !isManualQuantityValid(quantity)) return existing
        val normalized = normalizeName(name)
        val byId = id?.let { wanted -> existing.indexOfFirst { it.id == wanted } } ?: -1
        val index = if (byId >= 0) {
            byId
        } else {
            existing.indexOfFirst { normalizeName(it.name) == normalized }
        }
        if (index < 0) {
            return existing + ShoppingListItem(
                id = itemIdFor(name),
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim(),
                aisle = aisle,
                completed = false,
            )
        }
        val item = existing[index]
        return existing.toMutableList().also { result ->
            result[index] = item.copy(quantity = quantity, unit = unit.trim(), aisle = aisle)
        }
    }

    // --- Agrupación y progreso (RD20) ----------------------------------------

    /**
     * RD20: agrupa los ítems según el modo (D8):
     * - BY_AISLE: un grupo por `Aisle` en orden del enum y «Sin clasificar»
     *   (aisle null) al final.
     * - BY_RECIPE: un grupo por fuente ordenado por título normalizado
     *   (desempate por `sourceKey`) y «Sin receta» (sin fuentes) al final; un
     *   ítem multi-fuente se replica en cada grupo de sus fuentes (D2) y todas
     *   sus apariciones comparten el mismo `completed`.
     * Solo grupos no vacíos; dentro de cada grupo el orden es el de
     * `sortedItems` (nombre normalizado, id) con los completados en su sitio.
     */
    fun groupItems(
        items: List<ShoppingListItem>,
        mode: ShoppingGroupMode,
    ): List<ShoppingGroup> {
        val sorted = items.sortedWith(compareBy({ normalizeName(it.name) }, { it.id }))
        return when (mode) {
            ShoppingGroupMode.BY_AISLE -> {
                val byAisle = sorted.groupBy { it.aisle }
                Aisle.entries.mapNotNull { aisle ->
                    byAisle[aisle]?.let { ShoppingGroup(key = aisle.name, title = "", items = it) }
                } + listOfNotNull(
                    byAisle[null]?.let { ShoppingGroup(key = "", title = "", items = it) },
                )
            }

            ShoppingGroupMode.BY_RECIPE -> {
                val bySource = linkedMapOf<String, Pair<String, MutableList<ShoppingListItem>>>()
                val withoutSource = mutableListOf<ShoppingListItem>()
                sorted.forEach { item ->
                    if (item.sources.isEmpty()) {
                        withoutSource += item
                    } else {
                        item.sources.forEach { source ->
                            val group = bySource.getOrPut(source.sourceKey) {
                                source.sourceTitle to mutableListOf()
                            }
                            group.second += item
                        }
                    }
                }
                bySource.entries
                    .sortedWith(compareBy({ normalizeName(it.value.first) }, { it.key }))
                    .map { (key, titleAndItems) ->
                        ShoppingGroup(key = key, title = titleAndItems.first, items = titleAndItems.second)
                    } + listOfNotNull(
                    withoutSource.takeIf { it.isNotEmpty() }?.let {
                        ShoppingGroup(key = "", title = "", items = it)
                    },
                )
            }
        }
    }

    /**
     * RD20: progreso de la Lista sobre ítems únicos (D9): los multi-fuente
     * cuentan una vez y `percent` es el porcentaje redondeado al entero más
     * próximo (0 con la lista vacía).
     */
    fun progress(items: List<ShoppingListItem>): ShoppingProgress {
        val total = items.size
        val completed = items.count { it.completed }
        val percent = if (total == 0) 0 else (completed * 100.0 / total).roundToInt()
        return ShoppingProgress(completed = completed, total = total, percent = percent)
    }
}
