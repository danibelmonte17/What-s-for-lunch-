package com.whatsforlunch.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// D8: la key del build (local.properties) gana si no esta vacia; si no, fallback
// al almacen cifrado del dispositivo. La del build se recorta y nunca se persiste.
class PriorityAiKeyProviderTest {

    @Test
    fun buildKeyWinsOverStoredKey() {
        val provider = PriorityAiKeyProvider("sk-build", FakeProvider("sk-store"))

        assertEquals("sk-build", provider.apiKey())
    }

    @Test
    fun blankBuildKeyFallsBackToStoredKey() {
        val provider = PriorityAiKeyProvider("   ", FakeProvider("sk-store"))

        assertEquals("sk-store", provider.apiKey())
    }

    @Test
    fun buildKeyIsTrimmed() {
        val provider = PriorityAiKeyProvider("  sk-build  ", FakeProvider("sk-store"))

        assertEquals("sk-build", provider.apiKey())
    }

    @Test
    fun noBuildKeyAndNoStoredKeyReturnsNull() {
        val provider = PriorityAiKeyProvider("", FakeProvider(null))

        assertNull(provider.apiKey())
    }

    private class FakeProvider(private val key: String?) : AiKeyProvider {
        override fun apiKey(): String? = key
    }
}
