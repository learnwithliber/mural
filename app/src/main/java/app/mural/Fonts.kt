package app.mural

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Typeface

/** The wallpaper fonts, bundled in assets/fonts (all SIL Open Font License). */
object Fonts {
    data class Spec(val name: String, val file: String, val variable: Boolean, val boldFile: String? = null)

    val specs = listOf(
        Spec("Fraunces", "fonts/Fraunces.ttf", variable = true),
        Spec("Playfair Display", "fonts/PlayfairDisplay.ttf", variable = true),
        Spec("DM Serif Display", "fonts/DMSerifDisplay-Regular.ttf", variable = false),
        Spec("Bebas Neue", "fonts/BebasNeue-Regular.ttf", variable = false),
        Spec("Archivo Black", "fonts/ArchivoBlack-Regular.ttf", variable = false),
        Spec("Figtree", "fonts/Figtree.ttf", variable = true),
        Spec("Caveat", "fonts/Caveat.ttf", variable = true),
        Spec("Space Mono", "fonts/SpaceMono-Regular.ttf", variable = false, boldFile = "fonts/SpaceMono-Bold.ttf"),
    )

    private val cache = HashMap<String, Typeface>()
    private var assets: AssetManager? = null

    fun init(context: Context) {
        assets = context.applicationContext.assets
    }

    @Synchronized
    fun get(name: String, bold: Boolean): Typeface = cache.getOrPut("$name/$bold") {
        val spec = specs.firstOrNull { it.name == name } ?: specs.first()
        val am = assets
        val fallback = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        if (am == null) return@getOrPut fallback
        try {
            when {
                spec.variable -> Typeface.Builder(am, spec.file)
                    .setFontVariationSettings("'wght' ${if (bold) 700 else 400}")
                    .build() ?: fallback
                bold && spec.boldFile != null -> Typeface.createFromAsset(am, spec.boldFile)
                else -> Typeface.createFromAsset(am, spec.file)
            }
        } catch (e: Exception) {
            fallback
        }
    }
}
