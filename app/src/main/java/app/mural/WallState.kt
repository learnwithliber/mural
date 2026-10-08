package app.mural

import org.json.JSONObject

enum class BgType { Photo, Gradient, Color }
enum class Align { Left, Center, Right }

const val EXAMPLE_TEXT = "Make today\ncount."

/** Everything that describes one wallpaper. Positions are fractions of the screen (0..1). */
data class WallState(
    // background
    val bgType: BgType = BgType.Gradient,
    val color: Int = 0xFF0E1A1F.toInt(),
    val g1: Int = 0xFF1F3A3D.toInt(),
    val g2: Int = 0xFF050807.toInt(),
    val angle: Float = 170f,
    // photo framing
    val zoom: Float = 1f,
    val ox: Float = 0f,
    val oy: Float = 0f,
    // adjustments
    val bright: Float = 100f,
    val contrast: Float = 100f,
    val sat: Float = 100f,
    val blur: Float = 0f,
    val dim: Float = 0f,
    val grain: Float = 12f,
    // text
    val textOn: Boolean = true,
    val text: String = EXAMPLE_TEXT,
    val font: String = "Fraunces",
    val bold: Boolean = true,
    val size: Float = 11f,          // percent of screen width
    val textColor: Int = 0xFFF2ECE0.toInt(),
    val align: Align = Align.Center,
    val tx: Float = 0.5f,
    val ty: Float = 0.58f,
    val ls: Float = 0f,             // letter spacing, hundredths of an em
    val lh: Float = 1.05f,
    val upper: Boolean = false,
    val shadow: Boolean = true,
    val box: Boolean = false,
) {
    fun resetAdjustments() = copy(bright = 100f, contrast = 100f, sat = 100f, blur = 0f, dim = 0f, grain = 0f)

    fun toJson(): String = JSONObject().apply {
        put("bgType", bgType.name); put("color", color); put("g1", g1); put("g2", g2); put("angle", angle.toDouble())
        put("zoom", zoom.toDouble()); put("ox", ox.toDouble()); put("oy", oy.toDouble())
        put("bright", bright.toDouble()); put("contrast", contrast.toDouble()); put("sat", sat.toDouble())
        put("blur", blur.toDouble()); put("dim", dim.toDouble()); put("grain", grain.toDouble())
        put("textOn", textOn); put("text", text); put("font", font); put("bold", bold); put("size", size.toDouble())
        put("textColor", textColor); put("align", align.name); put("tx", tx.toDouble()); put("ty", ty.toDouble())
        put("ls", ls.toDouble()); put("lh", lh.toDouble()); put("upper", upper); put("shadow", shadow); put("box", box)
    }.toString()

    companion object {
        fun fromJson(json: String): WallState {
            val o = JSONObject(json)
            val d = WallState()
            fun f(k: String, def: Float) = o.optDouble(k, def.toDouble()).toFloat()
            return WallState(
                bgType = runCatching { BgType.valueOf(o.getString("bgType")) }.getOrDefault(d.bgType),
                color = o.optInt("color", d.color), g1 = o.optInt("g1", d.g1), g2 = o.optInt("g2", d.g2),
                angle = f("angle", d.angle), zoom = f("zoom", d.zoom), ox = f("ox", d.ox), oy = f("oy", d.oy),
                bright = f("bright", d.bright), contrast = f("contrast", d.contrast), sat = f("sat", d.sat),
                blur = f("blur", d.blur), dim = f("dim", d.dim), grain = f("grain", d.grain),
                textOn = o.optBoolean("textOn", d.textOn), text = o.optString("text", d.text),
                font = o.optString("font", d.font), bold = o.optBoolean("bold", d.bold), size = f("size", d.size),
                textColor = o.optInt("textColor", d.textColor),
                align = runCatching { Align.valueOf(o.getString("align")) }.getOrDefault(d.align),
                tx = f("tx", d.tx), ty = f("ty", d.ty), ls = f("ls", d.ls), lh = f("lh", d.lh),
                upper = o.optBoolean("upper", d.upper), shadow = o.optBoolean("shadow", d.shadow),
                box = o.optBoolean("box", d.box),
            )
        }
    }
}
