package com.notatki.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object PdfSlideRenderer {

    /**
     * Kopiuje plik z ContentResolver (SAF) do lokalnego pliku podręcznego,
     * zapobiegając zawieszaniu interfejsu i blokowaniu deskryptorów plików.
     */
    suspend fun copyUriToCache(context: Context, uri: Uri, fileNamePrefix: String): File? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(context.cacheDir, "${fileNamePrefix}_${UUID.randomUUID()}.pdf")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (cacheFile.exists() && cacheFile.length() > 0) cacheFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getPageCountForFile(file: File): Int = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return@withContext 0
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun renderPageThumbnailFromFile(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 360
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val aspect = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            page.close()
            renderer.close()
            pfd.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun renderPageToImageFile(
        context: Context,
        pdfFile: File,
        pageIndex: Int,
        targetWidth: Int = 1600
    ): File? = withContext(Dispatchers.IO) {
        try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY) ?: return@withContext null
            val renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                pfd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val aspect = page.height.toFloat() / page.width.toFloat()
            val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

            page.close()
            renderer.close()
            pfd.close()

            val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val outputFile = File(imagesDir, "slide_${UUID.randomUUID()}.png")

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            bitmap.recycle()
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Kopiuje wybrane zdjęcie do folderu aplikacji i przeskalowuje je, aby nie przeciążać pamięci RAM.
     */
    suspend fun saveAndDownsampleImage(context: Context, imageUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }
            val outputFile = File(imagesDir, "img_${UUID.randomUUID()}.png")

            // 1. Dekoduj tylko wymiary obrazu
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val maxDimension = 1400
            var sampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // 2. Dekoduj przeskalowany obraz
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = context.contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext null

            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
