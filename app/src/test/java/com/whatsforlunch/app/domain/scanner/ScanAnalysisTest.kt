package com.whatsforlunch.app.domain.scanner

import com.whatsforlunch.app.domain.model.ScanDraftItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanAnalysisTest {
    @Test
    fun invalidQuantitiesBecomeNoQuantityAndAboveMaximumAreCapped() {
        assertEquals(0, ScanAnalysis.normalizedQuantity(null))
        assertEquals(0, ScanAnalysis.normalizedQuantity(0.0))
        assertEquals(0, ScanAnalysis.normalizedQuantity(-3.0))
        assertEquals(0, ScanAnalysis.normalizedQuantity(1.5))
        assertEquals(0, ScanAnalysis.normalizedQuantity(Double.NaN))
        assertEquals(99, ScanAnalysis.normalizedQuantity(150.0))
        assertEquals(3, ScanAnalysis.normalizedQuantity(3.0))
    }

    @Test
    fun newProposalsEnterWithTheirQuantityAndBlankNamesAreDiscarded() {
        val merged = ScanAnalysis.mergeProposals(
            emptyList(),
            listOf(
                AnalyzedFood("Manzana", 3.0),
                AnalyzedFood("   ", 4.0),
                AnalyzedFood("Nata", 1.0),
            ),
        )

        assertEquals(listOf("Manzana", "Nata"), merged.map { it.name })
        assertEquals(listOf(3, 1), merged.map { it.quantity })
    }

    @Test
    fun duplicateProposalsInsideOneResponseFuseAndSumQuantities() {
        val merged = ScanAnalysis.mergeProposals(
            emptyList(),
            listOf(
                AnalyzedFood("TOMATE FRESCO", 2.0),
                AnalyzedFood("  Tomate  fresco ", 3.0),
            ),
        )

        assertEquals(1, merged.size)
        assertEquals("TOMATE FRESCO", merged.single().name)
        assertEquals(5, merged.single().quantity)
    }

    @Test
    fun successivePhotosAccumulateWithoutOverwritingUserEdits() {
        val edited = ScanDraftItem("Manzana", quantity = 2, id = "edited")
        val merged = ScanAnalysis.mergeProposals(
            listOf(edited),
            listOf(AnalyzedFood("manzana", 5.0), AnalyzedFood("pera", 1.0)),
        )

        assertEquals(listOf("Manzana", "pera"), merged.map { it.name })
        assertEquals(2, merged.first().quantity)
        assertEquals("edited", merged.first().id)
        assertEquals(1, merged.last().quantity)
    }

    @Test
    fun duplicateProposalSumIsCappedAtMaximum() {
        val merged = ScanAnalysis.mergeProposals(
            emptyList(),
            listOf(AnalyzedFood("Arroz", 60.0), AnalyzedFood("arroz", 80.0)),
        )

        assertEquals(99, merged.single().quantity)
    }

    @Test
    fun invalidProposalQuantityKeepsTheFoodWithoutQuantity() {
        val merged = ScanAnalysis.mergeProposals(
            emptyList(),
            listOf(AnalyzedFood("Leche", 0.5), AnalyzedFood("Sal", null)),
        )

        assertEquals(listOf("Leche", "Sal"), merged.map { it.name })
        assertEquals(listOf(0, 0), merged.map { it.quantity })
    }
}
