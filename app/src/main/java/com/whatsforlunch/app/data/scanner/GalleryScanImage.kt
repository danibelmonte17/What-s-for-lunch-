package com.whatsforlunch.app.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.graphics.scale
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads a photo picked from the gallery and returns the same in-memory JPEG the
 * camera produces (D6): at most [MAX_SIDE] px on the longest side, quality 85 and
 * EXIF rotation applied. Nothing is ever written to disk (RD06). Returns null when
 * the image cannot be read or decoded.
 */
suspend fun readGalleryScanJpeg(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    val original = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    } catch (_: LinkageError) {
        null
    } ?: return@withContext null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(original, 0, original.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

    // Muestreo previo para no decodificar fotos de alta resolución a tamaño completo.
    var sampleSize = 1
    var longestSide = max(bounds.outWidth, bounds.outHeight)
    while (longestSide / 2 >= MAX_SIDE) {
        sampleSize *= 2
        longestSide /= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeByteArray(original, 0, original.size, options)
        ?: return@withContext null

    val source = decoded.withExifRotation(original)
    return@withContext try {
        val scaled = source.scaledToMaxSide()
        try {
            ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.toByteArray()
            }
        } finally {
            if (scaled !== source) scaled.recycle()
            source.recycle()
        }
    } catch (_: Exception) {
        null
    } catch (_: LinkageError) {
        null
    }
}

private fun Bitmap.withExifRotation(jpeg: ByteArray): Bitmap {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(jpeg))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    } catch (_: LinkageError) {
        ExifInterface.ORIENTATION_NORMAL
    }
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees == 0f) return this
    return try {
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { rotated ->
            if (rotated !== this) recycle()
        }
    } catch (_: Exception) {
        this
    } catch (_: LinkageError) {
        this
    }
}

private fun Bitmap.scaledToMaxSide(): Bitmap {
    val longestSide = max(width, height)
    if (longestSide <= MAX_SIDE) return this
    val factor = MAX_SIDE.toFloat() / longestSide
    return scale(
        (width * factor).roundToInt().coerceAtLeast(1),
        (height * factor).roundToInt().coerceAtLeast(1),
        filter = true,
    )
}

private const val MAX_SIDE = 1024
private const val JPEG_QUALITY = 85
