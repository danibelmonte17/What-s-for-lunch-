package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.text.normalizeName

/** Pure rules for editing the scan tray and saving it into the Pantry. */
object ScanDetections {
    /**
     * Merge chips that become duplicates after a rename/add. A directly edited chip
     * keeps its name and quantity.
     */
    fun mergeDraftItems(items: List<ScanDraftItem>, preferredId: String? = null): List<ScanDraftItem> {
        val grouped = linkedMapOf<String, MutableList<ScanDraftItem>>()
        items.forEach { item ->
            val normalizedName = normalizeName(item.name)
            val key = normalizedName.ifEmpty { "\u0000${item.id}" }
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }
        return grouped.values.map { group ->
            group.firstOrNull { it.id == preferredId } ?: group.first()
        }
    }

    fun rename(items: List<ScanDraftItem>, id: String, name: String): List<ScanDraftItem> =
        mergeDraftItems(items.map { if (it.id == id) it.copy(name = name) else it }, preferredId = id)

    fun updateQuantity(items: List<ScanDraftItem>, id: String, quantity: Int): List<ScanDraftItem> =
        items.map {
            if (it.id == id) it.copy(quantity = quantity.coerceIn(PantryItemValidation.MIN_QUANTITY, PantryItemValidation.MAX_QUANTITY))
            else it
        }

    fun matchesWithPantry(items: List<ScanDraftItem>, pantryItems: List<PantryItem>): Map<String, PantryItem> {
        val byNormalizedName = pantryItems.associateBy { normalizeName(it.name) }
        return items.mapNotNull { item ->
            byNormalizedName[normalizeName(item.name)]?.let { item.id to it }
        }.toMap()
    }

    /**
     * RD13: adds the chip quantity to an existing pantry item. «No quantity» behaves as 0
     * (null + N -> N, M + 0 -> M, null + 0 -> null) and the total is capped at the app
     * maximum without warning (60 + 50 -> 99). The existing id and name are preserved.
     */
    fun sumInto(existing: PantryItem, draft: ScanDraftItem): PantryItem {
        val total = (existing.quantity ?: PantryItemValidation.MIN_QUANTITY) + draft.quantity
        val merged = total.coerceAtMost(PantryItemValidation.MAX_QUANTITY)
        return existing.copy(quantity = merged.takeUnless { it == PantryItemValidation.MIN_QUANTITY })
    }

    fun toPantryItem(draft: ScanDraftItem, id: String = draft.id): PantryItem = PantryItem(
        id = id,
        name = draft.name.trim(),
        quantity = draft.quantity.takeUnless { it == PantryItemValidation.MIN_QUANTITY },
    )
}
