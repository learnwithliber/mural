package app.mural

import android.app.Application
import android.app.WallpaperManager
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class EditorTab(val label: String) { Background("Background"), Text("Text"), Adjust("Adjust") }
enum class WallTarget(val label: String) { Home("Home screen"), Lock("Lock screen"), Both("Home and lock screen") }

class SavedWall(val dir: File, val thumb: ImageBitmap)

class MuralViewModel(app: Application) : AndroidViewModel(app) {

    /** Paints the preview (main thread). Export uses its own instance off the main thread. */
    val renderer = Renderer()
    private val exportRenderer = Renderer()

    var state by mutableStateOf(WallState())
        private set
    var photo by mutableStateOf<Bitmap?>(null)
        private set
    var tab by mutableStateOf(EditorTab.Background)
    var showLock by mutableStateOf(true)
    var busy by mutableStateOf(false)
        private set
    val saved = mutableStateListOf<SavedWall>()
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** The phone's real screen size in pixels, portrait. Wallpapers are made at exactly this size. */
    val screenW: Int
    val screenH: Int

    private val wallsDir = File(app.filesDir, "walls")

    init {
        Fonts.init(app)
        val wm = app.getSystemService(WindowManager::class.java)
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
        var sw = min(w, h).coerceAtLeast(1)
        var sh = max(w, h).coerceAtLeast(1)
        if (sh > 4000) { sw = sw * 4000 / sh; sh = 4000 }
        screenW = sw
        screenH = sh
        refreshSaved()
    }

    fun update(change: WallState.() -> WallState) {
        state = state.change()
    }

    /** Drag and pinch on the preview. dx/dy are fractions of the preview size. */
    fun onGesture(dx: Float, dy: Float, zoom: Float) {
        val s = state
        val p = photo
        val hasPhoto = s.bgType == BgType.Photo && p != null
        val textMovable = s.textOn && s.text.isNotBlank()
        state = when {
            textMovable && (tab == EditorTab.Text || !hasPhoto) ->
                s.copy(tx = (s.tx + dx).coerceIn(0f, 1f), ty = (s.ty + dy).coerceIn(0f, 1f))
            hasPhoto && p != null -> Renderer.clampPhoto(
                s.copy(zoom = (s.zoom * zoom).coerceIn(1f, 4f), ox = s.ox + dx, oy = s.oy + dy),
                p.width, p.height, screenW.toFloat() / screenH
            )
            else -> s
        }
    }

    fun loadPhoto(uri: Uri) = viewModelScope.launch {
        busy = true
        try {
            val bmp = withContext(Dispatchers.IO) { decode(uri) }
            photo = bmp
            state = state.copy(bgType = BgType.Photo, zoom = 1f, ox = 0f, oy = 0f)
        } catch (e: Exception) {
            messages.tryEmit("That file couldn't be opened as an image. Try a JPG or PNG.")
        } finally {
            busy = false
        }
    }

    private fun decode(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(getApplication<Application>().contentResolver, uri)
        val maxDim = max(screenH * 2, 2400).coerceAtMost(4096)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val longSide = max(w, h)
            if (longSide > maxDim) {
                val k = maxDim.toFloat() / longSide
                decoder.setTargetSize((w * k).toInt().coerceAtLeast(1), (h * k).toInt().coerceAtLeast(1))
            }
        }
    }

    fun removePhoto() {
        photo = null
        state = state.copy(bgType = BgType.Gradient)
    }

    fun saveToGallery() = viewModelScope.launch {
        busy = true
        try {
            val s = state
            val p = photo
            withContext(Dispatchers.Default) {
                val bmp = exportRenderer.render(screenW, screenH, s, p)
                writeToPictures(bmp)
                keep(bmp, s, p)
            }
            refreshSaved()
            messages.tryEmit("Saved to Pictures/Mural")
        } catch (e: Exception) {
            messages.tryEmit("Couldn't save the image. Check that your phone has free storage.")
        } finally {
            busy = false
        }
    }

    fun setWallpaper(target: WallTarget) = viewModelScope.launch {
        busy = true
        try {
            val s = state
            val p = photo
            withContext(Dispatchers.Default) {
                val bmp = exportRenderer.render(screenW, screenH, s, p)
                val flags = when (target) {
                    WallTarget.Home -> WallpaperManager.FLAG_SYSTEM
                    WallTarget.Lock -> WallpaperManager.FLAG_LOCK
                    WallTarget.Both -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                WallpaperManager.getInstance(getApplication<Application>()).setBitmap(bmp, null, true, flags)
                keep(bmp, s, p)
            }
            refreshSaved()
            messages.tryEmit("Wallpaper set on your ${target.label.lowercase()}")
        } catch (e: Exception) {
            messages.tryEmit("Your phone didn't allow the wallpaper change. Try Save, then set it from Gallery.")
        } finally {
            busy = false
        }
    }

    private fun writeToPictures(bmp: Bitmap) {
        val resolver = getApplication<Application>().contentResolver
        val name = "mural-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + ".png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Mural")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
            ?: error("insert failed")
        resolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("no stream")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    /** Keeps an editable copy in "My wallpapers" (app storage, this phone only). */
    private fun keep(bmp: Bitmap, s: WallState, p: Bitmap?) {
        val dir = File(wallsDir, System.currentTimeMillis().toString()).apply { mkdirs() }
        File(dir, "state.json").writeText(s.toJson())
        val tw = 270
        val th = (tw.toFloat() * bmp.height / bmp.width).toInt().coerceAtLeast(1)
        File(dir, "thumb.jpg").outputStream().use {
            Bitmap.createScaledBitmap(bmp, tw, th, true).compress(Bitmap.CompressFormat.JPEG, 85, it)
        }
        if (s.bgType == BgType.Photo && p != null) {
            File(dir, "photo.jpg").outputStream().use { p.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        }
    }

    private fun refreshSaved() = viewModelScope.launch {
        val list = withContext(Dispatchers.IO) {
            (wallsDir.listFiles()?.toList() ?: emptyList())
                .filter { File(it, "state.json").exists() }
                .sortedByDescending { it.name }
                .mapNotNull { dir ->
                    BitmapFactory.decodeFile(File(dir, "thumb.jpg").path)?.let { SavedWall(dir, it.asImageBitmap()) }
                }
        }
        saved.clear()
        saved.addAll(list)
    }

    fun open(w: SavedWall) = viewModelScope.launch {
        try {
            val (s, p) = withContext(Dispatchers.IO) {
                val st = WallState.fromJson(File(w.dir, "state.json").readText())
                val ph = File(w.dir, "photo.jpg").takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
                st to ph
            }
            photo = p
            state = if (s.bgType == BgType.Photo && p == null) s.copy(bgType = BgType.Gradient) else s
            messages.tryEmit("Opened. Edit away.")
        } catch (e: Exception) {
            messages.tryEmit("That saved wallpaper couldn't be opened.")
        }
    }

    fun delete(w: SavedWall) = viewModelScope.launch {
        withContext(Dispatchers.IO) { w.dir.deleteRecursively() }
        saved.remove(w)
    }
}
