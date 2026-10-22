package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.DetectedFood
import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.text.normalizeName

/** Pure rules for converting detector output into an editable scan tray. */
object ScanDetections {
    const val MIN_CONFIDENCE = 0.60

    /** Minimum interval between two live inferences (D1 of the live-detection spec). */
    const val LIVE_SAMPLE_INTERVAL_MS = 300L

    fun eligible(detections: List<DetectedFood>): List<DetectedFood> = detections.filter {
        it.name.isNotBlank() && it.confidence in MIN_CONFIDENCE..1.0
    }

    /**
     * Accumulates live detections into the current tray: every food enters once per
     * normalized name and only its confidence is updated (best score observed, never
     * below the current one); user-edited names and quantities are never overwritten.
     */
    fun mergeLiveDetections(items: List<ScanDraftItem>, detections: List<DetectedFood>): List<ScanDraftItem> {
        val merged = items.toMutableList()
        val grouped = linkedMapOf<String, MutableList<DetectedFood>>()
        eligible(detections).forEach { detection ->
            grouped.getOrPut(normalizeName(detection.name)) { mutableListOf() }.add(detection)
        }
        grouped.forEach { (normalized, group) ->
            val best = group.maxBy(DetectedFood::confidence)
            val index = merged.indexOfFirst { normalizeName(it.name) == normalized }
            if (index >= 0) {
                val existing = merged[index]
                merged[index] = existing.copy(confidence = maxOf(existing.confidence ?: 0.0, best.confidence))
            } else {
                merged.add(
                    ScanDraftItem(
                        name = mostReadableName(group.map(DetectedFood::name)),
                        confidence = best.confidence,
                    ),
                )
            }
        }
        return merged
    }

    fun toDraftItems(detections: List<DetectedFood>): List<ScanDraftItem> {
        val grouped = linkedMapOf<String, MutableList<DetectedFood>>()
        eligible(detections).forEach { detection ->
            grouped.getOrPut(normalizeName(detection.name)) { mutableListOf() }.add(detection)
        }
        return grouped.values.map { group ->
            val highest = group.maxBy { it.confidence }
            ScanDraftItem(
                name = mostReadableName(group.map(DetectedFood::name)),
                confidence = highest.confidence,
            )
        }
    }

    /**
     * Merge chips that become duplicates after a rename/add. A directly edited chip
     * keeps its name and quantity; confidence is always the best available score.
     */
    fun mergeDraftItems(items: List<ScanDraftItem>, preferredId: String? = null): List<ScanDraftItem> {
        val grouped = linkedMapOf<String, MutableList<ScanDraftItem>>()
        items.forEach { item ->
            val normalizedName = normalizeName(item.name)
            val key = normalizedName.ifEmpty { "\u0000${item.id}" }
            grouped.getOrPut(key) { mutableListOf() }.add(item)
        }
        return grouped.values.map { group ->
            val preferred = group.firstOrNull { it.id == preferredId } ?: group.first()
            preferred.copy(confidence = group.mapNotNull(ScanDraftItem::confidence).maxOrNull())
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

    private fun mostReadableName(names: List<String>): String = names
        .map { it.trim().replace(Regex("\\s+"), " ") }
        .minWithOrNull(
            compareBy<String>({ capitalizationPenalty(it) }, { it.count(Char::isWhitespace) }, { it.length }, { it }),
        )
        .orEmpty()

    private fun capitalizationPenalty(value: String): Int = when {
        value.isEmpty() -> 3
        value == value.uppercase() -> 2
        value == value.lowercase() -> 1
        else -> 0
    }
}
