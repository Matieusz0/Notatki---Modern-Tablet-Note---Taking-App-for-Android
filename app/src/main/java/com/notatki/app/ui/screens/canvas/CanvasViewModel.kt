package com.notatki.app.ui.screens.canvas

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notatki.app.NotatkiApp
import com.notatki.app.data.model.DrawingStroke
import com.notatki.app.data.model.EraserType
import com.notatki.app.data.model.Lesson
import com.notatki.app.data.model.NoteCanvasData
import com.notatki.app.data.model.NoteImageItem
import com.notatki.app.data.model.StrokePoint
import com.notatki.app.ui.screens.canvas.components.ActiveTool
import com.notatki.app.ui.screens.canvas.components.InsertedTemplateType
import com.notatki.app.ui.screens.canvas.components.SelectedShapeType
import com.notatki.app.util.GeometryUtils
import com.notatki.app.util.PdfExporter
import com.notatki.app.util.PdfSlideRenderer
import com.notatki.app.util.ShapeRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

// Wstawione zdjęcie / slajd PDF na tablicy
data class CanvasImage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val filePath: String = "",
    val bitmap: ImageBitmap,
    val canvasX: Float,   // pozycja środka w przestrzeni płótna
    val canvasY: Float,
    val widthPx: Float,   // rozmiar w jednostkach płótna (nie ekranu)
    val heightPx: Float,
    val rotationDegrees: Float = 0f
)

class CanvasViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotatkiApp
    private val storageRepo = app.storageRepository
    private val lessonDao = app.database.lessonDao()

    private var currentLessonId: Long = 0
    private var lesson: Lesson? = null

    private val _lessonTitle = MutableStateFlow("Notatka")
    val lessonTitle: StateFlow<String> = _lessonTitle.asStateFlow()

    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    private val _currentDrawingStroke = MutableStateFlow<DrawingStroke?>(null)
    val currentDrawingStroke: StateFlow<DrawingStroke?> = _currentDrawingStroke.asStateFlow()

    // Aktywne punkty tworzonej kreski (Zero-allocation point buffer)
    private val activeStrokePoints = ArrayList<StrokePoint>()

    // Undo / Redo
    private val undoStack = mutableListOf<List<DrawingStroke>>()
    private val redoStack = mutableListOf<List<DrawingStroke>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Narzędzia
    var activeTool = mutableStateOf(ActiveTool.PEN)
    var selectedShapeType = mutableStateOf(SelectedShapeType.RECTANGLE)
    var selectedTemplateType = mutableStateOf(InsertedTemplateType.COORDINATE_XY)
    var pendingFormulaSymbol = mutableStateOf<String?>("∫")
    var tableColumns = mutableIntStateOf(3)
    var tableRows = mutableIntStateOf(3)

    var currentColor = mutableStateOf(Color(0xFF000000))
    var currentStrokeWidth = mutableFloatStateOf(4.0f)
    var eraserType = mutableStateOf(EraserType.OBJECT)
    var stylusOnlyMode = mutableStateOf(false)

    val paperStyle = MutableStateFlow(com.notatki.app.data.model.PaperStyle.GRID)
    val settings = app.settingsRepository.settings
    private var autoSaveJob: Job? = null

    // ── Lasso ──────────────────────────────────────────────────────────────────
    private val _lassoPoints = MutableStateFlow<List<Offset>>(emptyList())
    val lassoPoints: StateFlow<List<Offset>> = _lassoPoints.asStateFlow()

    private val _selectedStrokeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedStrokeIds: StateFlow<Set<String>> = _selectedStrokeIds.asStateFlow()

    fun onLassoMove(point: Offset) {
        _lassoPoints.value = _lassoPoints.value + point
    }

    fun onLassoEnd() {
        val polygon = _lassoPoints.value
        if (polygon.size < 3) {
            _lassoPoints.value = emptyList()
            return
        }
        val selected = _strokes.value.filter { stroke ->
            stroke.points.any { pt ->
                GeometryUtils.isPointInPolygon(Offset(pt.x, pt.y), polygon)
            }
        }.map { it.id }.toSet()
        _selectedStrokeIds.value = selected
        _lassoPoints.value = emptyList()
    }

    fun clearLassoSelection() {
        _selectedStrokeIds.value = emptySet()
        _lassoPoints.value = emptyList()
    }

    fun deleteSelectedStrokes() {
        val ids = _selectedStrokeIds.value
        if (ids.isEmpty()) return
        pushToUndoStack()
        _strokes.value = _strokes.value.filter { it.id !in ids }
        _selectedStrokeIds.value = emptySet()
        scheduleAutoSave()
    }

    fun recolorSelectedStrokes(color: Color) {
        val ids = _selectedStrokeIds.value
        if (ids.isEmpty()) return
        pushToUndoStack()
        _strokes.value = _strokes.value.map { stroke ->
            if (stroke.id in ids) stroke.copy(colorArgb = color.toArgb().toLong()) else stroke
        }
        scheduleAutoSave()
    }

    fun moveSelectedStrokes(deltaX: Float, deltaY: Float) {
        val ids = _selectedStrokeIds.value
        if (ids.isEmpty()) return
        _strokes.value = _strokes.value.map { stroke ->
            if (stroke.id in ids) {
                stroke.copy(points = stroke.points.map { pt ->
                    pt.copy(x = pt.x + deltaX, y = pt.y + deltaY)
                })
            } else stroke
        }
        scheduleAutoSave()
    }

    // ── Symbol/Formula Insertion (High-Resolution Academic Typography Renderer)
    fun selectFormulaSymbol(symbolText: String) {
        pendingFormulaSymbol.value = symbolText
        activeTool.value = ActiveTool.FORMULA
    }

    fun insertFormulaAt(symbol: String, x: Float, y: Float) {
        val strokeColor = currentColor.value.toArgb()

        viewModelScope.launch {
            try {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 72f
                    color = strokeColor
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
                }

                val bounds = android.graphics.Rect()
                paint.getTextBounds(symbol, 0, symbol.length, bounds)

                val padding = 24
                val width = bounds.width() + padding * 2
                val height = bounds.height() + padding * 2

                if (width <= 0 || height <= 0) return@launch

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)

                val baselineY = padding - bounds.top
                canvas.drawText(symbol, padding.toFloat(), baselineY.toFloat(), paint)

                val file = File(app.cacheDir, "formula_${System.currentTimeMillis()}.png")
                withContext(Dispatchers.IO) {
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                insertImageFromFile(file, x - width / 2f, y - height / 2f)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── AutoShape (Draw & Hold) ────────────────────────────────────────────────
    var autoShapeEnabled = mutableStateOf(false)

    // ── Interaktywne tworzenie figur (Drag-to-Create) ─────────────────────────
    fun updateDragShape(start: Offset, current: Offset) {
        val isHighlighter = activeTool.value == ActiveTool.HIGHLIGHTER
        val isNeon = activeTool.value == ActiveTool.NEON_PEN
        val strokeColor = currentColor.value.toArgb().toLong()
        val width = currentStrokeWidth.floatValue

        val points = generateShapePoints(selectedShapeType.value, start, current)
        activeStrokePoints.clear()
        activeStrokePoints.addAll(points)

        val stroke = DrawingStroke(
            points = points,
            colorArgb = strokeColor,
            strokeWidth = width,
            isHighlighter = isHighlighter,
            isNeon = isNeon
        )
        _currentDrawingStroke.value = stroke
    }

    private fun generateShapePoints(type: SelectedShapeType, start: Offset, current: Offset): List<StrokePoint> {
        return when (type) {
            SelectedShapeType.LINE -> {
                listOf(
                    StrokePoint(start.x, start.y),
                    StrokePoint(current.x, current.y)
                )
            }
            SelectedShapeType.ARROW -> {
                val dx = current.x - start.x
                val dy = current.y - start.y
                val angle = kotlin.math.atan2(dy, dx)
                val headLen = 22f
                val arrowAngle = Math.PI / 6.0

                val x1 = current.x - (headLen * kotlin.math.cos(angle - arrowAngle)).toFloat()
                val y1 = current.y - (headLen * kotlin.math.sin(angle - arrowAngle)).toFloat()
                val x2 = current.x - (headLen * kotlin.math.cos(angle + arrowAngle)).toFloat()
                val y2 = current.y - (headLen * kotlin.math.sin(angle + arrowAngle)).toFloat()

                listOf(
                    StrokePoint(start.x, start.y, 1f),
                    StrokePoint(current.x, current.y, 1f),
                    StrokePoint(x1, y1, 0f),
                    StrokePoint(current.x, current.y, 1f),
                    StrokePoint(x2, y2, 1f)
                )
            }
            SelectedShapeType.RECTANGLE -> {
                val left = kotlin.math.min(start.x, current.x)
                val top = kotlin.math.min(start.y, current.y)
                val right = kotlin.math.max(start.x, current.x)
                val bottom = kotlin.math.max(start.y, current.y)

                listOf(
                    StrokePoint(left, top),
                    StrokePoint(right, top),
                    StrokePoint(right, bottom),
                    StrokePoint(left, bottom),
                    StrokePoint(left, top)
                )
            }
            SelectedShapeType.CIRCLE -> {
                val left = kotlin.math.min(start.x, current.x)
                val top = kotlin.math.min(start.y, current.y)
                val right = kotlin.math.max(start.x, current.x)
                val bottom = kotlin.math.max(start.y, current.y)

                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val rx = (right - left) / 2f
                val ry = (bottom - top) / 2f

                val result = mutableListOf<StrokePoint>()
                val steps = 48
                for (i in 0..steps) {
                    val a = (i.toFloat() / steps) * (2 * Math.PI)
                    val x = centerX + (rx * kotlin.math.cos(a)).toFloat()
                    val y = centerY + (ry * kotlin.math.sin(a)).toFloat()
                    result.add(StrokePoint(x, y))
                }
                result
            }
            SelectedShapeType.TRIANGLE -> {
                val left = kotlin.math.min(start.x, current.x)
                val top = kotlin.math.min(start.y, current.y)
                val right = kotlin.math.max(start.x, current.x)
                val bottom = kotlin.math.max(start.y, current.y)

                val topCenter = (left + right) / 2f

                listOf(
                    StrokePoint(topCenter, top),
                    StrokePoint(right, bottom),
                    StrokePoint(left, bottom),
                    StrokePoint(topCenter, top)
                )
            }
        }
    }

    // ── Interaktywne tworzenie szablonów (Drag-to-Create Templates) ────────────
    fun updateDragTemplate(start: Offset, current: Offset) {
        val isHighlighter = activeTool.value == ActiveTool.HIGHLIGHTER
        val isNeon = activeTool.value == ActiveTool.NEON_PEN
        val strokeColor = currentColor.value.toArgb().toLong()
        val width = currentStrokeWidth.floatValue

        val points = generateTemplatePoints(
            type = selectedTemplateType.value,
            start = start,
            current = current,
            cols = tableColumns.intValue,
            rows = tableRows.intValue
        )
        activeStrokePoints.clear()
        activeStrokePoints.addAll(points)

        val stroke = DrawingStroke(
            points = points,
            colorArgb = strokeColor,
            strokeWidth = width,
            isHighlighter = isHighlighter,
            isNeon = isNeon
        )
        _currentDrawingStroke.value = stroke
    }

    private fun generateTemplatePoints(
        type: InsertedTemplateType,
        start: Offset,
        current: Offset,
        cols: Int,
        rows: Int
    ): List<StrokePoint> {
        val left = kotlin.math.min(start.x, current.x)
        val top = kotlin.math.min(start.y, current.y)
        val right = kotlin.math.max(start.x, current.x)
        val bottom = kotlin.math.max(start.y, current.y)

        return when (type) {
            InsertedTemplateType.COORDINATE_XY -> {
                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val arrowLen = 16f

                listOf(
                    // Oś Y
                    StrokePoint(centerX, bottom, 1f),
                    StrokePoint(centerX, top, 1f),
                    // Strzałka Y
                    StrokePoint(centerX - arrowLen / 2, top + arrowLen, 0f),
                    StrokePoint(centerX, top, 1f),
                    StrokePoint(centerX + arrowLen / 2, top + arrowLen, 1f),
                    // Oś X
                    StrokePoint(left, centerY, 0f),
                    StrokePoint(right, centerY, 1f),
                    // Strzałka X
                    StrokePoint(right - arrowLen, centerY - arrowLen / 2, 0f),
                    StrokePoint(right, centerY, 1f),
                    StrokePoint(right - arrowLen, centerY + arrowLen / 2, 1f)
                )
            }
            InsertedTemplateType.COORDINATE_XYZ -> {
                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val arrowLen = 14f

                listOf(
                    // Oś Y (w górę)
                    StrokePoint(centerX, centerY, 1f),
                    StrokePoint(centerX, top, 1f),
                    StrokePoint(centerX - arrowLen / 2, top + arrowLen, 0f),
                    StrokePoint(centerX, top, 1f),
                    StrokePoint(centerX + arrowLen / 2, top + arrowLen, 1f),
                    // Oś X (w prawo)
                    StrokePoint(centerX, centerY, 0f),
                    StrokePoint(right, centerY, 1f),
                    StrokePoint(right - arrowLen, centerY - arrowLen / 2, 0f),
                    StrokePoint(right, centerY, 1f),
                    StrokePoint(right - arrowLen, centerY + arrowLen / 2, 1f),
                    // Oś Z (skos w lewo-dół)
                    StrokePoint(centerX, centerY, 0f),
                    StrokePoint(left, bottom, 1f)
                )
            }
            InsertedTemplateType.TABLE_GRID -> {
                val points = mutableListOf<StrokePoint>()

                // Ramka zewnętrzna (ciągła)
                points.add(StrokePoint(left, top, 1f))
                points.add(StrokePoint(right, top, 1f))
                points.add(StrokePoint(right, bottom, 1f))
                points.add(StrokePoint(left, bottom, 1f))
                points.add(StrokePoint(left, top, 1f))

                // Linie pionowe dla kolumn
                val colWidth = (right - left) / cols.coerceAtLeast(1)
                for (c in 1 until cols) {
                    val x = left + c * colWidth
                    points.add(StrokePoint(x, top, 0f))
                    points.add(StrokePoint(x, bottom, 1f))
                }

                // Linie poziome dla wierszy
                val rowHeight = (bottom - top) / rows.coerceAtLeast(1)
                for (r in 1 until rows) {
                    val y = top + r * rowHeight
                    points.add(StrokePoint(left, y, 0f))
                    points.add(StrokePoint(right, y, 1f))
                }

                points
            }
            InsertedTemplateType.POLAR_GRID -> {
                val centerX = (left + right) / 2f
                val centerY = (top + bottom) / 2f
                val maxRx = (right - left) / 2f
                val maxRy = (bottom - top) / 2f

                val points = mutableListOf<StrokePoint>()

                for (ring in 1..3) {
                    val rx = maxRx * (ring / 3f)
                    val ry = maxRy * (ring / 3f)
                    val steps = 36
                    for (i in 0..steps) {
                        val a = (i.toFloat() / steps) * (2 * Math.PI)
                        val x = centerX + (rx * kotlin.math.cos(a)).toFloat()
                        val y = centerY + (ry * kotlin.math.sin(a)).toFloat()
                        points.add(StrokePoint(x, y, if (i == 0) 0f else 1f))
                    }
                }

                points.add(StrokePoint(centerX, top, 0f))
                points.add(StrokePoint(centerX, bottom, 1f))
                points.add(StrokePoint(left, centerY, 0f))
                points.add(StrokePoint(right, centerY, 1f))

                points
            }
        }
    }

    // ── Wstawione obrazy / slajdy PDF ─────────────────────────────────────────
    private val _canvasImages = MutableStateFlow<List<CanvasImage>>(emptyList())
    val canvasImages: StateFlow<List<CanvasImage>> = _canvasImages.asStateFlow()

    private val _selectedImageId = MutableStateFlow<String?>(null)
    val selectedImageId: StateFlow<String?> = _selectedImageId.asStateFlow()

    fun selectCanvasImage(id: String?) {
        _selectedImageId.value = id
    }

    fun updateImageTransform(
        id: String,
        deltaX: Float = 0f,
        deltaY: Float = 0f,
        scaleFactor: Float = 1f,
        rotationDelta: Float = 0f
    ) {
        _canvasImages.value = _canvasImages.value.map { img ->
            if (img.id == id) {
                val newWidth = (img.widthPx * scaleFactor).coerceIn(100f, 4000f)
                val newHeight = (img.heightPx * scaleFactor).coerceIn(100f, 4000f)
                val newRotation = (img.rotationDegrees + rotationDelta) % 360f
                img.copy(
                    canvasX = img.canvasX + deltaX,
                    canvasY = img.canvasY + deltaY,
                    widthPx = newWidth,
                    heightPx = newHeight,
                    rotationDegrees = newRotation
                )
            } else img
        }
        scheduleAutoSave()
    }

    fun deleteSelectedImage() {
        val id = _selectedImageId.value ?: return
        deleteCanvasImage(id)
        _selectedImageId.value = null
    }

    fun insertImageFromUri(uri: Uri, centerX: Float = 0f, centerY: Float = 0f) {
        viewModelScope.launch {
            try {
                val file = PdfSlideRenderer.saveAndDownsampleImage(app, uri)
                if (file != null) {
                    insertImageFromFile(file, centerX, centerY)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertImageFromFile(file: File, centerX: Float = 0f, centerY: Float = 0f) {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } ?: return@launch
                insertBitmap(file.absolutePath, bitmap, centerX, centerY)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun insertPdfPageFile(pdfFile: File, pageIndex: Int, centerX: Float = 0f, centerY: Float = 0f) {
        viewModelScope.launch {
            try {
                val file = PdfSlideRenderer.renderPageToImageFile(app, pdfFile, pageIndex)
                if (file != null) {
                    insertImageFromFile(file, centerX, centerY)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun insertBitmap(filePath: String, bitmap: Bitmap, centerX: Float, centerY: Float) {
        val maxW = 800f  // max szerokość w jednostkach płótna
        val scale = if (bitmap.width > maxW) maxW / bitmap.width else 1f
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        val img = CanvasImage(
            filePath = filePath,
            bitmap = bitmap.asImageBitmap(),
            canvasX = centerX,
            canvasY = centerY,
            widthPx = w,
            heightPx = h
        )
        _canvasImages.value = _canvasImages.value + img
        _selectedImageId.value = img.id
        scheduleAutoSave()
    }

    fun deleteCanvasImage(id: String) {
        _canvasImages.value = _canvasImages.value.filter { it.id != id }
        if (_selectedImageId.value == id) {
            _selectedImageId.value = null
        }
        scheduleAutoSave()
    }

    // ──────────────────────────────────────────────────────────────────────────

    fun loadLesson(lessonId: Long) {
        currentLessonId = lessonId
        stylusOnlyMode.value = settings.value.defaultStylusOnly
        viewModelScope.launch {
            lesson = lessonDao.getLessonById(lessonId)
            _lessonTitle.value = lesson?.title ?: "Notatka"
            val styleStr = lesson?.paperStyle ?: "GRID"
            paperStyle.value = try {
                com.notatki.app.data.model.PaperStyle.valueOf(styleStr)
            } catch (e: Exception) {
                com.notatki.app.data.model.PaperStyle.GRID
            }

            val data = storageRepo.loadNote(lessonId)
            _strokes.value = data.strokes

            val loadedImages = mutableListOf<CanvasImage>()
            withContext(Dispatchers.IO) {
                for (item in data.images) {
                    if (File(item.filePath).exists()) {
                        val bmp = BitmapFactory.decodeFile(item.filePath)
                        if (bmp != null) {
                            loadedImages.add(
                                CanvasImage(
                                    id = item.id,
                                    filePath = item.filePath,
                                    bitmap = bmp.asImageBitmap(),
                                    canvasX = item.x,
                                    canvasY = item.y,
                                    widthPx = item.width,
                                    heightPx = item.height,
                                    rotationDegrees = item.rotationDegrees
                                )
                            )
                        }
                    }
                }
            }
            _canvasImages.value = loadedImages

            undoStack.clear()
            redoStack.clear()
            updateUndoRedoStates()
        }
    }

    fun startStroke(point: StrokePoint) {
        activeStrokePoints.clear()
        activeStrokePoints.add(point)
        val isHighlighter = activeTool.value == ActiveTool.HIGHLIGHTER
        val isNeon = activeTool.value == ActiveTool.NEON_PEN
        val stroke = DrawingStroke(
            points = listOf(point),
            colorArgb = currentColor.value.toArgb().toLong(),
            strokeWidth = currentStrokeWidth.floatValue,
            isHighlighter = isHighlighter,
            isNeon = isNeon
        )
        _currentDrawingStroke.value = stroke
    }

    fun addPointToStroke(point: StrokePoint) {
        val current = _currentDrawingStroke.value ?: return
        val last = activeStrokePoints.lastOrNull()
        if (last != null) {
            val dx = point.x - last.x
            val dy = point.y - last.y
            if (dx * dx + dy * dy < 1.0f) return // Pomiń mikro-drgania < 1px dla płynności
        }
        activeStrokePoints.add(point)
        _currentDrawingStroke.value = current.copy(points = ArrayList(activeStrokePoints))
    }

    fun finishStroke() {
        val current = _currentDrawingStroke.value ?: return
        if (activeStrokePoints.size >= 2) {
            val pts = ArrayList(activeStrokePoints)
            val strokeToProcess = current.copy(points = pts)

            // Autokorekta kształtów (Draw & Hold dla Pióra)
            val finalStroke = if (autoShapeEnabled.value && activeTool.value != ActiveTool.SHAPE && activeTool.value != ActiveTool.TEMPLATE && activeTool.value != ActiveTool.FORMULA) {
                val shape = ShapeRecognizer.detectShape(pts)
                if (shape != null) {
                    val avgPressure = pts.map { it.pressure }.average().toFloat()
                    val shapePts = ShapeRecognizer.shapeToStrokePoints(shape, avgPressure)
                    strokeToProcess.copy(points = shapePts)
                } else strokeToProcess
            } else strokeToProcess

            pushToUndoStack()
            _strokes.value = _strokes.value + finalStroke
            scheduleAutoSave()
        }
        activeStrokePoints.clear()
        _currentDrawingStroke.value = null
    }

    fun eraseObjectAt(point: Offset) {
        val threshold = 25f
        val currentList = _strokes.value
        val toRemove = currentList.find { stroke ->
            stroke.points.any { pt ->
                hypot(pt.x - point.x, pt.y - point.y) < threshold
            }
        }

        if (toRemove != null) {
            pushToUndoStack()
            _strokes.value = currentList - toRemove
            scheduleAutoSave()
        }
    }

    fun erasePixelAt(point: Offset, radius: Float) {
        val currentList = _strokes.value
        var modified = false

        val updated = currentList.mapNotNull { stroke ->
            val remainingPoints = stroke.points.filter { pt ->
                hypot(pt.x - point.x, pt.y - point.y) >= radius
            }
            if (remainingPoints.size != stroke.points.size) {
                modified = true
            }
            if (remainingPoints.size >= 2) {
                stroke.copy(points = remainingPoints)
            } else null
        }

        if (modified) {
            pushToUndoStack()
            _strokes.value = updated
            scheduleAutoSave()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_strokes.value)
            _strokes.value = undoStack.removeAt(undoStack.lastIndex)
            updateUndoRedoStates()
            scheduleAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_strokes.value)
            _strokes.value = redoStack.removeAt(redoStack.lastIndex)
            updateUndoRedoStates()
            scheduleAutoSave()
        }
    }

    private fun pushToUndoStack() {
        undoStack.add(_strokes.value)
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            storageRepo.saveNote(
                NoteCanvasData(
                    lessonId = currentLessonId,
                    strokes = _strokes.value,
                    images = _canvasImages.value.map { img ->
                        NoteImageItem(
                            id = img.id,
                            filePath = img.filePath,
                            x = img.canvasX,
                            y = img.canvasY,
                            width = img.widthPx,
                            height = img.heightPx,
                            rotationDegrees = img.rotationDegrees
                        )
                    }
                )
            )
        }
    }

    fun exportToPdf(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            val result = PdfExporter.exportToPdf(
                context = app,
                lessonTitle = _lessonTitle.value,
                noteData = NoteCanvasData(
                    lessonId = currentLessonId,
                    strokes = _strokes.value,
                    images = _canvasImages.value.map { img ->
                        NoteImageItem(
                            id = img.id,
                            filePath = img.filePath,
                            x = img.canvasX,
                            y = img.canvasY,
                            width = img.widthPx,
                            height = img.heightPx,
                            rotationDegrees = img.rotationDegrees
                        )
                    }
                )
            )
            onResult(result)
        }
    }

    fun exportToNotatkiProject(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            val result = storageRepo.exportProjectBackup(
                context = app,
                lessonId = currentLessonId,
                title = _lessonTitle.value,
                noteData = NoteCanvasData(
                    lessonId = currentLessonId,
                    strokes = _strokes.value,
                    images = _canvasImages.value.map { img ->
                        NoteImageItem(
                            id = img.id,
                            filePath = img.filePath,
                            x = img.canvasX,
                            y = img.canvasY,
                            width = img.widthPx,
                            height = img.heightPx,
                            rotationDegrees = img.rotationDegrees
                        )
                    }
                )
            )
            onResult(result)
        }
    }
}
