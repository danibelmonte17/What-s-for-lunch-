package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.ScanDraftItem
import com.whatsforlunch.app.domain.pantry.PantryItemValidation
import com.whatsforlunch.app.domain.text.normalizeName

/** One food proposal returned by the AI image analysis (D4). */
data class AnalyzedFood(
    val name: String,
    val quantity: Double? = null,
)

/** Pure rules for turning AI proposals into editable scan chips (D1, D2). */
object ScanAnalysis {

    /**
     * D2: non-integer, missing or non-positive quantities become «no quantity» (0) and
     * values above the app maximum are capped at 99. The food is never discarded.
     */
    fun normalizedQuantity(value: Double?): Int {
        if (value == null || !value.isFinite() || value % 1.0 != 0.0 || value <= 0.0) {
            return PantryItemValidation.MIN_QUANTITY
        }
        return value.toInt().coerceAtMost(PantryItemValidation.MAX_QUANTITY)
    }

    /**
     * D1: each photo is merged into the current tray by normalized name. Existing chips
     * keep their edited name and quantity; new foods enter with the proposed quantity.
     * Duplicate proposals inside one response are fused into a single chip by adding
     * their quantities (capped at 99). Blank names are discarded.
     */
    fun mergeProposals(items: List<ScanDraftItem>, proposals: List<AnalyzedFood>): List<ScanDraftItem> {
        val merged = items.toMutableList()
        val grouped = linkedMapOf<String, ProposalGroup>()
        proposals.forEach { proposal ->
            val readableName = proposal.name.trim().replace(WHITESPACE, " ")
            val normalized = normalizeName(readableName)
            if (normalized.isEmpty()) return@forEach
            val group = grouped.getOrPut(normalized) { ProposalGroup(readableName) }
            group.quantity = (group.quantity + normalizedQuantity(proposal.quantity))
                .coerceAtMost(PantryItemValidation.MAX_QUANTITY)
        }
        grouped.forEach { (normalized, group) ->
            val exists = merged.any { normalizeName(it.name) == normalized }
            if (!exists) {
                merged.add(ScanDraftItem(name = group.name, quantity = group.quantity))
            }
        }
        return merged
    }

    private class ProposalGroup(val name: String) {
        var quantity: Int = PantryItemValidation.MIN_QUANTITY
    }

    private val WHITESPACE = Regex("\\s+")
}
