package com.smrh.zujaj.render

/**
 * How a live widget is being drawn on this thread: everything (a still picture, e.g. the lock screen),
 * the still part only (under the animation), or the moving layer only (one frame of the animation).
 */
object LiveFx {
    const val ALL = 0
    const val BASE = 1
    const val FX_ONLY = 2
    val mode = ThreadLocal<Int>()
    val phase = ThreadLocal<Float>()
    const val FRAMES = 12

    inline fun <T> with(m: Int, p: Float, block: () -> T): T {
        val om = mode.get(); val op = phase.get()
        mode.set(m); phase.set(p)
        try { return block() } finally { mode.set(om ?: ALL); phase.set(op ?: 0f) }
    }

    /** Milliseconds per frame for the chosen speed. */
    fun interval(speed: Int) = when (speed) { 0 -> 170; 2 -> 75; else -> 110 }
}
