package app.mural

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws a wallpaper onto any Canvas. The same code paints the on-screen preview
 * and the full-resolution image, so what you see is exactly what you get.
 * One instance per thread: it caches the blurred photo.
 */
class Renderer {

    private val noise: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also { bmp ->
        val rnd = Random(7)
        val px = IntArray(256 * 256) { val v = rnd.nextInt(256); Color.rgb(v, v, v) }
        bmp.setPixels(px, 0, 256, 0, 0, 256, 256)
    }
    private val grainShader = BitmapShader(noise, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val grainMatrix = Matrix()
    private val grainPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY); shader = grainShader }
    private val photoPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(115, 0, 0, 0) }
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var blurSource: Bitmap? = null
    private var blurLevel = -1f
    private var blurred: Bitmap? = null

    fun render(width: Int, height: Int, s: WallState, photo: Bitmap?): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it), width.toFloat(), height.toFloat(), s, photo)
        }

    fun draw(c: Canvas, w: Float, h: Float, s: WallState, photo: Bitmap?) {
        if (w <= 0f || h <= 0f) return
        drawBackground(c, w, h, s, photo)
        if (s.dim > 0f) c.drawColor(Color.argb((s.dim / 100f * 255).toInt(), 0, 0, 0))
        if (s.grain > 0f) {
            grainMatrix.setScale(w / 1080f, w / 1080f)
            grainShader.setLocalMatrix(grainMatrix)
            grainPaint.alpha = (s.grain / 100f * 0.55f * 255).toInt().coerceIn(0, 255)
            c.drawRect(0f, 0f, w, h, grainPaint)
        }
        drawText(c, w, h, s)
    }

    private fun drawBackground(c: Canvas, w: Float, h: Float, s: WallState, photo: Bitmap?) {
        when {
            s.bgType == BgType.Photo && photo != null -> {
                c.drawColor(Color.BLACK)
                val src = blurredOf(photo, s.blur)
                val iw = photo.width.toFloat()
                val ih = photo.height.toFloat()
                // A blurred photo gets a touch of extra scale so its soft edges stay off-screen.
                val scale = max(w / iw, h / ih) * s.zoom * (if (s.blur > 0f) 1f + s.blur * 0.006f else 1f)
                val dw = iw * scale
                val dh = ih * scale
                val mx = max(0f, (dw - w) / 2f)
                val my = max(0f, (dh - h) / 2f)
                val left = (w - dw) / 2f + (s.ox * w).coerceIn(-mx, mx)
                val top = (h - dh) / 2f + (s.oy * h).coerceIn(-my, my)
                rect.set(left, top, left + dw, top + dh)
                photoPaint.colorFilter = ColorMatrixColorFilter(colorMatrix(s))
                c.drawBitmap(src, null, rect, photoPaint)
            }
            s.bgType == BgType.Gradient -> {
                val a = Math.toRadians((s.angle - 90f).toDouble())
                val cs = cos(a).toFloat()
                val sn = sin(a).toFloat()
                val len = (abs(w * cs) + abs(h * sn)) / 2f
                fillPaint.shader = LinearGradient(
                    w / 2f - cs * len, h / 2f - sn * len, w / 2f + cs * len, h / 2f + sn * len,
                    s.g1, s.g2, Shader.TileMode.CLAMP
                )
                c.drawRect(0f, 0f, w, h, fillPaint)
                fillPaint.shader = null
            }
            s.bgType == BgType.Color -> c.drawColor(s.color)
            else -> c.drawColor(0xFF0F0F0E.toInt()) // photo chosen but not picked yet
        }
    }

    private fun colorMatrix(s: WallState): ColorMatrix {
        val b = s.bright / 100f
        val ct = s.contrast / 100f
        val k = b * ct
        val off = 128f * (1f - ct)
        return ColorMatrix(
            floatArrayOf(
                k, 0f, 0f, 0f, off,
                0f, k, 0f, 0f, off,
                0f, 0f, k, 0f, off,
                0f, 0f, 0f, 1f, 0f,
            )
        ).apply { postConcat(ColorMatrix().apply { setSaturation(s.sat / 100f) }) }
    }

    /** Cheap, smooth blur: shrink the photo in halving steps, then let bilinear filtering stretch it back. */
    private fun blurredOf(photo: Bitmap, level: Float): Bitmap {
        if (level <= 0f) return photo
        if (photo === blurSource && level == blurLevel) blurred?.let { return it }
        val factor = 1f + level * 1.5f
        val tw = max(8, (photo.width / factor).toInt())
        val th = max(8, (photo.height / factor).toInt())
        var b = photo
        while (b.width / 2 > tw && b.height / 2 > th) {
            b = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
        }
        b = Bitmap.createScaledBitmap(b, tw, th, true)
        blurSource = photo
        blurLevel = level
        blurred = b
        return b
    }

    private fun drawText(c: Canvas, w: Float, h: Float, s: WallState) {
        if (!s.textOn || s.text.isBlank()) return
        val px = s.size / 100f * w
        textPaint.apply {
            typeface = Fonts.get(s.font, s.bold)
            textSize = px
            color = s.textColor
            letterSpacing = s.ls / 100f
            if (s.shadow) setShadowLayer(px * 0.3f, 0f, px * 0.04f, Color.argb(115, 0, 0, 0)) else clearShadowLayer()
        }
        val str = if (s.upper) s.text.uppercase() else s.text
        val alignment = when (s.align) {
            Align.Left -> Layout.Alignment.ALIGN_NORMAL
            Align.Center -> Layout.Alignment.ALIGN_CENTER
            Align.Right -> Layout.Alignment.ALIGN_OPPOSITE
        }
        fun build(width: Int): StaticLayout =
            StaticLayout.Builder.obtain(str, 0, str.length, textPaint, width)
                .setAlignment(alignment)
                .setLineSpacing(0f, s.lh)
                .setIncludePad(false)
                .build()

        val maxW = (w * 0.84f).toInt().coerceAtLeast(1)
        var layout = build(maxW)
        var widest = 0f
        for (i in 0 until layout.lineCount) widest = max(widest, layout.getLineWidth(i))
        val tight = ceil(widest).toInt() + 2
        if (tight < maxW) layout = build(tight)

        val bw = layout.width.toFloat()
        val bh = layout.height.toFloat()
        val left = s.tx * w - bw / 2f
        val top = s.ty * h - bh / 2f
        if (s.box) {
            val pad = px * 0.45f
            c.drawRoundRect(left - pad, top - pad * 0.7f, left + bw + pad, top + bh + pad * 0.7f, px * 0.3f, px * 0.3f, boxPaint)
        }
        c.save()
        c.translate(left, top)
        layout.draw(c)
        c.restore()
    }

    companion object {
        /** Keeps a zoomed photo covering the whole screen. [aspect] is screen width / height. */
        fun clampPhoto(s: WallState, iw: Int, ih: Int, aspect: Float): WallState {
            val w = aspect
            val h = 1f
            val scale = max(w / iw, h / ih) * s.zoom
            val mx = max(0f, (iw * scale - w) / 2f) / w
            val my = max(0f, (ih * scale - h) / 2f) / h
            return s.copy(ox = s.ox.coerceIn(-mx, mx), oy = s.oy.coerceIn(-my, my))
        }
    }
}
