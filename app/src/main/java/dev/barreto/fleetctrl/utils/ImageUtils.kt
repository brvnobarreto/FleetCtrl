package dev.barreto.fleetctrl.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object ImageUtils {
    
    /**
     * Converte uma imagem para Base64
     */
    fun imageToBase64(imagePath: String, maxDim: Int = 1280, quality: Int = 80): String? {
        return try {
            val file = File(imagePath)
            if (!file.exists()) return null

            // Primeiro tenta decodificar e comprimir para reduzir tamanho
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, bounds)
            val (w, h) = bounds.outWidth to bounds.outHeight
            if (w > 0 && h > 0) {
                // Calcular inSampleSize aproximado
                var sample = 1
                var tw = w
                var th = h
                while (tw / 2 >= maxDim || th / 2 >= maxDim) {
                    sample *= 2
                    tw /= 2
                    th /= 2
                }
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = BitmapFactory.decodeFile(imagePath, opts)
                if (decoded != null) {
                    // Redimensionar exatamente para caber em maxDim preservando proporção
                    val scale = maxOf(decoded.width.toFloat() / maxDim, decoded.height.toFloat() / maxDim, 1f)
                    val targetW = (decoded.width / scale).toInt()
                    val targetH = (decoded.height / scale).toInt()
                    val bitmap = if (scale > 1f) Bitmap.createScaledBitmap(decoded, targetW, targetH, true) else decoded

                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    val bytes = out.toByteArray()
                    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                }
            }

            // Fallback: lê bytes crús (pode exceder 1MiB)
            val inputStream = FileInputStream(file)
            val bytes = inputStream.readBytes()
            inputStream.close()
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: IOException) {
            println("Error converting image to Base64: ${e.message}")
            null
        }
    }
    
    /**
     * Converte Base64 para Bitmap
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            println("Error converting Base64 to Bitmap: ${e.message}")
            null
        }
    }
    
    /**
     * Salva Base64 como arquivo temporário
     */
    fun saveBase64AsTempFile(context: Context, base64: String, fileName: String): String? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            val tempFile = File(context.cacheDir, fileName)
            tempFile.writeBytes(decodedBytes)
            tempFile.absolutePath
        } catch (e: Exception) {
            println("Error saving Base64 as temp file: ${e.message}")
            null
        }
    }
    
    /**
     * Converte Bitmap para Base64
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
    }
}
