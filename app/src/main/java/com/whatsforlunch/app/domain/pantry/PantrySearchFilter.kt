package com.whatsforlunch.app.domain.pantry

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.text.normalizeName

/**
 * Filtra los Alimentos por nombre con coincidencia «contiene» sobre el nombre
 * normalizado (D18, `redisenar-vista-de-la-despensa`); una consulta vacía o en
 * blanco devuelve la lista completa. Mismo patrón que `filterRecipes`
 * (`domain/myrecipes/RecipeSearchFilter.kt`).
 */
fun filterPantryItems(items: List<PantryItem>, query: String): List<PantryItem> {
    val normalizedQuery = normalizeName(query)
    if (normalizedQuery.isEmpty()) return items
    return items.filter { normalizeName(it.name).contains(normalizedQuery) }
}
