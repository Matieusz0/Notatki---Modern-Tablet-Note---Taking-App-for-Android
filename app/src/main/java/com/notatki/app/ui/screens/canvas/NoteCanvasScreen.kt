package com.notatki.app.ui.screens.canvas

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notatki.app.ui.screens.canvas.components.ActiveTool
import com.notatki.app.ui.screens.canvas.components.CanvasTopBar
import com.notatki.app.ui.screens.canvas.components.FloatingSideDock
import com.notatki.app.ui.screens.canvas.components.PdfPageSelectorDialog
import com.notatki.app.ui.screens.canvas.components.ScientificCalculatorDialog
import com.notatki.app.ui.theme.NeonCyan

@Composable
fun NoteCanvasScreen(
    lessonId: Long,
    onBackClick: () -> Unit,
    viewModel: CanvasViewModel = viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }

    val lessonTitle by viewModel.lessonTitle.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val currentStroke by viewModel.currentDrawingStroke.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val lassoPoints by viewModel.lassoPoints.collectAsState()
    val selectedStrokeIds by viewModel.selectedStrokeIds.collectAsState()
    val canvasImages by viewModel.canvasImages.collectAsState()
    val selectedImageId by viewModel.selectedImageId.collectAsState()

    val settings by viewModel.settings.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (settings.themeMode) {
        com.notatki.app.data.model.AppThemeMode.DARK -> true
        com.notatki.app.data.model.AppThemeMode.LIGHT -> false
        com.notatki.app.data.model.AppThemeMode.SYSTEM -> isSystemDark
    }
    val paperStyle by viewModel.paperStyle.collectAsState()

    // Jeśli tryb ciemny i kolor jest czarny, ustaw domyślnie biały tusz
    LaunchedEffect(isDarkTheme) {
        if (isDarkTheme && viewModel.currentColor.value == androidx.compose.ui.graphics.Color(0xFF000000)) {
            viewModel.currentColor.value = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        } else if (!isDarkTheme && viewModel.currentColor.value == androidx.compose.ui.graphics.Color(0xFFFFFFFF)) {
            viewModel.currentColor.value = androidx.compose.ui.graphics.Color(0xFF000000)
        }
    }

    var showCalculator by remember { mutableStateOf(false) }
    var showRuler by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }

    // Wybór zdjęć z aparatu / galerii
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.insertImageFromUri(it) }
    }

    // Wybór dokumentów PDF / prezentacji
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
        }
    }

    Scaffold(
        topBar = {
            CanvasTopBar(
                lessonTitle = lessonTitle,
                stylusOnlyMode = viewModel.stylusOnlyMode.value,
                onStylusOnlyModeToggled = {
                    viewModel.stylusOnlyMode.value = !viewModel.stylusOnlyMode.value
                },
                autoShapeEnabled = viewModel.autoShapeEnabled.value,
                onAutoShapeToggled = {
                    viewModel.autoShapeEnabled.value = !viewModel.autoShapeEnabled.value
                },
                showRuler = showRuler,
                onRulerToggled = { showRuler = !showRuler },
                canUndo = canUndo,
                onUndo = { viewModel.undo() },
                canRedo = canRedo,
                onRedo = { viewModel.redo() },
                onInsertImageClick = { imagePickerLauncher.launch("image/*") },
                onInsertPdfClick = { pdfPickerLauncher.launch(arrayOf("application/pdf", "*/*")) },
                onCalculatorClick = { showCalculator = true },
                onExportPdfClick = { showExportDialog = true },
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            InfiniteCanvas(
                strokes = strokes,
                currentStroke = currentStroke,
                activeTool = viewModel.activeTool.value,
                eraserType = viewModel.eraserType.value,
                stylusOnlyMode = viewModel.stylusOnlyMode.value,
                paperStyle = paperStyle,
                isDarkTheme = isDarkTheme,
                lassoPoints = lassoPoints,
                selectedStrokeIds = selectedStrokeIds,
                canvasImages = canvasImages,
                selectedImageId = selectedImageId,
                showRuler = showRuler,
                onStrokeStart = { viewModel.startStroke(it) },
                onStrokeMove = { viewModel.addPointToStroke(it) },
                onStrokeEnd = { viewModel.finishStroke() },
                onDragShapeUpdate = { start, curr -> viewModel.updateDragShape(start, curr) },
                onDragTemplateUpdate = { start, curr -> viewModel.updateDragTemplate(start, curr) },
                onInsertFormulaAt = { pos ->
                    val sym = viewModel.pendingFormulaSymbol.value ?: "∫"
                    viewModel.insertFormulaAt(sym, pos.x, pos.y)
                },
                onObjectErase = { viewModel.eraseObjectAt(it) },
                onPixelErase = { point, radius -> viewModel.erasePixelAt(point, radius) },
                onLassoMove = { viewModel.onLassoMove(it) },
                onLassoEnd = { viewModel.onLassoEnd() },
                onMoveSelectedStrokes = { dx, dy -> viewModel.moveSelectedStrokes(dx, dy) },
                onSelectImage = { id -> viewModel.selectCanvasImage(id) },
                onUpdateImageTransform = { id, dx, dy, scale, rot ->
                    viewModel.updateImageTransform(id, dx, dy, scale, rot)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Boczny Pływający Dok Narzędziowy z Auto-Ukrywaniem podczas pisania
            FloatingSideDock(
                activeTool = viewModel.activeTool.value,
                onToolSelected = {
                    viewModel.activeTool.value = it
                    if (it != ActiveTool.LASSO) {
                        viewModel.clearLassoSelection()
                    }
                },
                selectedShapeType = viewModel.selectedShapeType.value,
                onShapeTypeSelected = { viewModel.selectedShapeType.value = it },
                selectedTemplateType = viewModel.selectedTemplateType.value,
                onTemplateTypeSelected = { viewModel.selectedTemplateType.value = it },
                tableColumns = viewModel.tableColumns.intValue,
                onTableColumnsChanged = { viewModel.tableColumns.intValue = it },
                tableRows = viewModel.tableRows.intValue,
                onTableRowsChanged = { viewModel.tableRows.intValue = it },
                currentColor = viewModel.currentColor.value,
                onColorSelected = { color ->
                    viewModel.currentColor.value = color
                    if (viewModel.activeTool.value == ActiveTool.LASSO) {
                        viewModel.recolorSelectedStrokes(color)
                    }
                },
                currentStrokeWidth = viewModel.currentStrokeWidth.floatValue,
                onStrokeWidthSelected = { viewModel.currentStrokeWidth.floatValue = it },
                eraserType = viewModel.eraserType.value,
                onEraserTypeChanged = { viewModel.eraserType.value = it },
                isDarkTheme = isDarkTheme,
                hasLassoSelection = selectedStrokeIds.isNotEmpty(),
                onDeleteLassoSelection = { viewModel.deleteSelectedStrokes() },
                onInsertSymbol = { symbol -> viewModel.selectFormulaSymbol(symbol) },
                isDrawing = currentStroke != null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            )

            // Pływający pasek narzędzi edycji zaznaczonego obrazka
            if (selectedImageId != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Obraz", style = MaterialTheme.typography.labelLarge, fontSize = 13.sp)

                        IconButton(onClick = {
                            viewModel.updateImageTransform(selectedImageId!!, scaleFactor = 1.15f)
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Powiększ")
                        }

                        IconButton(onClick = {
                            viewModel.updateImageTransform(selectedImageId!!, scaleFactor = 0.85f)
                        }) {
                            Icon(Icons.Default.Remove, contentDescription = "Zmniejsz")
                        }

                        IconButton(onClick = {
                            viewModel.updateImageTransform(selectedImageId!!, rotationDelta = 90f)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Obróć 90°")
                        }

                        IconButton(onClick = { viewModel.deleteSelectedImage() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Usuń obraz", tint = MaterialTheme.colorScheme.error)
                        }

                        IconButton(onClick = { viewModel.selectCanvasImage(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Odznacz")
                        }
                    }
                }
            }

            // Dialog wyboru wywoływany z przycisku Eksport
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text("Eksportuj Notatkę", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Wybierz format pliku, który ma zostać zapisany w folderze Pobrane (Downloads) na tablecie:", fontSize = 13.sp)

                            Button(
                                onClick = {
                                    showExportDialog = false
                                    viewModel.exportToPdf { result ->
                                        result.onSuccess { file ->
                                            Toast.makeText(context, "Zapisano PDF w Pobrane:\n${file.name}", Toast.LENGTH_LONG).show()
                                            try {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/pdf"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Udostępnij PDF"))
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }.onFailure {
                                            Toast.makeText(context, "Błąd eksportu: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Plik PDF (.pdf)", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    showExportDialog = false
                                    viewModel.exportToNotatkiProject { result ->
                                        result.onSuccess { file ->
                                            Toast.makeText(context, "Zapisano Projekt w Pobrane:\n${file.name}", Toast.LENGTH_LONG).show()
                                            try {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "*/*"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Udostępnij Projekt .notatki"))
                                            } catch (e: Exception) { e.printStackTrace() }
                                        }.onFailure {
                                            Toast.makeText(context, "Błąd kopii zapasowej: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Plik Projektu (.notatki)", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showExportDialog = false }) {
                            Text("Anuluj")
                        }
                    }
                )
            }

            // Dialog wyboru konkretnej strony z pliku PDF / Prezentacji
            selectedPdfUri?.let { uri ->
                PdfPageSelectorDialog(
                    pdfUri = uri,
                    onDismiss = { selectedPdfUri = null },
                    onPageSelected = { pdfFile, pageIndex ->
                        viewModel.insertPdfPageFile(pdfFile, pageIndex)
                        selectedPdfUri = null
                    }
                )
            }

            // Pływające okno kalkulatora naukowego
            if (showCalculator) {
                ScientificCalculatorDialog(
                    onDismiss = { showCalculator = false }
                )
            }
        }
    }
}
