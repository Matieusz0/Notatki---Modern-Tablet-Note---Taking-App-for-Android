package com.notatki.app.ui.screens.hub

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notatki.app.NotatkiApp
import com.notatki.app.data.model.AppSettings
import com.notatki.app.data.model.Subject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HubViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotatkiApp
    private val subjectDao = app.database.subjectDao()

    val settings: StateFlow<AppSettings> = app.settingsRepository.settings

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            subjectDao.getAllSubjects().collect { list ->
                _subjects.value = list
            }
        }
    }

    fun addSubject(name: String, colorHex: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            subjectDao.insertSubject(
                Subject(
                    name = name.trim(),
                    colorHex = colorHex,
                    description = description.trim()
                )
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            subjectDao.deleteSubject(subject)
        }
    }
}
