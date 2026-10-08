package app.mural.ui

import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.mural.Align
import app.mural.BgType
import app.mural.EXAMPLE_TEXT
import app.mural.EditorTab
import app.mural.Fonts
import app.mural.MuralViewModel
import app.mural.SavedWall
import app.mural.WallTarget
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GRADIENTS = listOf(
    0xFF1F3A3D to 0xFF050807, 0xFF2B2B2B to 0xFF000000, 0xFF3B2A4D to 0xFF0B0710, 0xFF4A2C22 to 0xFF0A0504,
    0xFF14304F to 0xFF02060D, 0xFF5C4A2E to 0xFF0C0904, 0xFF2E4A33 to 0xFF030805, 0xFF5A2236 to 0xFF0A0306,
).map { (a, b) -> a.toInt() to b.toInt() }
private val BG_COLORS = listOf(0xFF000000, 0xFF111111, 0xFF0E1A1F, 0xFF1B1530, 0xFF2A1712, 0xFF22301F, 0xFFE9E3D6, 0xFFB7A58A).map { it.toInt() }
private val TEXT_COLORS = listOf(0xFFF2ECE0, 0xFFFFFFFF, 0xFF000000, 0xFFE6D5B0, 0xFFA9C9BC, 0xFFE9A28B, 0xFF9FB3DE, 0xFFD9B2D0).map { it.toInt() }

private enum class ColorTarget { G1, G2, Background, TextColor }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: MuralViewModel) {
    val s = vm.state
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.loadPhoto(uri)
    }
    val pickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    var showSetSheet by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var colorTarget by remember { mutableStateOf<ColorTarget?>(null) }

    Box(Modifier.fillMaxSize().background(M.Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Header(vm)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) { WallPreview(vm, pickPhoto) }

            Tabs(vm)
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (vm.tab) {
                    EditorTab.Background -> BackgroundPanel(vm, pickPhoto) { colorTarget = it }
                    EditorTab.Text -> TextPanel(vm) { colorTarget = it }
                    EditorTab.Adjust -> AdjustPanel(vm)
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = { showGallery = true }, shape = RoundedCornerShape(12.dp)) {
                    Text("Saved ${vm.saved.size}", color = M.Fg)
                }
                OutlinedButton(onClick = { vm.saveToGallery() }, enabled = !vm.busy, shape = RoundedCornerShape(12.dp)) {
                    Text("Save", color = M.Fg)
                }
                Button(
                    onClick = { showSetSheet = true },
                    enabled = !vm.busy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = M.Accent, contentColor = M.AccentInk),
                ) { Text("Set wallpaper", fontWeight = FontWeight.SemiBold) }
            }
        }
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
    }

    if (showSetSheet) {
        ModalBottomSheet(onDismissRequest = { showSetSheet = false }, containerColor = M.Surface) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set as wallpaper", fontFamily = DisplayFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = M.Fg)
                Text("It's made at your screen's exact size, so it stays sharp.", color = M.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                WallTarget.entries.forEach { t ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(M.Surface2)
                            .clickable { showSetSheet = false; vm.setWallpaper(t) }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) { Text(t.label, color = M.Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    if (showGallery) {
        ModalBottomSheet(onDismissRequest = { showGallery = false }, containerColor = M.Surface) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("My wallpapers", fontFamily = DisplayFont, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = M.Fg)
                if (vm.saved.isEmpty()) {
                    Text(
                        "Wallpapers you save or set show up here, so you can open them and keep editing.",
                        color = M.Muted, modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Text("Tap one to keep editing it.", color = M.Muted, fontSize = 13.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.heightIn(max = 520.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(vm.saved, key = { it.dir.path }) { w ->
                            SavedTile(w, onOpen = { vm.open(w); showGallery = false }, onDelete = { vm.delete(w) })
                        }
                    }
                }
            }
        }
    }

    colorTarget?.let { target ->
        val start = when (target) {
            ColorTarget.G1 -> s.g1
            ColorTarget.G2 -> s.g2
            ColorTarget.Background -> s.color
            ColorTarget.TextColor -> s.textColor
        }
        ColorPickerDialog(start, onDismiss = { colorTarget = null }) { c ->
            vm.update {
                when (target) {
                    ColorTarget.G1 -> copy(g1 = c)
                    ColorTarget.G2 -> copy(g2 = c)
                    ColorTarget.Background -> copy(color = c)
                    ColorTarget.TextColor -> copy(textColor = c)
                }
            }
            colorTarget = null
        }
    }
}

private val DisplayFont: FontFamily by lazy { FontFamily(Fonts.get("Fraunces", true)) }

@Composable
private fun Header(vm: MuralViewModel) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Mural", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = M.Fg)
        Text(".", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = M.Accent)
        Spacer(Modifier.weight(1f))
        FilterChip(
            selected = vm.showLock,
            onClick = { vm.showLock = !vm.showLock },
            label = { Text("Lock screen") },
            shape = RoundedCornerShape(50),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = M.Accent, selectedLabelColor = M.AccentInk, labelColor = M.Muted,
            ),
        )
    }
}

@Composable
private fun WallPreview(vm: MuralViewModel, onPick: () -> Unit) {
    val s = vm.state
    val photo = vm.photo
    val ratio = vm.screenW.toFloat() / vm.screenH
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .aspectRatio(ratio, matchHeightConstraintsFirst = true)
            .clip(shape)
            .border(1.dp, M.Line, shape)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        vm.onGesture(pan.x / size.width, pan.y / size.height, zoom)
                    }
                }
        ) {
            drawIntoCanvas { vm.renderer.draw(it.nativeCanvas, size.width, size.height, s, photo) }
        }
        if (vm.showLock) LockOverlay()
        if (s.bgType == BgType.Photo && photo == null) {
            Column(
                Modifier.align(Alignment.Center).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Your photo goes here", color = M.Muted, fontSize = 13.sp)
                AccentButton("Choose a photo", onPick)
            }
        }
        if (vm.busy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = M.Accent)
    }
}

@Composable
private fun LockOverlay() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { delay(20_000); now = Date() } }
    val timeFmt = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm"
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val dateSize = with(density) { (maxWidth * 0.05f).toSp() }
        val timeSize = with(density) { (maxWidth * 0.24f).toSp() }
        Column(
            Modifier.fillMaxWidth().padding(top = maxHeight * 0.12f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now), color = Color.White, fontSize = dateSize, fontWeight = FontWeight.Medium)
            Text(SimpleDateFormat(timeFmt, Locale.getDefault()).format(now), color = Color.White, fontSize = timeSize, lineHeight = timeSize)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = maxHeight * 0.025f)
                .width(maxWidth * 0.3f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.75f))
        )
    }
}

@Composable
private fun Tabs(vm: MuralViewModel) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        EditorTab.entries.forEach { t ->
            val selected = vm.tab == t
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) M.Surface2 else Color.Transparent)
                    .clickable { vm.tab = t }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(t.label, color = if (selected) M.Fg else M.Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/* ---------------- panels ---------------- */

@Composable
private fun ColumnScope.BackgroundPanel(vm: MuralViewModel, onPick: () -> Unit, onCustomColor: (ColorTarget) -> Unit) {
    val s = vm.state
    Segmented(listOf("Photo", "Gradient", "Color"), s.bgType.ordinal) { i -> vm.update { copy(bgType = BgType.entries[i]) } }
    when (s.bgType) {
        BgType.Photo -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AccentButton(if (vm.photo == null) "Choose a photo" else "Replace photo", onPick)
                if (vm.photo != null) TextButton(onClick = { vm.removePhoto() }) { Text("Remove", color = M.Danger) }
            }
            Hint("Pick any photo from your phone. Drag it on the preview to move it, pinch to zoom.")
            if (vm.photo != null) {
                SliderRow("Zoom", s.zoom, 1f..4f, "%.2f×".format(s.zoom)) { vm.update { copy(zoom = it) } }
            }
        }
        BgType.Gradient -> {
            Label("Presets")
            SwatchRow {
                GRADIENTS.forEach { (a, b) ->
                    Swatch(
                        brush = Brush.linearGradient(listOf(Color(a), Color(b))),
                        selected = s.g1 == a && s.g2 == b,
                        shape = RoundedCornerShape(10.dp), width = 40.dp, height = 56.dp,
                    ) { vm.update { copy(g1 = a, g2 = b) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Swatch(Brush.linearGradient(listOf(Color(s.g1), Color(s.g1))), selected = false) { onCustomColor(ColorTarget.G1) }
                Swatch(Brush.linearGradient(listOf(Color(s.g2), Color(s.g2))), selected = false) { onCustomColor(ColorTarget.G2) }
                Text("Tap a dot to pick your own color", color = M.Muted, fontSize = 13.sp)
            }
            SliderRow("Direction", s.angle, 0f..360f, "${s.angle.toInt()}°") { vm.update { copy(angle = it) } }
        }
        BgType.Color -> {
            SwatchRow {
                BG_COLORS.forEach { c ->
                    Swatch(Brush.linearGradient(listOf(Color(c), Color(c))), selected = s.color == c) { vm.update { copy(color = c) } }
                }
                CustomSwatch { onCustomColor(ColorTarget.Background) }
            }
        }
    }
}

@Composable
private fun ColumnScope.TextPanel(vm: MuralViewModel, onCustomColor: (ColorTarget) -> Unit) {
    val s = vm.state
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Switch(
            checked = s.textOn,
            onCheckedChange = { vm.update { copy(textOn = it) } },
            colors = SwitchDefaults.colors(checkedTrackColor = M.Accent, checkedThumbColor = M.AccentInk),
        )
        Text("Show text on wallpaper", color = M.Fg)
    }
    OutlinedTextField(
        value = s.text,
        onValueChange = { vm.update { copy(text = it) } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Your words") },
        placeholder = { Text("A name, a verse, a reminder, anything") },
        supportingText = if (s.text == EXAMPLE_TEXT) { { Text("Example text. Replace it with yours.") } } else null,
        minLines = 2,
        maxLines = 5,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = M.Accent, unfocusedBorderColor = M.Line,
            focusedLabelColor = M.Accent, cursorColor = M.Accent,
        ),
    )
    Hint("Drag the text on the preview to move it.")

    Label("Font")
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Fonts.specs.forEach { f ->
            val family = remember(f.name) { FontFamily(Fonts.get(f.name, false)) }
            val selected = s.font == f.name
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (selected) M.Fg else M.Line, RoundedCornerShape(12.dp))
                    .background(if (selected) M.Surface2 else M.Surface)
                    .clickable { vm.update { copy(font = f.name) } }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text(f.name, fontFamily = family, fontSize = 18.sp, color = M.Fg, maxLines = 1) }
        }
    }

    SliderRow("Size", s.size, 3f..24f, "%.1f".format(s.size)) { vm.update { copy(size = it) } }
    Segmented(listOf("Regular", "Bold"), if (s.bold) 1 else 0) { i -> vm.update { copy(bold = i == 1) } }
    Segmented(listOf("Left", "Center", "Right"), s.align.ordinal) { i -> vm.update { copy(align = Align.entries[i]) } }

    Label("Color")
    SwatchRow {
        TEXT_COLORS.forEach { c ->
            Swatch(Brush.linearGradient(listOf(Color(c), Color(c))), selected = s.textColor == c) { vm.update { copy(textColor = c) } }
        }
        CustomSwatch { onCustomColor(ColorTarget.TextColor) }
    }

    SliderRow("Letter spacing", s.ls, -6f..40f, s.ls.toInt().toString()) { vm.update { copy(ls = it) } }
    SliderRow("Line height", s.lh, 0.8f..2f, "%.2f".format(s.lh)) { vm.update { copy(lh = it) } }

    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Toggle("All caps", s.upper) { vm.update { copy(upper = it) } }
        Toggle("Shadow", s.shadow) { vm.update { copy(shadow = it) } }
        Toggle("Dark band behind", s.box) { vm.update { copy(box = it) } }
    }
    TextButton(onClick = { vm.update { copy(tx = 0.5f, ty = 0.58f) } }) { Text("Center the text", color = M.Fg) }
}

@Composable
private fun ColumnScope.AdjustPanel(vm: MuralViewModel) {
    val s = vm.state
    Hint("Brightness, contrast, color and blur change your photo. Dim and grain work on any background.")
    SliderRow("Brightness", s.bright, 40f..160f, "${s.bright.toInt()}%") { vm.update { copy(bright = it) } }
    SliderRow("Contrast", s.contrast, 40f..160f, "${s.contrast.toInt()}%") { vm.update { copy(contrast = it) } }
    SliderRow("Color", s.sat, 0f..200f, "${s.sat.toInt()}%") { vm.update { copy(sat = it) } }
    SliderRow("Blur", s.blur, 0f..20f, s.blur.toInt().toString()) { vm.update { copy(blur = it) } }
    SliderRow("Dim", s.dim, 0f..85f, "${s.dim.toInt()}%") { vm.update { copy(dim = it) } }
    SliderRow("Grain", s.grain, 0f..60f, "${s.grain.toInt()}%") { vm.update { copy(grain = it) } }
    TextButton(onClick = { vm.update { resetAdjustments() } }) { Text("Reset adjustments", color = M.Fg) }
}

/* ---------------- small pieces ---------------- */

@Composable
private fun Label(text: String, value: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), color = M.Muted, fontSize = 11.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (value != null) Text(value, color = M.Fg, fontSize = 13.sp)
    }
}

@Composable
private fun Hint(text: String) = Text(text, color = M.Muted, fontSize = 12.5.sp)

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Column {
        Label(label, display)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = M.Fg, activeTrackColor = M.Fg, inactiveTrackColor = M.Line),
        )
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(M.Surface2)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) M.Fg else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (on) M.AccentInk else M.Muted, fontSize = 13.5.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun SwatchRow(content: @Composable () -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun Swatch(
    brush: Brush,
    selected: Boolean,
    shape: Shape = CircleShape,
    width: Dp = 32.dp,
    height: Dp = 32.dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(width, height)
            .border(if (selected) 2.dp else 1.dp, if (selected) M.Fg else M.Line, shape)
            .padding(if (selected) 4.dp else 0.dp)
            .clip(shape)
            .background(brush)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun CustomSwatch(onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .border(1.dp, M.Muted, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text("+", color = M.Muted, fontSize = 18.sp) }
}

@Composable
private fun Toggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    FilterChip(
        selected = on,
        onClick = { onChange(!on) },
        label = { Text(label) },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = M.Surface2, selectedLabelColor = M.Fg, labelColor = M.Muted,
        ),
    )
}

@Composable
private fun AccentButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = M.Accent, contentColor = M.AccentInk),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SavedTile(w: SavedWall, onOpen: () -> Unit, onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(confirm) { if (confirm) { delay(2500); confirm = false } }
    Box(
        Modifier
            .aspectRatio(9f / 19.5f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, M.Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
    ) {
        Image(w.thumb, contentDescription = "Saved wallpaper", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Text(
            if (confirm) "Sure?" else "Delete",
            color = if (confirm) M.AccentInk else Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (confirm) M.Danger else Color.Black.copy(alpha = 0.7f))
                .clickable { if (confirm) onDelete() else confirm = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ColorPickerDialog(start: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember(start) { FloatArray(3).also { android.graphics.Color.colorToHSV(start, it) } }
    var h by remember(start) { mutableFloatStateOf(hsv[0]) }
    var sat by remember(start) { mutableFloatStateOf(hsv[1]) }
    var v by remember(start) { mutableFloatStateOf(hsv[2]) }
    val current = android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = M.Surface,
        title = { Text("Pick a color", fontFamily = DisplayFont, fontWeight = FontWeight.Bold, color = M.Fg) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, M.Line, RoundedCornerShape(12.dp))
                        .background(Color(current))
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Brush.horizontalGradient((0..6).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 60f, 1f, 1f))) }))
                )
                SliderRow("Hue", h, 0f..360f, "${h.toInt()}°") { h = it }
                SliderRow("Strength", sat, 0f..1f, "${(sat * 100).toInt()}%") { sat = it }
                SliderRow("Brightness", v, 0f..1f, "${(v * 100).toInt()}%") { v = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onPick(current) }) { Text("Use color", color = M.Accent) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = M.Muted) }
        },
    )
}

