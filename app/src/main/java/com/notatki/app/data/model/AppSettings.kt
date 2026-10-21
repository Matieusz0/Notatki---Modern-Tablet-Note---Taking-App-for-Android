package com.notatki.app.data.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class Handedness {
    RIGHT_HANDED,
    LEFT_HANDED
}

enum class AngleUnit {
    DEG,
    RAD
}

enum class PaperStyle {
    GRID,       // Kratka
    LINES,      // Linie
    DOTS,       // Kropki
    MILLIMETER, // Papier Milimetrowy
    HEXAGON,    // Siatka Heksagonalna (Chemia/Fizyka)
    VINTAGE,    // Ciepły Papier Retro
    BLANK       // Czysta
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val handedness: Handedness = Handedness.RIGHT_HANDED,
    val defaultAngleUnit: AngleUnit = AngleUnit.DEG,
    val defaultStylusOnly: Boolean = false,
    val defaultPaperStyle: PaperStyle = PaperStyle.GRID,
    val updateServerUrl: String = "http://matieusz.fun/notatki/version.json",
    val immersiveMode: Boolean = true
)
