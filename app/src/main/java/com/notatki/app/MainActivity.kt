package com.notatki.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notatki.app.data.model.AppThemeMode
import com.notatki.app.ui.screens.canvas.NoteCanvasScreen
import com.notatki.app.ui.screens.hub.HubScreen
import com.notatki.app.ui.screens.lessons.LessonsScreen
import com.notatki.app.ui.screens.settings.SettingsScreen
import com.notatki.app.ui.theme.NotatkiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as NotatkiApp

        setContent {
            val settings by app.settingsRepository.settings.collectAsState()
            val isSystemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemDark
            }

            val window = this.window
            val view = LocalView.current
            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                if (settings.immersiveMode) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            NotatkiTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "hub"
    ) {
        // Ekran 1: HUB Przedmiotów
        composable("hub") {
            HubScreen(
                onSubjectClick = { subjectId ->
                    navController.navigate("lessons/$subjectId")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        // Ekran 2: Ustawienia
        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Ekran 3: Lekcje / Tematy w danym przedmiocie
        composable(
            route = "lessons/{subjectId}",
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getLong("subjectId") ?: 0L
            LessonsScreen(
                subjectId = subjectId,
                onBackClick = { navController.popBackStack() },
                onLessonClick = { lessonId ->
                    navController.navigate("canvas/$lessonId")
                }
            )
        }

        // Ekran 4: Zeszyt w kratkę (Nieskończona tablica, Rysik, Kalkulator, PDF)
        composable(
            route = "canvas/{lessonId}",
            arguments = listOf(navArgument("lessonId") { type = NavType.LongType })
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getLong("lessonId") ?: 0L
            NoteCanvasScreen(
                lessonId = lessonId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
