package com.vinz.fpsroyale.core

/** Preset kualitas grafik yang bisa dipilih user dari menu Settings. */
enum class GraphicsQuality(
    val label: String,
    val renderDistance: Float,   // jarak render dunia (meter)
    val botRenderDistance: Float,// jarak maksimum bot dirender (buat hemat saat 100 bot aktif)
    val shadows: Boolean,
    val maxVisibleBots: Int,     // batas bot yang digambar detail penuh per frame
    val resolutionScale: Float,  // render di resolusi lebih rendah lalu di-upscale (HP low-end)
    val particles: Boolean
) {
    LOW("Low", renderDistance = 120f, botRenderDistance = 60f, shadows = false,
        maxVisibleBots = 15, resolutionScale = 0.65f, particles = false),
    MEDIUM("Medium", renderDistance = 220f, botRenderDistance = 110f, shadows = false,
        maxVisibleBots = 30, resolutionScale = 0.85f, particles = true),
    HIGH("High", renderDistance = 400f, botRenderDistance = 200f, shadows = true,
        maxVisibleBots = 60, resolutionScale = 1.0f, particles = true);

    companion object {
        fun next(current: GraphicsQuality): GraphicsQuality {
            val values = entries
            return values[(current.ordinal + 1) % values.size]
        }
    }
}

/** Setting global yang dipakai di seluruh game, disimpan ke Preferences biar persist. */
object GameSettings {
    var quality: GraphicsQuality = GraphicsQuality.MEDIUM
    var musicVolume: Float = 0.6f
    var sfxVolume: Float = 1.0f
    var sensitivity: Float = 1.0f
    var invertY: Boolean = false
    var botCount: Int = 99 // 99 bot + 1 player = 100 total di battle royale offline
    var fieldOfView: Float = 75f
}
