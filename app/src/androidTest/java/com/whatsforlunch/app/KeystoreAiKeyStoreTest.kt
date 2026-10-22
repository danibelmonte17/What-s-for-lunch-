package com.whatsforlunch.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whatsforlunch.app.data.ai.KeystoreAiKeyStore
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Escenario 2 (D2, ⚠ S05): la key se guarda cifrada en SharedPreferences, el
// round-trip la recupera, clear() la elimina y unos datos corruptos no rompen.
@RunWith(AndroidJUnit4::class)
class KeystoreAiKeyStoreTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun resetPreferences() {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun savedKeyIsCiphertextOnDiskAndRoundTrips() {
        val store = KeystoreAiKeyStore(context)
        val key = "sk-test-ciphertext-1234567890"

        store.save(key)

        val stored = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, null)
        assertNotNull(stored)
        assertFalse(stored!!.contains(key))

        val preferencesFile = File(context.dataDir, "shared_prefs/$PREFERENCES_NAME.xml")
        assertTrue(preferencesFile.exists())
        assertFalse(preferencesFile.readText().contains(key))

        assertEquals(key, store.apiKey())
    }

    @Test
    fun clearRemovesTheKey() {
        val store = KeystoreAiKeyStore(context)
        store.save("sk-test-to-delete")
        assertEquals("sk-test-to-delete", store.apiKey())

        store.clear()

        assertNull(store.apiKey())
        assertNull(
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_KEY, null),
        )
    }

    @Test
    fun corruptedCiphertextReturnsNullWithoutCrashing() {
        val store = KeystoreAiKeyStore(context)
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, "not-a-valid-encrypted-payload")
            .commit()

        assertNull(store.apiKey())
    }

    private companion object {
        const val PREFERENCES_NAME = "ai_key_prefs"
        const val KEY_API_KEY = "api_key"
    }
}
