<p align="center">
  <img src="docs/logo.svg" width="220" alt="Lucida Studio Logo" />
</p>

<h1 align="center">Lucida Studio</h1>

<p align="center">
  <strong>A modern, distraction-free digital Camera Lucida for Android artists and illustrators.</strong>
</p>

<p align="center">
  <a href="https://github.com/Yashaswi-Katta/Lucida/actions/workflows/android-cloud-build.yml">
    <img src="https://github.com/Yashaswi-Katta/Lucida/actions/workflows/android-cloud-build.yml/badge.svg" alt="Build Status" />
  </a>
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--34)-3DDC84?logo=android" alt="Android API" />
  <img src="https://img.shields.io/badge/Language-Kotlin%201.9.24-7F52FF?logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose" alt="Compose" />
  <img src="https://img.shields.io/badge/Styling-Dark%20Studio-FFB300" alt="Theme" />
</p>

---

## Overview

**Lucida Studio** (`com.lucida.studio`) reimagines the classical optical [Camera Lucida](https://en.wikipedia.org/wiki/Camera_lucida) for the digital age. By mounting your phone on a stand or desktop clamp facing your drawing surface, Lucida superimposes your digital reference artwork directly over your live camera feed. 

Artists can look through their screen and accurately trace, align proportions, isolate tonal values, and calibrate perspective onto physical paper, canvas, or walls.

---

## ✨ Key Features

### 🔍 Optical Superposition & Gesture Workspace
* **Fluid Two-Finger Canvas Manipulation:** Freeform pan, pinch-to-zoom, and rotation gestures to position your digital reference art anywhere across the drawing surface.
* **Instant Alpha Blend Control:** Smooth thumb slider on the floating HUD to dial opacity from 0% (full paper visibility) to 100% (full digital reference).
* **Format Agnostic:** Import PNG, JPEG, WebP, and Vector SVG graphics through Android's Storage Access Framework (SAF) with automatic hardware-safe downsampling.

### 📌 3D Perspective Homography & Precision Loupe
* **4-Point Corner Calibration:** Phone mounted at an angle? Drag four calibration pins (Top-Left, Top-Right, Bottom-Right, Bottom-Left) to the corners of your physical paper.
* **Direct Linear Transform (DLT):** Warps and maps your flat reference into real 3D perspective to match your physical sheet.
* **One-Euro Jitter Filter:** Adaptive low-pass filtering on pin coordinates eliminates touch micro-jitters.
* **Offset Magnification Loupe:** A circular magnifying zoom lens floats above your fingertip while dragging pins so your finger never obstructs the alignment target.

### 🎨 Artist Vision Filters
* **Sobel Stencil Edges:** Extracts clean, high-contrast monochrome contours from complex reference images with real-time threshold adjustment (10–120) and inversion.
* **Tonal Solo Posterizer:** Isolates lighting and values into distinct categories (*Shadows*, *Dark Midtones*, *Light Midtones*, *Highlights*) to paint accurate tonal studies one layer at a time.
* **Rule-of-Thirds & Golden Ratio Grids:** Overlay precision compositional grids with customizable density and opacity.

### 📸 Direct MediaStore Gallery Integration
* **One-Tap Clean Snapshots:** The camera's dedicated `ImageCapture` pipeline takes high-resolution stills of your physical drawing surface completely independent of the Compose UI.
* **Automatic Gallery Indexing:** Snapshots are written directly to Android's public `MediaStore` in **`Pictures/Lucida`**, appearing instantly in OnePlus Photos, Google Photos, and system galleries without needing legacy file permissions.

### 🔒 Distraction-Free Ghost Mode & Hardware Lock
* **Full UI Concealment:** Tapping **Lock** (or pressing the physical **Volume Down** button) instantly hides all headers, drawers, sliders, and gestures.
* **Palm Rejection:** Rest your hand or wrist naturally on the screen without accidentally shifting, zooming, or rotating your artwork.
* **Volume Key Override:** Press **Volume Down** again anytime to exit Ghost Mode without looking away from your easel.

### ⚙️ Pro Camera Controls & Telemetry
* **Camera2 AF/AE Lock:** Lock auto-focus and auto-exposure with a single tap so passing hands and shifting paper do not trigger lens hunting or brightness swings.
* **Studio Telemetry HUD:** Real-time HUD displaying camera resolution, sensor lux, estimated EV, preview FPS, and battery status.
* **Flashlight Torch:** Toggle device back torch for working in dimly lit studios.
* **Timed Frame Sequences:** Automated time-lapse frame recorder (5s to 60s intervals) exporting complete ZIP archives with timestamped `manifest.csv`.

### 📖 Interactive Studio Tutorial
* **First-Launch Onboarding:** Automatically welcomes new artists after camera permission is granted with a 4-step dynamic walkthrough.
* **Live Interactive Slider:** Includes a hands-on opacity test playground directly inside the dialog card.
* **Always Accessible:** Replay the complete tutorial anytime via **Tools Drawer > 📖 Guide**.

### 🛡️ Rock-Solid Stability
* **Native System Permissions:** Directly triggers Android's official permission prompt on launch.
* **Lifecycle-Safe Edge-to-Edge:** Compatible with Android 14+ / OxygenOS 16.
* **Synchronous Crash Capture:** If an unexpected fault occurs, Lucida intercepts the exception, writes a detailed trace to disk, and displays a recovery card with one-click clipboard copying.

---

## 📱 Download & Installation

### Option 1: Download Latest Build (GitHub Actions)
Precompiled, installable debug APKs are automatically built on every commit:
1. Visit the [Lucida GitHub Actions Runs](https://github.com/Yashaswi-Katta/Lucida/actions/workflows/android-cloud-build.yml).
2. Click on the latest run.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download **`Lucida-Debug-APK`**, extract the ZIP, and install the `.apk` on your Android device (API 26+).

### Option 2: Build from Source
Requirements:
* **JDK 17+**
* **Android SDK Platform 34 & Build-Tools**
* **Gradle 8.6+**

```bash
# Clone the repository
git clone https://github.com/Yashaswi-Katta/Lucida.git
cd Lucida

# Generate wrapper (if needed)
gradle wrapper --gradle-version 8.6

# Run unit tests (Homography math & One-Euro filter)
./gradlew :app:testDebugUnitTest

# Assemble debug APK
./gradlew :app:assembleDebug
```
The output APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🏛️ Architecture & Project Structure

```
Lucida/
├── .github/workflows/
│   └── android-cloud-build.yml         # Automated CI (JDK 17 + Gradle 8.6 + APK Artifact)
├── docs/
│   └── logo.svg                        # Official Dark Studio vector brand logo
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml         # Hardware camera features & scoped storage
│   │   ├── res/
│   │   │   ├── drawable/               # Dark Studio launcher & splash vector assets
│   │   │   └── values/                 # Edge-to-edge transparent splash theme
│   │   └── java/com/lucida/studio/
│   │       ├── LucidaApp.kt            # Application subclass, Coil SVG factory, crash logger
│   │       ├── MainActivity.kt         # Edge-to-edge init, Volume Down key interceptor
│   │       ├── domain/
│   │       │   ├── imaging/
│   │       │   │   ├── DocumentImageLoader.kt    # SAF image importer & bounds decoder
│   │       │   │   ├── StencilEngine.kt          # Sobel edge filter & contour extractor
│   │       │   │   ├── TonalSoloEngine.kt        # 4-tier value isolation posterizer
│   │       │   │   └── TimelapseFrameRecorder.kt # MediaStore Pictures/Lucida & ZIP exporter
│   │       │   └── math/
│   │       │       ├── HomographySolver.kt       # Direct Linear Transform 3D matrix math
│   │       │       └── OneEuroFilter.kt          # 60fps jitter-reduction low-pass filter
│   │       └── ui/
│   │           ├── theme/                        # Dark Studio palette (Charcoal, Amber, Crimson)
│   │           └── workspace/
│   │               ├── WorkspaceState.kt         # MVI immutable workspace model
│   │               ├── WorkspaceViewModel.kt     # State machine & DataStore persistence
│   │               ├── WorkspaceScreen.kt        # Root layout, SAF launcher, dialog coordinator
│   │               └── components/
│   │                   ├── CameraPreviewLayer.kt      # CameraX preview & permission launcher
│   │                   ├── StudioCameraController.kt  # Camera2 AF/AE lock, torch, capture
│   │                   ├── TransformableCanvas.kt     # PointerInput pan/zoom/rotate engine
│   │                   ├── CornerCalibrationOverlay.kt# 4-pin homography pins & loupe
│   │                   ├── GridOverlayDrawer.kt       # Compositional rule lines
│   │                   ├── TelemetryTopBar.kt         # FPS, EV, resolution, battery HUD
│   │                   ├── StudioHudBar.kt            # Bottom dock, opacity slider, tools tabs
│   │                   └── StudioTutorialDialog.kt    # 4-step dynamic onboarding walkthrough
```

---

## 🛠️ Studio Best Practices

1. **Mounting:** Use a desktop gooseneck clamp, tripod, or elevated stand positioned parallel to your drawing pad.
2. **Focus Lock:** Point the camera at your paper, allow autofocus to settle, then tap **Lock Focus/AE** in the Tools drawer. This prevents your hand from triggering focus hunting.
3. **Ghost Mode Drawing:** Once aligned, hit **Volume Down** on your phone. All controls will vanish, allowing you to trace distraction-free and rest your hand freely on the glass.

---

## 📄 License

Lucida Studio is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
