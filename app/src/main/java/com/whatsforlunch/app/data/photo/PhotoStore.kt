package com.whatsforlunch.app.data.photo

import android.content.Context
import java.io.File

// Almacén de fotos de Recetas: copia los bytes a filesDir/recipes/<recipeId>.jpg
// y devuelve la ruta. La Receta guarda esa ruta (RD06 aplica solo a fotos de Escaneo).
interface PhotoStore {
    suspend fun save(recipeId: String, bytes: ByteArray): String

    suspend fun delete(path: String)
}

class FilePhotoStore(private val context: Context) : PhotoStore {

    override suspend fun save(recipeId: String, bytes: ByteArray): String {
        val dir = File(context.filesDir, RECIPES_DIR)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "$recipeId.jpg")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    override suspend fun delete(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }

    private companion object {
        const val RECIPES_DIR = "recipes"
    }
}
