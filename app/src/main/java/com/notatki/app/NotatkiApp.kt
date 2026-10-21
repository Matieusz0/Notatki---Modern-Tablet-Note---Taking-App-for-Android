package com.notatki.app

import android.app.Application
import com.notatki.app.data.local.AppDatabase
import com.notatki.app.data.repository.NoteStorageRepository

class NotatkiApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var storageRepository: NoteStorageRepository
        private set
    lateinit var settingsRepository: com.notatki.app.data.repository.SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        storageRepository = NoteStorageRepository(this)
        settingsRepository = com.notatki.app.data.repository.SettingsRepository(this)
    }
}
