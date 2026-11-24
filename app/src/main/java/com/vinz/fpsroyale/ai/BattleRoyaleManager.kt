package com.vinz.fpsroyale.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.vinz.fpsroyale.core.GameSettings

enum class MatchPhase { WAITING, RUNNING, PLAYER_WON, PLAYER_DIED }

/**
 * Mengatur mode Battle Royale OFFLINE: spawn sampai 100 bot (controlled by GameSettings.botCount),
 * zona yang menyusut seiring waktu (mirip battle royale pada umumnya), dan penghitungan
 * berapa pemain yang masih hidup ("X/100 tersisa").
 */
class BattleRoyaleManager(
    private val mapHalfSize: Float = 150f // dunia dari -150..150 di X/Z
) {
    val bots = mutableListOf<Bot>()
    var phase = MatchPhase.WAITING
        private set

    val zoneCenter = Vector3(0f, 0f, 0f)
    var zoneRadius = mapHalfSize
        private set
    private var targetZoneRadius = mapHalfSize
    private var shrinkTimer = 0f
    private var shrinkPhaseIndex = 0
    private val shrinkIntervalSec = 35f   // tiap 35 detik zona mengecil
    private val shrinkDurationSec = 15f   // butuh 15 detik buat benar2 mengecil ke target
    private var shrinking = false
    private var shrinkElapsed = 0f
    private var startRadiusForShrink = mapHalfSize

    var botsAliveCount = 0
        private set
    var matchTimeSec = 0f
        private set

    private val botVariants = listOf("bot_rogue.glb", "bot_barbarian.glb", "bot_mage.glb")

    fun startMatch() {
        bots.clear()
        val count = GameSettings.botCount.coerceIn(1, 99)
        repeat(count) { i ->
            val angle = MathUtils.random(0f, MathUtils.PI2)
            val dist = MathUtils.random(10f, mapHalfSize * 0.9f)
            val pos = Vector3(MathUtils.cos(angle) * dist, 0f, MathUtils.sin(angle) * dist)
            bots.add(Bot(i, botVariants[i % botVariants.size], pos))
        }
        botsAliveCount = count
        phase = MatchPhase.RUNNING
        matchTimeSec = 0f
        zoneRadius = mapHalfSize
        targetZoneRadius = mapHalfSize
        shrinkTimer = 0f
        shrinkPhaseIndex = 0
        shrinking = false
    }

    /** Dipanggil tiap frame dari game loop utama. */
    fun update(
        delta: Float,
        playerPos: Vector3,
        playerAlive: Boolean,
        hasLineOfSight: (Vector3, Vector3) -> Boolean,
        onBotShoot: (Bot, Vector3, Vector3) -> Unit
    ) {
        if (phase != MatchPhase.RUNNING) return
        matchTimeSec += delta
        updateZone(delta)

        var alive = 0
        for (bot in bots) {
            if (bot.isAlive) {
                bot.update(delta, playerPos, playerAlive, zoneCenter, zoneRadius, hasLineOfSight, onBotShoot)
                // damage zona kalau bot di luar lama (disederhanakan: dicek di luar manager pakai zoneRadius)
                alive++
            }
        }
        botsAliveCount = alive

        if (!playerAlive) {
            phase = MatchPhase.PLAYER_DIED
        } else if (alive == 0) {
            phase = MatchPhase.PLAYER_WON
        }
    }

    /** Return true kalau posisi dunia ini di luar zona aman (buat damage-over-time ke player). */
    fun isOutsideZone(pos: Vector3): Boolean {
        val d = Vector3(pos.x - zoneCenter.x, 0f, pos.z - zoneCenter.z).len()
        return d > zoneRadius
    }

    private fun updateZone(delta: Float) {
        if (!shrinking) {
            shrinkTimer += delta
            if (shrinkTimer >= shrinkIntervalSec) {
                shrinkTimer = 0f
                shrinkPhaseIndex++
                startRadiusForShrink = zoneRadius
                targetZoneRadius = (mapHalfSize * Math.pow(0.65, shrinkPhaseIndex.toDouble())).toFloat()
                    .coerceAtLeast(6f)
                shrinking = true
                shrinkElapsed = 0f
            }
        } else {
            shrinkElapsed += delta
            val t = (shrinkElapsed / shrinkDurationSec).coerceIn(0f, 1f)
            zoneRadius = MathUtils.lerp(startRadiusForShrink, targetZoneRadius, t)
            if (t >= 1f) shrinking = false
        }
    }

    fun playersRemaining(): Int = botsAliveCount + 1 // +1 = player (kalau masih hidup; dicek di luar)
}
