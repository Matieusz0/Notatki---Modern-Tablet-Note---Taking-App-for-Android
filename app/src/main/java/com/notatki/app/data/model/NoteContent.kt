package com.notatki.app.data.model

import java.util.UUID

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<StrokePoint> = emptyList(),
    val colorArgb: Long = 0xFF000000,
    val strokeWidth: Float = 4.0f,
    val isHighlighter: Boolean = false,
    val isNeon: Boolean = false
)

enum class EraserType {
    OBJECT, // Kasuje cale pociagniecie po dotknieciu
    PIXEL   // Sciera punkty
}

data class NoteImageItem(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotationDegrees: Float = 0f
)

data class NoteCanvasData(
    val lessonId: Long = 0,
    val strokes: List<DrawingStroke> = emptyList(),
    val images: List<NoteImageItem> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
