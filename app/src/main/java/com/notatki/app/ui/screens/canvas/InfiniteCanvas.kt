package com.notatki.app.ui.screens.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notatki.app.data.model.DrawingStroke
import com.notatki.app.data.model.EraserType
import com.notatki.app.data.model.StrokePoint
import com.notatki.app.ui.screens.canvas.components.ActiveTool
import com.notatki.app.ui.theme.NeonCyan
import com.notatki.app.util.GeometryUtils
import kotlin.math.hypot

@Composable
fun InfiniteCanvas(
    strokes: List<DrawingStroke>,
    currentStroke: DrawingStroke?,
    activeTool: ActiveTool,
    eraserType: EraserType,
    stylusOnlyMode: Boolean,
    paperStyle: com.notatki.app.data.model.PaperStyle,
    isDarkTheme: Boolean,
    lassoPoints: List<Offset> = emptyList(),
    selectedStrokeIds: Set<String> = emptySet(),
    canvasImages: List<CanvasImage> = emptyList(),
    selectedImageId: String? = null,
    showRuler: Boolean = false,
    onStrokeStart: (StrokePoint) -> Unit,
    onStrokeMove: (StrokePoint) -> Unit,
    onStrokeEnd: () -> Unit,
    onDragShapeUpdate: (Offset, Offset) -> Unit = { _, _ -> },
    onDragTemplateUpdate: (Offset, Offset) -> Unit = { _, _ -> },
    onInsertFormulaAt: (Offset) -> Unit = {},
    onObjectErase: (Offset) -> Unit,
    onPixelErase: (Offset, Float) -> Unit,
    onLassoMove: (Offset) -> Unit = {},
    onLassoEnd: () -> Unit = {},
    onMoveSelectedStrokes: (Float, Float) -> Unit = { _, _ -> },
    onSelectImage: (String?) -> Unit = {},
    onUpdateImageTransform: (String, Float, Float, Float, Float) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    // Stan transformacji tablicy (Pan i Zoom)
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var shapeStartPos by remember { mutableStateOf<Offset?>(null) }

    fun screenToCanvas(screenPoint: Offset): Offset {
        return (screenPoint - panOffset) / zoomScale
    }

    // Pamięć podręczna (Cache) gotowych obiektów Path w sprzętowych współrzędnych płótna
    val strokePathsMap = remember(strokes) {
        strokes.associate { stroke ->
            stroke.id to buildStrokePath(stroke)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activeTool, stylusOnlyMode, eraserType, panOffset, zoomScale, selectedStrokeIds, selectedImageId, canvasImages) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes
                            if (pointers.isEmpty()) continue

                            val primaryPointer = pointers.first()
                            val isTouch = primaryPointer.type == PointerType.Touch
                            val isMultiTouch = pointers.size > 1

                            // 1. Dwa palce ZAWSZE przesuwają i skalują tablicę (lub zaznaczone zdjęcie)
                            if (isMultiTouch) {
                                val p1 = pointers[0]
                                val p2 = pointers[1]
                                val prevDiff = p1.previousPosition - p2.previousPosition
                                val currDiff = p1.position - p2.position
                                val prevDist = prevDiff.getDistance()
                                val currDist = currDiff.getDistance()

                                val scaleFactor = if (prevDist > 0f) currDist / prevDist else 1f
                                val panDelta = (p1.position - p1.previousPosition + (p2.position - p2.previousPosition)) / 2f

                                val prevAngle = kotlin.math.atan2(prevDiff.y, prevDiff.x)
                                val currAngle = kotlin.math.atan2(currDiff.y, currDiff.x)
                                val angleDeltaRad = currAngle - prevAngle
                                val angleDeltaDeg = Math.toDegrees(angleDeltaRad.toDouble()).toFloat()

                                if (selectedImageId != null) {
                                    val canvasDelta = panDelta / zoomScale
                                    onUpdateImageTransform(selectedImageId, canvasDelta.x, canvasDelta.y, scaleFactor, angleDeltaDeg)
                                } else {
                                    zoomScale = (zoomScale * scaleFactor).coerceIn(0.3f, 4.0f)
                                    panOffset += panDelta
                                }

                                p1.consume()
                                p2.consume()
                                continue
                            }

                            // 2. Kliknięcie na obrazek lub tło (detekcja zaznaczenia)
                            if (primaryPointer.changedToDown()) {
                                val canvasPos = screenToCanvas(primaryPointer.position)
                                val clickedImage = canvasImages.findLast { img ->
                                    canvasPos.x >= img.canvasX && canvasPos.x <= (img.canvasX + img.widthPx) &&
                                            canvasPos.y >= img.canvasY && canvasPos.y <= (img.canvasY + img.heightPx)
                                }
                                if (clickedImage != null) {
                                    onSelectImage(clickedImage.id)
                                } else if (activeTool == ActiveTool.HAND) {
                                    onSelectImage(null)
                                }
                            }

                            // 3. Przesuwanie zaznaczonego obrazka 1 palcem/rysikiem
                            if (selectedImageId != null && (activeTool == ActiveTool.HAND || activeTool == ActiveTool.LASSO)) {
                                if (primaryPointer.pressed) {
                                    val deltaScreen = primaryPointer.position - primaryPointer.previousPosition
                                    val deltaCanvas = deltaScreen / zoomScale
                                    if (deltaCanvas.getDistance() > 0.1f) {
                                        onUpdateImageTransform(selectedImageId, deltaCanvas.x, deltaCanvas.y, 1f, 0f)
                                    }
                                    primaryPointer.consume()
                                    continue
                                }
                            }

                            // 4. Palm Rejection – jeden palec w trybie "Tylko Rysik" -> przesuń
                            if (stylusOnlyMode && isTouch) {
                                if (primaryPointer.pressed) {
                                    val delta = primaryPointer.position - primaryPointer.previousPosition
                                    panOffset += delta
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 5. Narzędzie Ręka (Pan)
                            if (activeTool == ActiveTool.HAND) {
                                if (primaryPointer.pressed) {
                                    val delta = primaryPointer.position - primaryPointer.previousPosition
                                    panOffset += delta
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 6. Wstawianie symbolu / wzoru w miejsce kliknięcia (Tap-to-Place)
                            if (activeTool == ActiveTool.FORMULA) {
                                val canvasPos = screenToCanvas(primaryPointer.position)
                                if (primaryPointer.changedToDown()) {
                                    onInsertFormulaAt(canvasPos)
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 7. Gumka
                            if (activeTool == ActiveTool.ERASER) {
                                if (primaryPointer.pressed) {
                                    val canvasPos = screenToCanvas(primaryPointer.position)
                                    if (eraserType == EraserType.OBJECT) {
                                        onObjectErase(canvasPos)
                                    } else {
                                        onPixelErase(canvasPos, 20f / zoomScale)
                                    }
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 8. Interaktywne tworzenie figur lub szablonów MS Paint style (Drag-to-Create)
                            if (activeTool == ActiveTool.SHAPE || activeTool == ActiveTool.TEMPLATE) {
                                val canvasPos = screenToCanvas(primaryPointer.position)
                                if (primaryPointer.changedToDown()) {
                                    shapeStartPos = canvasPos
                                    if (activeTool == ActiveTool.SHAPE) {
                                        onDragShapeUpdate(canvasPos, canvasPos)
                                    } else {
                                        onDragTemplateUpdate(canvasPos, canvasPos)
                                    }
                                    primaryPointer.consume()
                                } else if (primaryPointer.pressed && shapeStartPos != null) {
                                    if (activeTool == ActiveTool.SHAPE) {
                                        onDragShapeUpdate(shapeStartPos!!, canvasPos)
                                    } else {
                                        onDragTemplateUpdate(shapeStartPos!!, canvasPos)
                                    }
                                    primaryPointer.consume()
                                } else if (primaryPointer.changedToUp()) {
                                    onStrokeEnd()
                                    shapeStartPos = null
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 9. Przesuwanie zaznaczonego obiektu Lasso
                            if (activeTool == ActiveTool.LASSO && selectedStrokeIds.isNotEmpty() && lassoPoints.isEmpty()) {
                                if (primaryPointer.pressed) {
                                    val deltaScreen = primaryPointer.position - primaryPointer.previousPosition
                                    val deltaCanvas = deltaScreen / zoomScale
                                    if (deltaCanvas.getDistance() > 0.1f) {
                                        onMoveSelectedStrokes(deltaCanvas.x, deltaCanvas.y)
                                    }
                                    primaryPointer.consume()
                                    continue
                                }
                            }

                            // 10. Lasso – rysowanie pętli zaznaczenia
                            if (activeTool == ActiveTool.LASSO) {
                                val canvasPos = screenToCanvas(primaryPointer.position)
                                if (primaryPointer.changedToDown()) {
                                    onLassoMove(canvasPos)
                                    primaryPointer.consume()
                                } else if (primaryPointer.pressed) {
                                    onLassoMove(canvasPos)
                                    primaryPointer.consume()
                                } else if (primaryPointer.changedToUp()) {
                                    onLassoEnd()
                                    primaryPointer.consume()
                                }
                                continue
                            }

                            // 11. Rysowanie (Pióro, Neon, Zakreślacz)
                            val canvasPoint = screenToCanvas(primaryPointer.position)
                            val pressure = primaryPointer.pressure.coerceIn(0.2f, 2.0f)
                            val strokePt = StrokePoint(canvasPoint.x, canvasPoint.y, pressure)

                            if (primaryPointer.changedToDown()) {
                                onStrokeStart(strokePt)
                                primaryPointer.consume()
                            } else if (primaryPointer.pressed) {
                                onStrokeMove(strokePt)
                                primaryPointer.consume()
                            } else if (primaryPointer.changedToUp()) {
                                onStrokeEnd()
                                primaryPointer.consume()
                            }
                        }
                    }
                }
        ) {
            // A. Tło kartki (Jasny papier, Vintage lub Ciemny OLED)
            val paperColor = when (paperStyle) {
                com.notatki.app.data.model.PaperStyle.VINTAGE -> Color(0xFFFBF4E8)
                else -> if (isDarkTheme) com.notatki.app.ui.theme.DarkPaperColor else com.notatki.app.ui.theme.NotebookPaperColor
            }
            val patternColor = if (isDarkTheme) com.notatki.app.ui.theme.DarkGridLineColor else com.notatki.app.ui.theme.GridLineColor
            drawRect(color = paperColor)

            // Użyj akcelerowanej sprzętowo macierzy transformacji GPU (Przesunięcie i Skalowanie)
            withTransform({
                translate(panOffset.x, panOffset.y)
                scale(zoomScale, zoomScale, pivot = Offset.Zero)
            }) {
                // B. Rysowanie wzoru kartki (kratka, linie, kropki, milimetrowy, heksagony)
                drawCanvasPattern(paperStyle, patternColor, size, panOffset, zoomScale)

                // C. Wstawione obrazy (zdjęcia / slajdy PDF)
                for (img in canvasImages) {
                    val isSelected = img.id == selectedImageId

                    drawIntoCanvas { canvas ->
                        canvas.save()
                        canvas.rotate(img.rotationDegrees, img.canvasX + img.widthPx / 2f, img.canvasY + img.heightPx / 2f)

                        val paint = Paint().apply { isAntiAlias = true }
                        canvas.drawImageRect(
                            image = img.bitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(img.canvasX.toInt(), img.canvasY.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(img.widthPx.toInt(), img.heightPx.toInt()),
                            paint = paint
                        )

                        if (isSelected) {
                            val borderPaint = Paint().apply {
                                color = Color(0xFF2979FF)
                                style = PaintingStyle.Stroke
                                strokeWidth = 3f / zoomScale
                            }
                            canvas.drawRect(
                                androidx.compose.ui.geometry.Rect(
                                    img.canvasX - 4f,
                                    img.canvasY - 4f,
                                    img.canvasX + img.widthPx + 4f,
                                    img.canvasY + img.heightPx + 4f
                                ),
                                borderPaint
                            )
                        }

                        canvas.restore()
                    }
                }

                // D. Akcelerowane sprzętowo rysowanie istniejących kresek notatek z pamięci podręcznej (Cache)
                for (stroke in strokes) {
                    val path = strokePathsMap[stroke.id] ?: continue
                    val isSelected = stroke.id in selectedStrokeIds

                    if (isSelected) {
                        drawPath(
                            path = path,
                            color = Color(0x552979FF),
                            style = Stroke(
                                width = stroke.strokeWidth + 10f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter
                            )
                        )
                    }

                    val strokeColor = Color(stroke.colorArgb)
                    val alpha = if (stroke.isHighlighter) 0.35f else strokeColor.alpha

                    if (stroke.isNeon) {
                        // Neon Glow Aura
                        drawPath(
                            path = path,
                            color = strokeColor.copy(alpha = 0.4f),
                            style = Stroke(
                                width = stroke.strokeWidth + 14f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        // Neon Core
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else {
                        drawPath(
                            path = path,
                            color = strokeColor.copy(alpha = alpha),
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter
                            )
                        )
                    }
                }

                // E. Rysowanie aktualnie tworzonej kreski pod rysikiem / podgląd figury na żywo
                currentStroke?.let { stroke ->
                    val currentPath = buildStrokePath(stroke)
                    val strokeColor = Color(stroke.colorArgb)
                    val alpha = if (stroke.isHighlighter) 0.35f else strokeColor.alpha

                    if (stroke.isNeon) {
                        drawPath(
                            path = currentPath,
                            color = strokeColor.copy(alpha = 0.4f),
                            style = Stroke(
                                width = stroke.strokeWidth + 14f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawPath(
                            path = currentPath,
                            color = Color.White,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    } else {
                        drawPath(
                            path = currentPath,
                            color = strokeColor.copy(alpha = alpha),
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Miter
                            )
                        )
                    }
                }

                // F. Overlay Lasso (przerywana niebieska pętla)
                if (lassoPoints.size >= 2) {
                    val lassoPath = Path()
                    lassoPath.moveTo(lassoPoints[0].x, lassoPoints[0].y)
                    for (i in 1 until lassoPoints.size) {
                        lassoPath.lineTo(lassoPoints[i].x, lassoPoints[i].y)
                    }
                    drawPath(
                        path = lassoPath,
                        color = Color(0xFF2979FF),
                        style = Stroke(
                            width = 2f / zoomScale,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / zoomScale, 8f / zoomScale), 0f)
                        )
                    )
                }
            }

            // G. Wirtualna Linijka Ekranowa tuż pod górnym paskiem
            if (showRuler) {
                drawScreenRuler(isDarkTheme)
            }
        }

        // H. Mini-Mapa Tablicy w Prawym Dolnym Rogu (Nawigator z przyciskiem Wyśrodkuj)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0A0D18).copy(alpha = 0.90f),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp)
                .size(130.dp, 95.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val miniWidth = size.width
                    val miniHeight = size.height

                    val vpX = (-panOffset.x / zoomScale) * 0.05f + miniWidth / 2f
                    val vpY = (-panOffset.y / zoomScale) * 0.05f + miniHeight / 2f
                    val vpW = (size.width / zoomScale) * 0.05f
                    val vpH = (size.height / zoomScale) * 0.05f

                    drawRect(
                        color = Color.White.copy(alpha = 0.15f),
                        topLeft = Offset.Zero,
                        size = Size(miniWidth, miniHeight)
                    )

                    drawRect(
                        color = NeonCyan,
                        topLeft = Offset(vpX.coerceIn(0f, miniWidth - 10f), vpY.coerceIn(0f, miniHeight - 10f)),
                        size = Size(vpW.coerceIn(10f, miniWidth), vpH.coerceIn(10f, miniHeight)),
                        style = Stroke(width = 1.5f)
                    )
                }

                IconButton(
                    onClick = {
                        panOffset = Offset.Zero
                        zoomScale = 1.0f
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = "Wyśrodkuj widok",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${(zoomScale * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawCanvasPattern(
    style: com.notatki.app.data.model.PaperStyle,
    color: Color,
    screenSize: Size,
    pan: Offset,
    scale: Float
) {
    val gridSize = 35f
    val viewportWidth = screenSize.width / scale
    val viewportHeight = screenSize.height / scale

    val startX = (-pan.x / scale).let { val rem = it % gridSize; if (rem > 0) it - rem else it - gridSize }
    val startY = (-pan.y / scale).let { val rem = it % gridSize; if (rem > 0) it - rem else it - gridSize }
    val endX = startX + viewportWidth + gridSize * 2
    val endY = startY + viewportHeight + gridSize * 2

    when (style) {
        com.notatki.app.data.model.PaperStyle.GRID -> {
            var x = startX
            while (x < endX) {
                drawLine(color = color, start = Offset(x, startY), end = Offset(x, endY), strokeWidth = 1f / scale)
                x += gridSize
            }
            var y = startY
            while (y < endY) {
                drawLine(color = color, start = Offset(startX, y), end = Offset(endX, y), strokeWidth = 1f / scale)
                y += gridSize
            }
        }
        com.notatki.app.data.model.PaperStyle.MILLIMETER -> {
            val mmSize = 10f
            var x = startX
            while (x < endX) {
                val isMajor = (x / mmSize).toInt() % 5 == 0
                drawLine(
                    color = if (isMajor) color.copy(alpha = 0.6f) else color.copy(alpha = 0.22f),
                    start = Offset(x, startY),
                    end = Offset(x, endY),
                    strokeWidth = if (isMajor) 1.2f / scale else 0.6f / scale
                )
                x += mmSize
            }
            var y = startY
            while (y < endY) {
                val isMajor = (y / mmSize).toInt() % 5 == 0
                drawLine(
                    color = if (isMajor) color.copy(alpha = 0.6f) else color.copy(alpha = 0.22f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = if (isMajor) 1.2f / scale else 0.6f / scale
                )
                y += mmSize
            }
        }
        com.notatki.app.data.model.PaperStyle.HEXAGON -> {
            val hexRadius = 25f
            val h = hexRadius * kotlin.math.sqrt(3f)
            var row = 0
            var y = startY
            while (y < endY) {
                var x = startX + if (row % 2 == 1) hexRadius * 1.5f else 0f
                while (x < endX) {
                    drawCircle(color = color.copy(alpha = 0.45f), radius = 2f / scale, center = Offset(x, y))
                    x += hexRadius * 3f
                }
                y += h / 2f
                row++
            }
        }
        com.notatki.app.data.model.PaperStyle.VINTAGE -> {
            var y = startY
            while (y < endY) {
                drawLine(color = Color(0xFFD4B185).copy(alpha = 0.45f), start = Offset(startX, y), end = Offset(endX, y), strokeWidth = 1f / scale)
                y += gridSize
            }
        }
        com.notatki.app.data.model.PaperStyle.LINES -> {
            var y = startY
            while (y < endY) {
                drawLine(color = color, start = Offset(startX, y), end = Offset(endX, y), strokeWidth = 1f / scale)
                y += gridSize
            }
        }
        com.notatki.app.data.model.PaperStyle.DOTS -> {
            var x = startX
            while (x < endX) {
                var y = startY
                while (y < endY) {
                    drawCircle(color = color, radius = 1.5f / scale, center = Offset(x, y))
                    y += gridSize
                }
                x += gridSize
            }
        }
        com.notatki.app.data.model.PaperStyle.BLANK -> {}
    }
}

private fun DrawScope.drawScreenRuler(isDarkTheme: Boolean) {
    val rulerHeight = 60f
    val rulerWidth = size.width
    val bgColor = if (isDarkTheme) Color(0xDD121626) else Color(0xDDFAFAFA)
    val lineColor = if (isDarkTheme) NeonCyan else Color(0xFF1E293B)

    drawRect(
        color = bgColor,
        topLeft = Offset(0f, 0f),
        size = Size(rulerWidth, rulerHeight)
    )

    drawLine(
        color = lineColor,
        start = Offset(0f, rulerHeight),
        end = Offset(rulerWidth, rulerHeight),
        strokeWidth = 2f
    )

    var x = 20f
    var cmCount = 0
    while (x < rulerWidth) {
        val isMajor = cmCount % 10 == 0
        val tickLen = if (isMajor) 25f else 12f

        drawLine(
            color = lineColor,
            start = Offset(x, rulerHeight - tickLen),
            end = Offset(x, rulerHeight),
            strokeWidth = if (isMajor) 2f else 1f
        )

        x += 15f
        cmCount++
    }
}

private fun buildStrokePath(stroke: DrawingStroke): Path {
    val path = Path()
    val points = stroke.points
    if (points.isEmpty()) return path

    path.moveTo(points[0].x, points[0].y)
    val useStraightLines = points.size <= 10 || isPolyline(points)

    if (useStraightLines) {
        for (i in 1 until points.size) {
            val pt = points[i]
            if (pt.pressure == 0f) {
                path.moveTo(pt.x, pt.y)
            } else {
                path.lineTo(pt.x, pt.y)
            }
        }
    } else {
        for (i in 1 until points.size) {
            val curr = points[i]
            if (curr.pressure == 0f) {
                path.moveTo(curr.x, curr.y)
                continue
            }
            val prev = points[i - 1]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadraticBezierTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(points.last().x, points.last().y)
    }
    return path
}

private fun isPolyline(points: List<StrokePoint>): Boolean {
    if (points.size in 2..55) {
        var totalDist = 0f
        for (i in 1 until points.size) {
            totalDist += hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        val avgSegLen = totalDist / (points.size - 1)
        if (avgSegLen > 15f) return true
    }
    return false
}
