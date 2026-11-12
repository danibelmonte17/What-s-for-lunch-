package com.whatsforlunch.app.domain.pantry

import com.whatsforlunch.app.domain.model.PantryItem
import com.whatsforlunch.app.domain.text.normalizeName
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Frescura de un Alimento según su fecha de caducidad (RD28, D8). */
enum class Freshness { GOOD, EXPIRING_SOON }

/**
 * Texto de caducidad que la UI traduce a `strings.xml` (P19): la regla vive en
 * dominio y el formato localizado en recursos (RD28, D8/D13).
 */
sealed interface ExpiryText {
    /** Sin fecha de caducidad → «Larga duración». */
    data object LongShelf : ExpiryText

    /** Fecha anterior a hoy → «Caducada». */
    data object Expired : ExpiryText

    /** Fecha igual a hoy → «Caduca hoy». */
    data object Today : ExpiryText

    /** Fecha igual a mañana → «Caduca mañana». */
    data object Tomorrow : ExpiryText

    /** Fecha a N días (N ≥ 2) → «Caduca en N días». */
    data class InDays(val days: Int) : ExpiryText
}

/**
 * Reglas puras de frescura, capacidad y consejo de cocina de la Despensa
 * (`redisenar-vista-de-la-despensa`, D8-D11/D13). Sin dependencias de Android;
 * `today` la aporta el llamador para poder testear (P25).
 */
object FreshnessRules {
    /** D8: ≤ 2 días restantes (hoy, mañana y fecha pasada incluidas) → «Consumir pronto». */
    const val EXPIRING_SOON_DAYS = 2

    /** D10: capacidad de referencia de la tarjeta de resumen (la cifra del prototipo es ilustrativa). */
    const val CAPACITY_REFERENCE = 40

    fun daysRemaining(expiry: LocalDate, today: LocalDate): Long =
        ChronoUnit.DAYS.between(today, expiry)

    fun freshness(expiry: LocalDate?, today: LocalDate): Freshness = when {
        expiry == null -> Freshness.GOOD
        daysRemaining(expiry, today) <= EXPIRING_SOON_DAYS -> Freshness.EXPIRING_SOON
        else -> Freshness.GOOD
    }

    fun expiryText(expiry: LocalDate?, today: LocalDate): ExpiryText = when {
        expiry == null -> ExpiryText.LongShelf
        expiry.isBefore(today) -> ExpiryText.Expired
        expiry.isEqual(today) -> ExpiryText.Today
        expiry.isEqual(today.plusDays(1)) -> ExpiryText.Tomorrow
        else -> ExpiryText.InDays(daysRemaining(expiry, today).toInt())
    }

    /** D10: `round(min(100, N / 40 * 100))` con capacidad de referencia 40 Alimentos. */
    fun pantryCapacityPercent(items: Collection<PantryItem>): Int =
        (items.size * 100.0 / CAPACITY_REFERENCE).roundToInt().coerceIn(0, 100)

    /** D9: «N por caducar» cuenta los Alimentos «Consumir pronto». */
    fun expiringCount(items: Collection<PantryItem>, today: LocalDate): Int =
        items.count { freshness(it.expiryDate, today) == Freshness.EXPIRING_SOON }

    /** D11: hasta `limit` nombres del consejo, en orden alfabético normalizado. */
    fun expiringNames(items: Collection<PantryItem>, today: LocalDate, limit: Int = 2): List<String> =
        items.filter { freshness(it.expiryDate, today) == Freshness.EXPIRING_SOON }
            .map { it.name }
            .sortedWith(compareBy { normalizeName(it) })
            .take(limit)
}
