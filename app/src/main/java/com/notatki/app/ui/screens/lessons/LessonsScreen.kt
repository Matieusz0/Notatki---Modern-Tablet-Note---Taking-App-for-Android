package com.notatki.app.ui.screens.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notatki.app.data.model.Lesson
import com.notatki.app.data.model.PaperStyle
import com.notatki.app.ui.theme.NeonCyan
import com.notatki.app.ui.theme.NeonYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(
    subjectId: Long,
    onBackClick: () -> Unit,
    onLessonClick: (Long) -> Unit,
    viewModel: LessonsViewModel = viewModel()
) {
    LaunchedEffect(subjectId) {
        viewModel.loadSubject(subjectId)
    }

    val subject by viewModel.subject.collectAsState()
    val lessons by viewModel.lessons.collectAsState()
    var selectedTagFilter by remember { mutableStateOf("ALL") } // ALL, FAVORITE, or specific chapter
    var showAddDialog by remember { mutableStateOf(false) }

    val subjectColor = subject?.let {
        try {
            val parsed = Color(android.graphics.Color.parseColor(it.colorHex))
            if (parsed == Color.Black || parsed == Color(0xFF000000)) NeonCyan else parsed
        } catch (e: Exception) {
            NeonCyan
        }
    } ?: NeonCyan

    // Pobierz wszystkie unikalne działy/tagi w tym zeszycie
    val uniqueChapters = remember(lessons) {
        lessons.map { it.chapter }.filter { it.isNotBlank() }.distinct()
    }

    val filteredLessons = remember(lessons, selectedTagFilter) {
        when (selectedTagFilter) {
            "ALL" -> lessons
            "FAVORITE" -> lessons.filter { it.isFavorite }
            else -> lessons.filter { it.chapter == selectedTagFilter }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF000000),
                tonalElevation = 0.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF121524))
                                .border(1.dp, subjectColor.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Wstecz",
                                tint = subjectColor
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp, 28.dp)
                                    .background(subjectColor, shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = subject?.name ?: "Zeszyt",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${lessons.size} notatek w tym zeszycie",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Pasek filtrowania po Działach / Tagach / Ulubionych
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTagFilter == "ALL",
                            onClick = { selectedTagFilter = "ALL" },
                            label = { Text("Wszystkie (${lessons.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        FilterChip(
                            selected = selectedTagFilter == "FAVORITE",
                            onClick = { selectedTagFilter = "FAVORITE" },
                            label = { Text("★ Ulubione", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(14.dp)) },
                            shape = RoundedCornerShape(12.dp)
                        )

                        uniqueChapters.forEach { chapter ->
                            FilterChip(
                                selected = selectedTagFilter == chapter,
                                onClick = { selectedTagFilter = chapter },
                                label = { Text(chapter, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Nowa lekcja", tint = Color.Black) },
                text = { Text("Nowa Notatka", fontWeight = FontWeight.Bold, color = Color.Black) },
                containerColor = subjectColor,
                shape = RoundedCornerShape(20.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF000000))
        ) {
            if (filteredLessons.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = subjectColor.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (selectedTagFilter != "ALL") "Brak notatek w tej kategorii" else "Czysty Zeszyt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedTagFilter != "ALL") "Przełącz filtr lub stwórz nową notatkę z tym tagiem." else "Ten zeszyt nie zawiera jeszcze żadnych tematów. Kliknij przycisk poniżej, aby otworzyć pierwszą kartkę i zacząć pisać.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLessons, key = { it.id }) { lesson ->
                        LessonOledCard(
                            lesson = lesson,
                            accentColor = subjectColor,
                            onClick = { onLessonClick(lesson.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(lesson) },
                            onDelete = { viewModel.deleteLesson(lesson) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddLessonDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, chapter, paperStyle ->
                viewModel.addLesson(title, chapter, paperStyle)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun LessonOledCard(
    lesson: Lesson,
    accentColor: Color,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val paperLabel = when (lesson.paperStyle) {
        "LINES" -> "Linie"
        "DOTS" -> "Kropki"
        "MILLIMETER" -> "Milimetrowy"
        "HEXAGON" -> "Heksagony"
        "VINTAGE" -> "Vintage"
        "BLANK" -> "Czysta"
        else -> "Kratka"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF090B14))
            .border(1.dp, Color(0xFF1E2436), RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = lesson.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accentColor.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor)
                        ) {
                            Text(
                                text = paperLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        if (lesson.chapter.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E243A)
                            ) {
                                Text(
                                    text = lesson.chapter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (lesson.dateString.isNotBlank()) {
                            Text(
                                text = "• ${lesson.dateString}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (lesson.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Ulubione",
                        tint = if (lesson.isFavorite) NeonYellow else Color.Gray
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opcje",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Usuń Lekcję", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddLessonDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, chapter: String, paperStyle: PaperStyle) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var chapter by remember { mutableStateOf("") }
    var selectedPaperStyle by remember { mutableStateOf(PaperStyle.GRID) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF0F121E),
        title = {
            Text(
                text = "Nowy Temat Lekcji",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tytuł lekcji (np. Całki nieoznaczone)", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF22283A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = chapter,
                    onValueChange = { chapter = it },
                    label = { Text("Dział / Tag (np. Całki, Kolokwium 1)", color = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0xFF22283A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Wybierz wzór liniatury kartki:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PaperStyle.GRID to "Kratka",
                        PaperStyle.LINES to "Linie",
                        PaperStyle.DOTS to "Kropki",
                        PaperStyle.MILLIMETER to "Milimetrowy",
                        PaperStyle.HEXAGON to "Heksagony",
                        PaperStyle.VINTAGE to "Vintage",
                        PaperStyle.BLANK to "Czysta"
                    ).forEach { (style, label) ->
                        FilterChip(
                            selected = selectedPaperStyle == style,
                            onClick = { selectedPaperStyle = style },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, chapter, selectedPaperStyle) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Otwórz Notatnik", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj", color = Color.Gray)
            }
        }
    )
}
