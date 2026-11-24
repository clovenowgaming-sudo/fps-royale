package com.vinz.fpsroyale.weapons

enum class FireMode { AUTO, SEMI, PUMP }

/**
 * Data senjata. modelPath menunjuk ke file .glb asli di assets/models/guns/
 * (AK47 & SMG & Shotgun & Sniper diambil dari paket senjata CC/free-use,
 * Pistol dari model generik "pew" — lihat CREDITS.md untuk sumber & lisensi).
 */
data class WeaponData(
    val id: String,
    val displayName: String,
    val modelPath: String,
    val fireMode: FireMode,
    val damage: Float,
    val fireRateRps: Float,   // tembakan per detik
    val magSize: Int,
    val reloadTimeSec: Float,
    val range: Float,         // jarak efektif raycast (meter)
    val spreadDeg: Float,     // akurasi (semakin besar semakin ngacak)
    val sfxShot: String,
    val recoilPitch: Float    // derajat kick kamera per tembakan
) {
    companion object {
        val AK47 = WeaponData(
            id = "ak47", displayName = "AK-47",
            modelPath = "models/guns/ak47.glb",
            fireMode = FireMode.AUTO, damage = 28f, fireRateRps = 10f,
            magSize = 30, reloadTimeSec = 2.3f, range = 120f, spreadDeg = 2.2f,
            sfxShot = "sfx/ak47_shot.wav", recoilPitch = 1.4f
        )
        val PISTOL = WeaponData(
            id = "pistol", displayName = "Pistol",
            modelPath = "models/guns/pistol.glb",
            fireMode = FireMode.SEMI, damage = 22f, fireRateRps = 4f,
            magSize = 12, reloadTimeSec = 1.6f, range = 60f, spreadDeg = 1.0f,
            sfxShot = "sfx/pistol_shot.wav", recoilPitch = 1.0f
        )
        val SMG = WeaponData(
            id = "smg", displayName = "SMG",
            modelPath = "models/guns/smg.glb",
            fireMode = FireMode.AUTO, damage = 16f, fireRateRps = 14f,
            magSize = 35, reloadTimeSec = 2.0f, range = 55f, spreadDeg = 3.2f,
            sfxShot = "sfx/smg_shot.wav", recoilPitch = 1.0f
        )
        val SHOTGUN = WeaponData(
            id = "shotgun", displayName = "Shotgun",
            modelPath = "models/guns/shotgun.glb",
            fireMode = FireMode.PUMP, damage = 14f /* x8 pellet */, fireRateRps = 1.1f,
            magSize = 6, reloadTimeSec = 3.0f, range = 18f, spreadDeg = 9f,
            sfxShot = "sfx/shotgun_shot.wav", recoilPitch = 3.0f
        )
        val SNIPER = WeaponData(
            id = "sniper", displayName = "Sniper",
            modelPath = "models/guns/sniper.glb",
            fireMode = FireMode.SEMI, damage = 95f, fireRateRps = 0.9f,
            magSize = 5, reloadTimeSec = 2.8f, range = 300f, spreadDeg = 0.15f,
            sfxShot = "sfx/sniper_shot.wav", recoilPitch = 4.5f
        )

        val ALL = listOf(AK47, PISTOL, SMG, SHOTGUN, SNIPER)
        val PELLETS_SHOTGUN = 8
    }
}
