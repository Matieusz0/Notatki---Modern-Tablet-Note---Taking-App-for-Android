# 🖊️ Notatki — Modern Tablet Note-Taking App for Android

A high-performance, open-source Android note-taking application built with **Kotlin** and **Jetpack Compose**, specifically engineered for stylus tablets (Lenovo Precision Pen, Samsung S-Pen, active capacitive pens).

Features an **infinite hardware-accelerated canvas**, **OLED pitch-black dark mode**, **drag-to-create shape tools**, **customizable tables ($C \times R$)**, **academic math/physics formulas**, **PDF slides import**, **Lasso selection**, and **OTA updates**.

---

## 📸 Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/051983c9-b15d-4849-a220-4c778b0a864d" width="48%" alt="Ekran Główny (Hub)" />
  <img src="https://github.com/user-attachments/assets/e97cd83c-2701-42a8-9bae-a530d5ea67f7" width="48%" alt="Nieskończone Płótno (OLED & Neon)" />
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/823993d7-ba0c-4e04-9567-814070ef7ef7" width="48%" alt="Szablony & Tabela C x R" />
</p>

---

## ✨ Features

- **🖌️ Hardware-Accelerated Infinite Canvas:** Matrix transform GPU rendering with zero-allocation touch point buffering and sub-pixel jitter filtering.
- **Palm Rejection ("Tylko Rysik"):** Filters hand/palm touches so you can rest your wrist on the screen while writing.
- **🖤 OLED Pitch Black & Neon Glow Pen:** True OLED black canvas background with glowing neon ink paths for radiant formulas and diagrams.
- **📐 AutoShape & Drag-to-Create:** Ramer-Douglas-Peucker (RDP) shape auto-recognition (triangles, rectangles, circles) plus Paint-style interactive drag-to-create shape tools.
- **📋 Customizable Academic Templates:** XY Coordinate grids, 3D XYZ axes, Polar graphs, and $C \times R$ customizable tables ($1 \times 1$ to $10 \times 10$).
- **🧮 Academic Symbol & Formula Snippets:** Calculus ($\int$, $\sum$, $\lim$), Linear Algebra ($\det(A)$, matrices, $\mathbb{R}^n$), and Physics ($F=ma$, $E=mc^2$, $\nabla \times B$).
- **📸 PDF Slides & Image Insertion:** Import PDF slides/presentations, select specific pages, scale, rotate, and move them directly on the canvas.
- **🪢 Lasso Tool:** Freehand loop selection, 1-click ink recoloring, drag-and-drop moving, and batch deletion.
- **🗺️ Mini-Map Navigator & Screen Ruler:** Bottom-right corner canvas view navigator with 1-tap Recenter button and top screen millimeter ruler.
- **🏷️ Subject Notebooks & Chapter Tags:** Organize notes into notebooks with custom cover colors, chapter tags, and favorite stars.
- **🔄 Over-The-Air (OTA) Updates:** In-app version checker and package installer from `version.json` endpoint without losing any notes.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room DB with KSP (Kotlin Symbol Processing)
- **Asynchrony:** Kotlin Coroutines & StateFlow
- **Vector Rendering:** Canvas API + Hardware Matrix Transforms
- **Updates & PDF:** FileProvider, MediaStore API, PdfDocument
- **Target SDK:** Android 15 (API 35)
- **Min SDK:** Android 8.0 (API 26)

---

## 🚀 Building & Running

1. **Clone the repository:**
   ```bash
   git clone https://github.com/twoj-nick/notatki.git
   cd notatki
   ```

2. **Build the Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected tablet via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📦 Open Source Project Structure

```
notatki/
├── app/                  # Główny moduł Android
│   ├── src/main/java/    # Kod źródłowy Kotlin (UI, ViewModel, Data, DB)
│   ├── src/main/res/     # Zasoby (xml, ikony, style)
│   └── build.gradle.kts  # Konfiguracja modułu
├── web/                  # Strona WWW & Serwer OTA (index.html, version.json)
├── docs/                 # Zrzuty ekranu i dokumentacja
├── build.gradle.kts      # Główny skrypt Gradle
├── settings.gradle.kts   # Ustawienia projektu
└── README.md
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
