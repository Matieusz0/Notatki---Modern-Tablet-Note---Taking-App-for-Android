package com.notatki.app.data.repository

import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import com.notatki.app.data.model.NoteCanvasData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoteStorageRepository(private val context: Context) {
    private val gson = Gson()
    private val notesDir = File(context.filesDir, "notes").apply {
        if (!exists()) mkdirs()
    }

    private fun getFileForLesson(lessonId: Long): File {
        return File(notesDir, "note_$lessonId.json")
    }

    suspend fun saveNote(data: NoteCanvasData) = withContext(Dispatchers.IO) {
        try {
            val file = getFileForLesson(data.lessonId)
            val jsonString = gson.toJson(data)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadNote(lessonId: Long): NoteCanvasData = withContext(Dispatchers.IO) {
        val file = getFileForLesson(lessonId)
        if (!file.exists()) {
            return@withContext NoteCanvasData(lessonId = lessonId)
        }
        try {
            val jsonString = file.readText()
            gson.fromJson(jsonString, NoteCanvasData::class.java) ?: NoteCanvasData(lessonId = lessonId)
        } catch (e: Exception) {
            e.printStackTrace()
            NoteCanvasData(lessonId = lessonId)
        }
    }

    suspend fun deleteNote(lessonId: Long) = withContext(Dispatchers.IO) {
        val file = getFileForLesson(lessonId)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun exportProjectBackup(
        context: Context,
        lessonId: Long,
        title: String,
        noteData: NoteCanvasData
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            val cleanTitle = title.replace("[^a-zA-Z0-9ąćęłńóśźżĄĆĘŁŃÓŚŹŻ_\\-]".toRegex(), "_")
            val backupFile = File(downloadsDir, "${cleanTitle}_projekt.notatki")

            val jsonString = gson.toJson(noteData)
            backupFile.writeText(jsonString)

            Result.success(backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
