package com.smrh.zujaj.wall

/**
 * Ready-made wallpaper looks. Applying one only sets the photo look and normal layers, so every
 * part stays editable afterwards (move, resize, colour, glass, delete…).
 */
object WallTemplates {
    class Template(
        val id: String,
        val en: String,
        val ar: String,
        /** true = the clock goes behind the subject (needs ✦ detection) */
        val depth: Boolean = false,
        val filter: Int = -1,
        val topShade: Int = -1,
        /** colours are picked from the photo when true */
        val autoColor: Boolean = true,
        /** always light text (tinted from the photo) */
        val light: Boolean = false,
        /** one soft tint taken from the photo (iPhone style) instead of a gradient */
        val tint: Boolean = false,
        val layers: () -> List<WallLayer>,
    )

    private var n = 0L
    private fun id() = System.nanoTime() + (n++)

    val all = listOf(
        Template("depth", "Depth", "عمق", depth = true, tint = true) {
            listOf(
                WallLayer(id = id(), kind = "clock_tall", cx = 0.5f, cy = 0.26f, w = 0.84f, behind = true, fit = true, hide = 0.3f),
                WallLayer(id = id(), kind = "date_line", cx = 0.5f, cy = 0.1f, w = 0.6f, stick = true),
            )
        },
        Template("liquid", "Liquid glass", "زجاج سائل", topShade = 0) {
            listOf(
                WallLayer(id = id(), kind = "clock_tall", cx = 0.5f, cy = 0.25f, w = 0.78f, glassFill = true, shadow = false),
                WallLayer(id = id(), kind = "date_greg", cx = 0.5f, cy = 0.085f, w = 0.46f, glass = true),
            )
        },
        Template("minimal", "Minimal", "بسيط", topShade = 40, light = true) {
            listOf(
                WallLayer(id = id(), kind = "clock_thin", cx = 0.5f, cy = 0.215f, w = 0.42f),
                WallLayer(id = id(), kind = "date_greg", cx = 0.5f, cy = 0.065f, w = 0.4f),
            )
        },
        Template("arabic", "Arabic", "عربي", topShade = 40, light = true) {
            listOf(
                WallLayer(id = id(), kind = "hijri_full", cx = 0.5f, cy = 0.055f, w = 0.58f),
                WallLayer(id = id(), kind = "day_calli", cx = 0.5f, cy = 0.135f, w = 0.56f),
                WallLayer(id = id(), kind = "clock_thin", cx = 0.5f, cy = 0.25f, w = 0.24f),
            )
        },
        Template("bottom", "Glass card", "كارت زجاج") {
            listOf(
                WallLayer(id = id(), kind = "clock_tall", cx = 0.5f, cy = 0.72f, w = 0.5f, glass = true),
                WallLayer(id = id(), kind = "date_pair", cx = 0.5f, cy = 0.86f, w = 0.62f, glass = true),
            )
        },
        Template("prayer", "Prayer", "الصلاة", topShade = 40, light = true) {
            listOf(
                WallLayer(id = id(), kind = "clock_stack", cx = 0.5f, cy = 0.2f, w = 0.26f),
                WallLayer(id = id(), kind = "prayer_next", cx = 0.5f, cy = 0.075f, w = 0.6f),
                WallLayer(id = id(), kind = "prayer_row", cx = 0.5f, cy = 0.84f, w = 0.86f, glass = true),
            )
        },
        Template("cinema", "Cinema", "سينما", filter = 4, topShade = 25, tint = true) {
            listOf(
                WallLayer(id = id(), kind = "clock_tall", cx = 0.5f, cy = 0.27f, w = 0.84f, behind = true, fit = true, hide = 0.4f),
                WallLayer(id = id(), kind = "dual_full", cx = 0.5f, cy = 0.085f, w = 0.62f),
            )
        },
        Template("noir", "Noir", "نوار", depth = true, filter = 7, autoColor = false) {
            listOf(
                WallLayer(id = id(), kind = "clock_tall", cx = 0.5f, cy = 0.27f, w = 0.88f, behind = true, fit = true, hide = 0.25f, color = 0xFFFFFFFF.toInt()),
                WallLayer(id = id(), kind = "date_line", cx = 0.5f, cy = 0.1f, w = 0.6f, stick = true, color = 0xFFFFFFFF.toInt()),
            )
        },
    )

    /**
     * The design with [t] applied to it. [photo] is used to pick readable colours; layers that need a
     * subject cutout are only put behind when [hasMask].
     */
    fun apply(t: Template, d: WallDesign, photo: android.graphics.Bitmap?, W: Int, H: Int, hasMask: Boolean): WallDesign {
        var base = d.copy(
            filter = if (t.filter >= 0) t.filter else 0,
            topShade = if (t.topShade >= 0) t.topShade else 0,
        )
        val layers = t.layers().map { l ->
            var x = l
            if (t.autoColor && photo != null) {
                val (a, b) = runCatching { WallRender.autoColors(photo, x, W, H, if (t.light) maxOf(base.dim, 65) else base.dim, base) }.getOrNull() ?: (x.color to 0)
                // glass shapes take their look from the photo itself: keep them white
                x = if (x.glassFill) x else x.copy(color = a, color2 = if (t.tint) 0 else b, gDir = 0)
            }
            x
        }
        base = base.copy(layers = layers)
        return base
    }
}
