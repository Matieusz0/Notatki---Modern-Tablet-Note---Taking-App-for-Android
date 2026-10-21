package com.notatki.app.ui.screens.lessons

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notatki.app.NotatkiApp
import com.notatki.app.data.model.Lesson
import com.notatki.app.data.model.PaperStyle
import com.notatki.app.data.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LessonsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotatkiApp
    private val lessonDao = app.database.lessonDao()
    private val subjectDao = app.database.subjectDao()
    private val storageRepo = app.storageRepository

    private val _subject = MutableStateFlow<Subject?>(null)
    val subject: StateFlow<Subject?> = _subject.asStateFlow()

    private val _lessons = MutableStateFlow<List<Lesson>>(emptyList())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private var currentSubjectId: Long = 0

    fun loadSubject(subjectId: Long) {
        currentSubjectId = subjectId
        viewModelScope.launch {
            _subject.value = subjectDao.getSubjectById(subjectId)
            lessonDao.getLessonsForSubject(subjectId).collect { list ->
                _lessons.value = list
            }
        }
    }

    fun addLesson(title: String, chapter: String = "", paperStyle: PaperStyle = PaperStyle.GRID) {
        if (title.isBlank() || currentSubjectId == 0L) return
        val dateFormat = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
        val dateString = dateFormat.format(Date())

        viewModelScope.launch {
            lessonDao.insertLesson(
                Lesson(
                    subjectId = currentSubjectId,
                    title = title.trim(),
                    chapter = chapter.trim(),
                    dateString = dateString,
                    paperStyle = paperStyle.name
                )
            )
        }
    }

    fun toggleFavorite(lesson: Lesson) {
        viewModelScope.launch {
            lessonDao.updateLesson(lesson.copy(isFavorite = !lesson.isFavorite))
        }
    }

    fun deleteLesson(lesson: Lesson) {
        viewModelScope.launch {
            lessonDao.deleteLesson(lesson)
            storageRepo.deleteNote(lesson.id)
        }
    }
}
