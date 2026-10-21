package com.notatki.app.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.notatki.app.data.model.StrokePoint
import kotlin.math.*

object ShapeRecognizer {

    sealed class RecognizedShape {
        data class Line(val start: StrokePoint, val end: StrokePoint) : RecognizedShape()
        data class Circle(val center: Offset, val radius: Float) : RecognizedShape()
        data class Rectangle(val rect: Rect) : RecognizedShape()
        data class Triangle(val p1: Offset, val p2: Offset, val p3: Offset) : RecognizedShape()
    }

    /**
     * Precyzyjne rozpoznawanie kształtów dla rysika (Linia, Okrąg, Prostokąt, Trójkąt)
     */
    fun detectShape(points: List<StrokePoint>): RecognizedShape? {
        if (points.size < 6) return null

        val first = points.first()
        val last = points.last()
        val startEndDist = hypot(first.x - last.x, first.y - last.y)

        var totalLength = 0f
        for (i in 1 until points.size) {
            totalLength += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }

        if (totalLength <= 10f) return null

        // 1. Sprawdź czy to prosta linia
        val straightness = startEndDist / totalLength
        if (straightness > 0.86f) {
            var maxDev = 0f
            val lineA = Offset(first.x, first.y)
            val lineB = Offset(last.x, last.y)
            for (p in points) {
                val dev = pointToLineDistance(Offset(p.x, p.y), lineA, lineB)
                if (dev > maxDev) maxDev = dev
            }
            if (maxDev < 40f) {
                return RecognizedShape.Line(first, last)
            }
        }

        // 2. Sprawdź czy pętla (zamknięty kształt)
        val isClosed = startEndDist < (totalLength * 0.35f) || startEndDist < 80f
        if (!isClosed) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (p in points) {
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = max(maxX, p.x)
            maxY = max(maxY, p.y)
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width < 15f || height < 15f) return null

        val center = Offset((minX + maxX) / 2f, (minY + maxY) / 2f)
        val avgRadius = (width + height) / 4f

        // 3. Najpierw sprawdź wierzchołki dla Trójkątów i Prostokątów (zapobiega myleniu kwadratów z kółkami)
        val corners = findCornerVertices(points)
        if (corners.size == 3) {
            return RecognizedShape.Triangle(corners[0], corners[1], corners[2])
        } else if (corners.size == 4) {
            return RecognizedShape.Rectangle(Rect(minX, minY, maxX, maxY))
        }

        // 4. Sprawdź czy to Okrąg (z rygorystycznym progiem odchylenia do 10%, bo kwadrat ma ~20%)
        var radiusDevSum = 0f
        for (p in points) {
            val dist = hypot(p.x - center.x, p.y - center.y)
            radiusDevSum += abs(dist - avgRadius)
        }
        val avgRadiusDev = radiusDevSum / points.size

        if (avgRadiusDev < avgRadius * 0.10f) {
            return RecognizedShape.Circle(center, avgRadius)
        }

        // 5. Użyj uproszczenia RDP do detekcji wierzchołków
        val offsetPoints = points.map { Offset(it.x, it.y) }
        val simplified = ramerDouglasPeucker(offsetPoints, totalLength * 0.07f)
        val uniqueVertices = mutableListOf<Offset>()
        for (pt in simplified) {
            if (uniqueVertices.none { hypot(it.x - pt.x, it.y - pt.y) < 25f }) {
                uniqueVertices.add(pt)
            }
        }

        if (uniqueVertices.size == 3) {
            return RecognizedShape.Triangle(uniqueVertices[0], uniqueVertices[1], uniqueVertices[2])
        } else if (uniqueVertices.size == 4) {
            return RecognizedShape.Rectangle(Rect(minX, minY, maxX, maxY))
        }

        return null
    }

    private fun findCornerVertices(points: List<StrokePoint>): List<Offset> {
        val step = max(2, points.size / 12)
        val candidates = mutableListOf<Pair<Int, Float>>()

        for (i in step until points.size - step) {
            val pPrev = Offset(points[i - step].x, points[i - step].y)
            val pCurr = Offset(points[i].x, points[i].y)
            val pNext = Offset(points[i + step].x, points[i + step].y)

            val v1 = pCurr - pPrev
            val v2 = pNext - pCurr

            val mag1 = hypot(v1.x, v1.y)
            val mag2 = hypot(v2.x, v2.y)

            if (mag1 > 0f && mag2 > 0f) {
                val dot = (v1.x * v2.x + v1.y * v2.y) / (mag1 * mag2)
                val cosTheta = dot.coerceIn(-1f, 1f)
                val angleChange = acos(cosTheta)
                if (angleChange > 0.52f) { // zmiana kierunku > ~30 stopni
                    candidates.add(i to angleChange)
                }
            }
        }

        val corners = mutableListOf<Offset>()
        if (candidates.isNotEmpty()) {
            var best = candidates[0]
            for (i in 1 until candidates.size) {
                if (candidates[i].first - best.first < step * 3) {
                    if (candidates[i].second > best.second) {
                        best = candidates[i]
                    }
                } else {
                    corners.add(Offset(points[best.first].x, points[best.first].y))
                    best = candidates[i]
                }
            }
            corners.add(Offset(points[best.first].x, points[best.first].y))
        }

        return corners
    }

    private fun ramerDouglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points

        var maxDist = 0f
        var index = 0
        val lineStart = points.first()
        val lineEnd = points.last()

        for (i in 1 until points.size - 1) {
            val dist = pointToLineDistance(points[i], lineStart, lineEnd)
            if (dist > maxDist) {
                maxDist = dist
                index = i
            }
        }

        return if (maxDist > epsilon) {
            val left = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val right = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(lineStart, lineEnd)
        }
    }

    fun shapeToStrokePoints(shape: RecognizedShape, avgPressure: Float = 1.0f): List<StrokePoint> {
        return when (shape) {
            is RecognizedShape.Line -> {
                listOf(shape.start, shape.end)
            }
            is RecognizedShape.Circle -> {
                val result = mutableListOf<StrokePoint>()
                val steps = 48
                for (i in 0..steps) {
                    val angle = (i.toFloat() / steps) * (2 * Math.PI)
                    val x = shape.center.x + (shape.radius * cos(angle)).toFloat()
                    val y = shape.center.y + (shape.radius * sin(angle)).toFloat()
                    result.add(StrokePoint(x, y, avgPressure))
                }
                result
            }
            is RecognizedShape.Rectangle -> {
                val r = shape.rect
                listOf(
                    StrokePoint(r.left, r.top, avgPressure),
                    StrokePoint(r.right, r.top, avgPressure),
                    StrokePoint(r.right, r.bottom, avgPressure),
                    StrokePoint(r.left, r.bottom, avgPressure),
                    StrokePoint(r.left, r.top, avgPressure)
                )
            }
            is RecognizedShape.Triangle -> {
                listOf(
                    StrokePoint(shape.p1.x, shape.p1.y, avgPressure),
                    StrokePoint(shape.p2.x, shape.p2.y, avgPressure),
                    StrokePoint(shape.p3.x, shape.p3.y, avgPressure),
                    StrokePoint(shape.p1.x, shape.p1.y, avgPressure)
                )
            }
        }
    }

    private fun pointToLineDistance(point: Offset, lineA: Offset, lineB: Offset): Float {
        val dx = lineB.x - lineA.x
        val dy = lineB.y - lineA.y
        val lineLen = hypot(dx, dy)
        if (lineLen == 0f) return hypot(point.x - lineA.x, point.y - lineA.y)

        val area = abs(dy * point.x - dx * point.y + lineB.x * lineA.y - lineB.y * lineA.x)
        return area / lineLen
    }
}
