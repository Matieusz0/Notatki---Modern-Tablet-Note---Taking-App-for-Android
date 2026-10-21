package com.notatki.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.notatki.app.data.model.DrawingStroke
import com.notatki.app.data.model.NoteCanvasData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object PdfExporter {

    // Standardowy rozmiar kartki A4 w punktach PDF (72 DPI)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f

    suspend fun exportToPdf(
        context: Context,
        lessonTitle: String,
        noteData: NoteCanvasData
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            // 1. Oblicz granice wszystkich narysowanych kresek
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            val strokes = noteData.strokes
            if (strokes.isEmpty()) {
                // Pusta strona A4
                minX = 0f
                minY = 0f
                maxX = PAGE_WIDTH.toFloat()
                maxY = PAGE_HEIGHT.toFloat()
            } else {
                for (stroke in strokes) {
                    for (point in stroke.points) {
                        minX = min(minX, point.x)
                        minY = min(minY, point.y)
                        maxX = max(maxX, point.x)
                        maxY = max(maxY, point.y)
                    }
                }
            }

            val contentWidth = max(maxX - minX, 100f)
            val contentHeight = max(maxY - minY, 100f)

            val availableWidth = PAGE_WIDTH - (MARGIN * 2)
            val availableHeight = PAGE_HEIGHT - (MARGIN * 2)

            val scale = min(availableWidth / contentWidth, availableHeight / contentHeight).coerceAtMost(1.0f)

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            val gridPaint = Paint().apply {
                color = Color.parseColor("#E5E9F0")
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }
            val gridSize = 20f
            var gx = 0f
            while (gx < PAGE_WIDTH) {
                canvas.drawLine(gx, 0f, gx, PAGE_HEIGHT.toFloat(), gridPaint)
                gx += gridSize
            }
            var gy = 0f
            while (gy < PAGE_HEIGHT) {
                canvas.drawLine(0f, gy, PAGE_WIDTH.toFloat(), gy, gridPaint)
                gy += gridSize
            }

            canvas.save()
            canvas.translate(MARGIN, MARGIN)
            canvas.scale(scale, scale)
            canvas.translate(-minX, -minY)

            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            for (stroke in strokes) {
                if (stroke.points.size < 2) continue

                strokePaint.color = stroke.colorArgb.toInt()
                strokePaint.strokeWidth = stroke.strokeWidth

                if (stroke.isHighlighter) {
                    strokePaint.alpha = 100
                } else {
                    strokePaint.alpha = (stroke.colorArgb shr 24 and 0xFF).toInt().coerceAtLeast(255)
                }

                val path = Path()
                val first = stroke.points.first()
                path.moveTo(first.x, first.y)

                for (i in 1 until stroke.points.size) {
                    val p = stroke.points[i]
                    if (p.pressure == 0f) {
                        path.moveTo(p.x, p.y)
                    } else {
                        path.lineTo(p.x, p.y)
                    }
                }

                canvas.drawPath(path, strokePaint)
            }

            canvas.restore()
            pdfDocument.finishPage(page)

            // Zapisz do publicznego folderu Pobrane (Downloads) w pamięci wewnętrznej tabletu!
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val cleanTitle = lessonTitle.replace("[^a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ_\\-]".toRegex(), "_")
            val pdfFile = File(downloadsDir, "${cleanTitle}_notatka.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
