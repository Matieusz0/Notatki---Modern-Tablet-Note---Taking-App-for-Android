package com.notatki.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notatki.app.data.model.DrawingStroke
import com.notatki.app.data.model.StrokePoint
import kotlin.math.max
import kotlin.math.min

object GeometryUtils {

    /**
     * Sprawdza, czy dany punkt leży wewnątrz wielokąta pętli narysowanej lassem (Ray-Casting Algorithm)
     */
    fun isPointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]

            if ((pi.y > point.y) != (pj.y > point.y) &&
                point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Zwraca obrys prostokątny (Bounding Box) dla zbioru kresek
     */
    fun computeBoundingBox(strokes: List<DrawingStroke>): Rect? {
        if (strokes.isEmpty()) return null
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        var hasPoints = false
        for (stroke in strokes) {
            for (p in stroke.points) {
                hasPoints = true
                minX = min(minX, p.x)
                minY = min(minY, p.y)
                maxX = max(maxX, p.x)
                maxY = max(maxY, p.y)
            }
        }

        return if (hasPoints) Rect(minX - 10f, minY - 10f, maxX + 10f, maxY + 10f) else null
    }
}
