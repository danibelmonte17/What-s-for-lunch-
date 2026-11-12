package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanAnalysisParserTest {
    @Test
    fun parsesPlainJsonObject() {
        val foods = ScanAnalysisParser.parse("""{"manzana":3,"nata":1}""")

        assertEquals(listOf("manzana", "nata"), foods?.map { it.name })
        assertEquals(listOf(3.0, 1.0), foods?.map { it.quantity })
    }

    @Test
    fun toleratesMarkdownFencesAndSurroundingText() {
        val content = "Aquí tienes el resultado:\n```json\n{\"manzana\": 3}\n```\nEspero que ayude."

        val foods = ScanAnalysisParser.parse(content)

        assertEquals("manzana", foods?.single()?.name)
        assertEquals(3.0, foods?.single()?.quantity)
    }

    @Test
    fun recoversOnlyTheFirstJsonObject() {
        val foods = ScanAnalysisParser.parse("""{"manzana":3} {"pera":1}""")

        assertEquals(1, foods?.size)
        assertEquals("manzana", foods?.single()?.name)
    }

    @Test
    fun missingObjectIsAParseFailure() {
        assertNull(ScanAnalysisParser.parse("No se reconoce ningún alimento."))
        assertNull(ScanAnalysisParser.parse(""))
    }

    @Test
    fun nonNumericQuantitiesBecomeNullForDomainNormalization() {
        val foods = ScanAnalysisParser.parse("""{"sal":"un kilo","pimienta":null,"arroz":2}""")

        assertNull(foods?.first { it.name == "sal" }?.quantity)
        assertNull(foods?.first { it.name == "pimienta" }?.quantity)
        assertEquals(2.0, foods?.first { it.name == "arroz" }?.quantity)
    }

    @Test
    fun emptyObjectIsAnEmptySuccessfulAnalysis() {
        assertEquals(emptyList<Any>(), ScanAnalysisParser.parse("{}"))
    }
}
