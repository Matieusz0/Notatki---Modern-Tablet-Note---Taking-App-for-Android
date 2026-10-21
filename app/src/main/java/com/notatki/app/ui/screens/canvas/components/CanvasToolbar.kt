package com.notatki.app.ui.screens.canvas.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notatki.app.data.model.EraserType
import com.notatki.app.ui.theme.NeonCyan
import com.notatki.app.ui.theme.NeonGreen
import com.notatki.app.ui.theme.NeonPink
import com.notatki.app.ui.theme.NeonPurple
import com.notatki.app.ui.theme.NeonYellow

enum class ActiveTool {
    PEN,
    NEON_PEN,
    HIGHLIGHTER,
    SHAPE,
    TEMPLATE,
    FORMULA,
    ERASER,
    LASSO,
    HAND // Tylko przesuwanie/powiększanie
}

enum class SelectedShapeType {
    LINE,
    ARROW,
    RECTANGLE,
    CIRCLE,
    TRIANGLE
}

enum class InsertedTemplateType {
    COORDINATE_XY,
    COORDINATE_XYZ,
    TABLE_GRID,
    POLAR_GRID
}

val NOTE_COLORS_LIGHT = listOf(
    Color(0xFF0F172A), // Czarny Obsidian
    Color(0xFF1E40AF), // Granatowy
    Color(0xFFDC2626), // Czerwony
    Color(0xFF16A34A), // Zielony
    Color(0xFF7C3AED), // Fioletowy
    Color(0xFFEA580C)  // Pomarańczowy
)

val NOTE_COLORS_DARK = listOf(
    Color(0xFFFFFFFF), // Czysta Biel
    NeonCyan,          // Neon Cyjan (#00F0FF)
    NeonGreen,         // Neon Lime (#00FF88)
    NeonPurple,        // Neon Violet (#A855F7)
    NeonPink,          // Neon Pink (#FF007A)
    NeonYellow         // Neon Gold (#FFD600)
)

val HIGHLIGHTER_COLORS = listOf(
    Color(0xFFFEF08A), // Żółty pastel
    Color(0xFFBBF7D0), // Zielony pastel
    Color(0xFFFBCFE8), // Różowy pastel
    Color(0xFFBAE6FD)  // Błękitny pastel
)

@Composable
fun CanvasTopBar(
    lessonTitle: String,
    stylusOnlyMode: Boolean,
    onStylusOnlyModeToggled: () -> Unit,
    autoShapeEnabled: Boolean,
    onAutoShapeToggled: () -> Unit,
    showRuler: Boolean,
    onRulerToggled: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    canRedo: Boolean,
    onRedo: () -> Unit,
    onInsertImageClick: () -> Unit,
    onInsertPdfClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onExportPdfClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .statusBarsPadding(),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackClick()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUndo()
                    },
                    enabled = canUndo
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Cofnij")
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRedo()
                    },
                    enabled = canRedo
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Ponów")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Zdjęcie
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInsertImageClick()
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Zdjęcie", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Slajd / PDF
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onInsertPdfClick()
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Slajd / PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilterChip(
                    selected = autoShapeEnabled,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAutoShapeToggled()
                    },
                    label = { Text("Kształty", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.SquareFoot, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                FilterChip(
                    selected = showRuler,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRulerToggled()
                    },
                    label = { Text("Linijka", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                FilterChip(
                    selected = stylusOnlyMode,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStylusOnlyModeToggled()
                    },
                    label = {
                        Text(if (stylusOnlyMode) "Tylko Rysik" else "Rysik + Palec", fontSize = 12.sp)
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCalculatorClick()
                }) {
                    Icon(Icons.Default.Calculate, contentDescription = "Kalkulator")
                }

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onExportPdfClick()
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Eksport PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/**
 * Boczny Pływający Dok Narzędziowy z Kompaktową Paletą oraz Auto-Ukrywaniem podczas pisania
 */
@Composable
fun FloatingSideDock(
    activeTool: ActiveTool,
    onToolSelected: (ActiveTool) -> Unit,
    selectedShapeType: SelectedShapeType,
    onShapeTypeSelected: (SelectedShapeType) -> Unit,
    selectedTemplateType: InsertedTemplateType,
    onTemplateTypeSelected: (InsertedTemplateType) -> Unit,
    tableColumns: Int,
    onTableColumnsChanged: (Int) -> Unit,
    tableRows: Int,
    onTableRowsChanged: (Int) -> Unit,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    eraserType: EraserType,
    onEraserTypeChanged: (EraserType) -> Unit,
    isDarkTheme: Boolean,
    hasLassoSelection: Boolean,
    onDeleteLassoSelection: () -> Unit,
    onInsertSymbol: (String) -> Unit = {},
    isDrawing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val isPaletteVisible = !isDrawing && (
        activeTool == ActiveTool.PEN ||
        activeTool == ActiveTool.NEON_PEN ||
        activeTool == ActiveTool.HIGHLIGHTER ||
        activeTool == ActiveTool.SHAPE ||
        activeTool == ActiveTool.TEMPLATE ||
        activeTool == ActiveTool.FORMULA ||
        activeTool == ActiveTool.ERASER ||
        (activeTool == ActiveTool.LASSO && hasLassoSelection)
    )

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Płytka Narzędzi (Pionowy Dok)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shadowElevation = 10.dp,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.padding(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SideDockToolItem(
                    icon = Icons.Default.Edit,
                    label = "Pióro",
                    isSelected = activeTool == ActiveTool.PEN,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.PEN)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.AutoAwesome,
                    label = "Neon",
                    isSelected = activeTool == ActiveTool.NEON_PEN,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.NEON_PEN)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.BorderColor,
                    label = "Zakreślacz",
                    isSelected = activeTool == ActiveTool.HIGHLIGHTER,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.HIGHLIGHTER)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.Functions,
                    label = "Wzory",
                    isSelected = activeTool == ActiveTool.FORMULA,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.FORMULA)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.Category,
                    label = "Figury",
                    isSelected = activeTool == ActiveTool.SHAPE,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.SHAPE)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.TableChart,
                    label = "Szablony",
                    isSelected = activeTool == ActiveTool.TEMPLATE,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.TEMPLATE)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.AutoFixNormal,
                    label = "Gumka",
                    isSelected = activeTool == ActiveTool.ERASER,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.ERASER)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.Polyline,
                    label = "Lasso",
                    isSelected = activeTool == ActiveTool.LASSO,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.LASSO)
                    }
                )

                SideDockToolItem(
                    icon = Icons.Default.PanTool,
                    label = "Przesuń",
                    isSelected = activeTool == ActiveTool.HAND,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onToolSelected(ActiveTool.HAND)
                    }
                )
            }
        }

        // Kompaktowa Paleta Opcji (Automatycznie znika podczas pisania - Auto-Hide)
        AnimatedVisibility(
            visible = isPaletteVisible,
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                shadowElevation = 10.dp,
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .width(200.dp)
                    .padding(start = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Wybór symboli i wzorów matematycznych (dla narzędzia FORMULA)
                    if (activeTool == ActiveTool.FORMULA) {
                        Text("Symbole & Wzory:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        var formulaCategory by remember { mutableIntStateOf(0) } // 0: Analiza, 1: Algebra, 2: Fizyka

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            FilterChip(
                                selected = formulaCategory == 0,
                                onClick = { formulaCategory = 0 },
                                label = { Text("Analiza", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = formulaCategory == 1,
                                onClick = { formulaCategory = 1 },
                                label = { Text("Algebra", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = formulaCategory == 2,
                                onClick = { formulaCategory = 2 },
                                label = { Text("Fizyka", fontSize = 9.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val symbols = when (formulaCategory) {
                            0 -> listOf("∫", "∬", "∑", "lim", "d/dx", "√x", "∞", "π", "θ", "Δ")
                            1 -> listOf("Det(A)", "[Macierz]", "v⃗", "λ", "ℝⁿ", "∀x", "∃y", "∈", "⊂", "⊥")
                            else -> listOf("F=m·a", "E=mc²", "Δt", "λ", "ω", "ρ", "μ", "∇×B", "ℏ", "P=U·I")
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            symbols.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    row.forEach { symbol ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onInsertSymbol(symbol)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Wybór typu figury (dla narzędzia SHAPE)
                    if (activeTool == ActiveTool.SHAPE) {
                        Text("Wybierz Figurę:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        
                        val shapes = listOf(
                            SelectedShapeType.RECTANGLE to "🔲 Prostokąt",
                            SelectedShapeType.CIRCLE to "⭕ Kółko",
                            SelectedShapeType.TRIANGLE to "🔺 Trójkąt",
                            SelectedShapeType.LINE to "📏 Linia",
                            SelectedShapeType.ARROW to "➡️ Strzałka"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            shapes.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    row.forEach { (type, label) ->
                                        val isSelected = selectedShapeType == type
                                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bg)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onShapeTypeSelected(type)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textCol)
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Wybór typu szablonu (dla narzędzia TEMPLATE)
                    if (activeTool == ActiveTool.TEMPLATE) {
                        Text("Gotowe Szablony:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        val templates = listOf(
                            InsertedTemplateType.COORDINATE_XY to "📊 Układ XY",
                            InsertedTemplateType.COORDINATE_XYZ to "📈 Osie 3D",
                            InsertedTemplateType.TABLE_GRID to "📋 Tabela",
                            InsertedTemplateType.POLAR_GRID to "🎯 Biegunowa"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            templates.chunked(2).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    row.forEach { (type, label) ->
                                        val isSelected = selectedTemplateType == type
                                        val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bg)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onTemplateTypeSelected(type)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textCol)
                                        }
                                    }
                                }
                            }
                        }

                        // Kustomizacja wymiaru tabeli (Kolumny x Wiersze)
                        if (selectedTemplateType == InsertedTemplateType.TABLE_GRID) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Rozmiar: ${tableColumns}x${tableRows}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Kolumny:", fontSize = 10.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onTableColumnsChanged((tableColumns - 1).coerceAtLeast(1)) },
                                                modifier = Modifier.size(20.dp)
                                            ) { Text("-", fontWeight = FontWeight.Bold) }
                                            Text("$tableColumns", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { onTableColumnsChanged((tableColumns + 1).coerceAtMost(10)) },
                                                modifier = Modifier.size(20.dp)
                                            ) { Text("+", fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Wiersze:", fontSize = 10.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onTableRowsChanged((tableRows - 1).coerceAtLeast(1)) },
                                                modifier = Modifier.size(20.dp)
                                            ) { Text("-", fontWeight = FontWeight.Bold) }
                                            Text("$tableRows", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { onTableRowsChanged((tableRows + 1).coerceAtMost(10)) },
                                                modifier = Modifier.size(20.dp)
                                            ) { Text("+", fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    if (activeTool == ActiveTool.LASSO && hasLassoSelection) {
                        Button(
                            onClick = onDeleteLassoSelection,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Usuń", fontSize = 11.sp)
                        }
                    }

                    if (activeTool == ActiveTool.ERASER) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = eraserType == EraserType.OBJECT,
                                onClick = { onEraserTypeChanged(EraserType.OBJECT) },
                                label = { Text("Obiekt", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = eraserType == EraserType.PIXEL,
                                onClick = { onEraserTypeChanged(EraserType.PIXEL) },
                                label = { Text("Piksel", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (activeTool == ActiveTool.PEN || activeTool == ActiveTool.NEON_PEN || activeTool == ActiveTool.HIGHLIGHTER || activeTool == ActiveTool.SHAPE || activeTool == ActiveTool.TEMPLATE || (activeTool == ActiveTool.LASSO && hasLassoSelection)) {
                        val colors = if (activeTool == ActiveTool.PEN || activeTool == ActiveTool.NEON_PEN || activeTool == ActiveTool.SHAPE || activeTool == ActiveTool.TEMPLATE || activeTool == ActiveTool.LASSO) {
                            if (isDarkTheme) NOTE_COLORS_DARK else NOTE_COLORS_LIGHT
                        } else HIGHLIGHTER_COLORS

                        // Wiersz 1: Próbki kolorów
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            colors.forEach { color ->
                                val isSelected = currentColor == color
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onColorSelected(color)
                                        }
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (color == Color.White) Color.Black else Color.White)
                                        )
                                    }
                                }
                            }
                        }

                        // Wiersz 2: Wybór grubości (Cienka / Średnia / Gruba)
                        if (activeTool == ActiveTool.PEN || activeTool == ActiveTool.NEON_PEN || activeTool == ActiveTool.HIGHLIGHTER || activeTool == ActiveTool.SHAPE || activeTool == ActiveTool.TEMPLATE) {
                            val widths = if (activeTool == ActiveTool.PEN || activeTool == ActiveTool.NEON_PEN || activeTool == ActiveTool.SHAPE || activeTool == ActiveTool.TEMPLATE) {
                                listOf(2f to "1", 4f to "2", 8f to "3")
                            } else {
                                listOf(16f to "1", 28f to "2")
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                widths.forEach { (width, label) ->
                                    val isSelected = currentStrokeWidth == width
                                    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(26.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(bg)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onStrokeWidthSelected(width)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (label == "1") "Cienka" else if (label == "2") "Średnia" else "Gruba",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SideDockToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
