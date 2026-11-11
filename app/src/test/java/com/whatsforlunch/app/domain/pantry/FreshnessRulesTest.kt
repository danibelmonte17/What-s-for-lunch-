package com.whatsforlunch.app.domain.pantry

import com.whatsforlunch.app.domain.model.PantryItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class FreshnessRulesTest {
    private val today: LocalDate = LocalDate.of(2026, 11, 11)

    private fun item(id: String, name: String, expiry: LocalDate? = null) =
        PantryItem(id, name, expiryDate = expiry)

    // S7 / verify 7 (D8): ≤ 2 días restantes —hoy, mañana y fecha pasada
    // incluidas— es «Consumir pronto»; más de 2 días o sin fecha, «En buen estado».
    @Test
    fun classifiesFreshnessByDaysRemaining() {
        assertEquals(Freshness.GOOD, FreshnessRules.freshness(today.plusDays(3), today))
        assertEquals(Freshness.EXPIRING_SOON, FreshnessRules.freshness(today.plusDays(2), today))
        assertEquals(Freshness.EXPIRING_SOON, FreshnessRules.freshness(today.plusDays(1), today))
        assertEquals(Freshness.EXPIRING_SOON, FreshnessRules.freshness(today, today))
        assertEquals(Freshness.EXPIRING_SOON, FreshnessRules.freshness(today.minusDays(1), today))
        assertEquals(Freshness.GOOD, FreshnessRules.freshness(null, today))
    }

    // S7 / verify 7 (D8/D13): los textos de caducidad son estados puros que la
    // UI traduce a `strings.xml` (P19); los literales se comprueban en E2E.
    @Test
    fun mapsExpiryToDisplayStates() {
        assertEquals(ExpiryText.InDays(3), FreshnessRules.expiryText(today.plusDays(3), today))
        assertEquals(ExpiryText.InDays(2), FreshnessRules.expiryText(today.plusDays(2), today))
        assertEquals(ExpiryText.Tomorrow, FreshnessRules.expiryText(today.plusDays(1), today))
        assertEquals(ExpiryText.Today, FreshnessRules.expiryText(today, today))
        assertEquals(ExpiryText.Expired, FreshnessRules.expiryText(today.minusDays(1), today))
        assertEquals(ExpiryText.LongShelf, FreshnessRules.expiryText(null, today))
    }

    // D10: capacidad = round(min(100, N / 40 * 100)) con capacidad de referencia
    // 40 Alimentos (25 → 63 %, verify 7).
    @Test
    fun computesCapacityPercentCappedAtHundred() {
        assertEquals(63, FreshnessRules.pantryCapacityPercent(List(25) { item("$it", "Alimento $it") }))
        assertEquals(0, FreshnessRules.pantryCapacityPercent(emptyList()))
        assertEquals(100, FreshnessRules.pantryCapacityPercent(List(40) { item("$it", "Alimento $it") }))
        assertEquals(100, FreshnessRules.pantryCapacityPercent(List(99) { item("$it", "Alimento $it") }))
    }

    // D9: «N por caducar» cuenta los Alimentos «Consumir pronto».
    @Test
    fun countsExpiringItems() {
        val items = listOf(
            item("1", "Tomate", today.plusDays(1)),
            item("2", "Harina", today.plusDays(10)),
            item("3", "Leche", today),
            item("4", "Arroz", null),
        )
        assertEquals(2, FreshnessRules.expiringCount(items, today))
        assertEquals(0, FreshnessRules.expiringCount(listOf(item("1", "Arroz")), today))
    }

    // D11: hasta 2 nombres para el consejo, en orden alfabético normalizado
    // (mayúsculas y acentos sin importar).
    @Test
    fun listsExpiringNamesSortedAndLimited() {
        val items = listOf(
            item("1", "Zanahoria", today),
            item("2", "Aguacate", today),
            item("3", "Huevos", today.plusDays(5)),
            item("4", "Ñoquis", today.minusDays(1)),
        )
        assertEquals(listOf("Aguacate", "Ñoquis"), FreshnessRules.expiringNames(items, today))
        assertEquals(listOf("Aguacate"), FreshnessRules.expiringNames(items, today, limit = 1))
        assertEquals(emptyList<String>(), FreshnessRules.expiringNames(listOf(item("3", "Huevos", today.plusDays(5))), today))
    }
}
