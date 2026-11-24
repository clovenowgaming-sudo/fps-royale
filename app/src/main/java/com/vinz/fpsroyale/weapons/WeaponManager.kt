package com.vinz.fpsroyale.weapons

/** Status animasi lengan/senjata saat ini — dipakai untuk pilih klip animasi di model viewmodel. */
enum class WeaponAnimState { IDLE, WALK, AIM, FIRE, RELOAD }

/**
 * Mengelola inventori 2 slot senjata milik player/bot: ammo di magazine, ammo cadangan,
 * reload timer, fire-rate cooldown, dan state untuk animasi.
 */
class WeaponManager(
    startWeapons: List<WeaponData> = listOf(WeaponData.AK47, WeaponData.PISTOL)
) {
    private val slots: MutableList<WeaponData> = startWeapons.toMutableList()
    private val magAmmo = HashMap<String, Int>().apply { slots.forEach { put(it.id, it.magSize) } }
    private val reserveAmmo = HashMap<String, Int>().apply { slots.forEach { put(it.id, it.magSize * 3) } }

    var currentIndex = 0
        private set
    val current: WeaponData get() = slots[currentIndex]

    private var cooldown = 0f
    private var reloadTimeLeft = 0f
    var isReloading = false
        private set
    var animState: WeaponAnimState = WeaponAnimState.IDLE
        private set
    var isAiming = false

    fun currentMagAmmo() = magAmmo[current.id] ?: 0
    fun currentReserveAmmo() = reserveAmmo[current.id] ?: 0

    fun switchWeapon(index: Int) {
        if (index == currentIndex || index !in slots.indices || isReloading) return
        currentIndex = index
        cooldown = 0.15f // jeda kecil biar nggak insta-tembak abis ganti senjata
    }

    fun cycleNext() = switchWeapon((currentIndex + 1) % slots.size)

    fun pickUpWeapon(weapon: WeaponData) {
        // kalau slot sudah ada senjata tipe sama, cuma tambah reserve ammo
        val existingIdx = slots.indexOfFirst { it.id == weapon.id }
        if (existingIdx >= 0) {
            reserveAmmo[weapon.id] = (reserveAmmo[weapon.id] ?: 0) + weapon.magSize * 2
            return
        }
        if (slots.size < 2) {
            slots.add(weapon)
            magAmmo[weapon.id] = weapon.magSize
            reserveAmmo[weapon.id] = weapon.magSize * 3
        } else {
            // ganti senjata yang sedang tidak aktif
            val replaceIdx = if (currentIndex == 0) 1 else 0
            slots[replaceIdx] = weapon
            magAmmo[weapon.id] = weapon.magSize
            reserveAmmo[weapon.id] = weapon.magSize * 3
        }
    }

    /** Dipanggil tiap frame. Return true kalau sebuah tembakan beneran terjadi frame ini. */
    fun update(delta: Float, triggerHeld: Boolean, reloadPressed: Boolean, isMoving: Boolean): Boolean {
        cooldown = (cooldown - delta).coerceAtLeast(0f)

        if (isReloading) {
            reloadTimeLeft -= delta
            animState = WeaponAnimState.RELOAD
            if (reloadTimeLeft <= 0f) {
                finishReload()
            }
            return false
        }

        if (reloadPressed && currentMagAmmo() < current.magSize && currentReserveAmmo() > 0) {
            startReload()
            return false
        }

        var fired = false
        val canFire = cooldown <= 0f && currentMagAmmo() > 0
        val wantsFire = if (current.fireMode == FireMode.AUTO) triggerHeld else triggerHeld // semi ditangani di layer input (edge-trigger)
        if (wantsFire && canFire) {
            magAmmo[current.id] = currentMagAmmo() - 1
            cooldown = 1f / current.fireRateRps
            fired = true
        } else if (triggerHeld && currentMagAmmo() <= 0) {
            fired = false // harusnya main empty_click.wav di layer atas
        }

        animState = when {
            fired -> WeaponAnimState.FIRE
            isAiming -> WeaponAnimState.AIM
            isMoving -> WeaponAnimState.WALK
            else -> WeaponAnimState.IDLE
        }
        return fired
    }

    private fun startReload() {
        isReloading = true
        reloadTimeLeft = current.reloadTimeSec
    }

    private fun finishReload() {
        val need = current.magSize - currentMagAmmo()
        val take = minOf(need, currentReserveAmmo())
        magAmmo[current.id] = currentMagAmmo() + take
        reserveAmmo[current.id] = currentReserveAmmo() - take
        isReloading = false
    }
}
