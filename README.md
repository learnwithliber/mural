# Mural

A dark, minimal Android wallpaper maker. Bring your own photo or your own words, style them, and set the result as your home screen, lock screen, or both in one tap.

## What it does

- **Background:** your own photo (drag to move, pinch to zoom), a gradient, or a solid color, with a custom color picker.
- **Text:** your own words in 8 bundled fonts, with size, weight, alignment, color, letter spacing, line height, all caps, shadow, and a dark band for readability. Drag the text on the preview to place it.
- **Adjust:** brightness, contrast, color, blur, dim, and grain.
- **Lock screen preview:** shows the clock over your design so you can keep the time readable.
- **Set wallpaper:** home, lock, or both, made at your screen's exact pixel size.
- **Save:** writes a PNG to `Pictures/Mural`.
- **My wallpapers:** every saved or applied design is kept so you can reopen and keep editing.
- **Share to Mural:** share a photo from Gallery and it opens straight in the editor.

Requires Android 10 or newer.

## Get the APK without a computer (GitHub)

1. Create a new GitHub repository and upload this whole folder to it.
2. GitHub builds the app automatically (Actions tab, "Build APK", about 5 minutes).
3. Open the finished run, download **Mural-apk**, unzip it, and install `app-release.apk` on your phone. Allow "Install unknown apps" when asked.

## Build in Android Studio

1. Open this folder in Android Studio (Ladybug or newer).
2. Let Gradle sync, then press Run with your phone connected or an emulator open.

To publish on the Play Store, create your own signing key (Build > Generate Signed App Bundle) and replace the debug signing line in `app/build.gradle.kts`.

## Project layout

```
app/src/main/java/app/mural/
  WallState.kt       everything that describes one wallpaper
  Renderer.kt        draws the wallpaper (same code for preview and final image)
  Fonts.kt           bundled fonts
  MuralViewModel.kt  photo loading, saving, setting wallpaper, My wallpapers
  MainActivity.kt    app entry, share-to-Mural
  ui/EditorScreen.kt the editor
  ui/Theme.kt        colors
app/src/main/assets/fonts/  fonts (SIL Open Font License, see OFL.txt)
```
