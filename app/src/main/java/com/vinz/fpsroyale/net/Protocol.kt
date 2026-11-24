package com.vinz.fpsroyale.net

/**
 * Protokol teks super ringan (CSV-ish, 1 baris = 1 pesan) supaya gampang di-debug & gak butuh
 * library serialisasi tambahan. Cocok untuk LAN/VPS kecil dengan jumlah player sedikit (8-16).
 *
 * Format pesan (dipisah '|'):
 *   HELLO|<playerId>|<name>
 *   STATE|<playerId>|<x>|<y>|<z>|<yaw>|<pitch>|<health>|<weaponId>
 *   SHOOT|<playerId>|<ox>|<oy>|<oz>|<dx>|<dy>|<dz>|<weaponId>
 *   HIT|<shooterId>|<victimId>|<damage>
 *   DEATH|<playerId>|<killerId>
 *   LEAVE|<playerId>
 */
sealed class NetMessage {
    data class Hello(val playerId: String, val name: String) : NetMessage()
    data class State(
        val playerId: String, val x: Float, val y: Float, val z: Float,
        val yaw: Float, val pitch: Float, val health: Float, val weaponId: String
    ) : NetMessage()
    data class Shoot(
        val playerId: String, val ox: Float, val oy: Float, val oz: Float,
        val dx: Float, val dy: Float, val dz: Float, val weaponId: String
    ) : NetMessage()
    data class Hit(val shooterId: String, val victimId: String, val damage: Float) : NetMessage()
    data class Death(val playerId: String, val killerId: String) : NetMessage()
    data class Leave(val playerId: String) : NetMessage()

    fun encode(): String = when (this) {
        is Hello -> "HELLO|$playerId|$name"
        is State -> "STATE|$playerId|$x|$y|$z|$yaw|$pitch|$health|$weaponId"
        is Shoot -> "SHOOT|$playerId|$ox|$oy|$oz|$dx|$dy|$dz|$weaponId"
        is Hit -> "HIT|$shooterId|$victimId|$damage"
        is Death -> "DEATH|$playerId|$killerId"
        is Leave -> "LEAVE|$playerId"
    }

    companion object {
        fun decode(line: String): NetMessage? {
            val p = line.trim().split("|")
            if (p.isEmpty()) return null
            return try {
                when (p[0]) {
                    "HELLO" -> Hello(p[1], p[2])
                    "STATE" -> State(p[1], p[2].toFloat(), p[3].toFloat(), p[4].toFloat(),
                        p[5].toFloat(), p[6].toFloat(), p[7].toFloat(), p[8])
                    "SHOOT" -> Shoot(p[1], p[2].toFloat(), p[3].toFloat(), p[4].toFloat(),
                        p[5].toFloat(), p[6].toFloat(), p[7].toFloat(), p[8])
                    "HIT" -> Hit(p[1], p[2], p[3].toFloat())
                    "DEATH" -> Death(p[1], p[2])
                    "LEAVE" -> Leave(p[1])
                    else -> null
                }
            } catch (e: Exception) { null }
        }
    }
}
