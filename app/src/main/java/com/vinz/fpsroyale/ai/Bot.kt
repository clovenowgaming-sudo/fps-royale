package com.vinz.fpsroyale.ai

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.vinz.fpsroyale.weapons.WeaponData
import com.vinz.fpsroyale.weapons.WeaponManager
import kotlin.random.Random

enum class BotState { PATROL, CHASE, ATTACK, FLEE_ZONE, DEAD }

/**
 * Satu bot musuh. Sampai 100 instance bisa aktif sekaligus di mode battle royale offline
 * (jumlah yang benar-benar digambar detail dibatasi oleh GraphicsQuality.maxVisibleBots,
 * sisanya tetap disimulasikan tapi dirender sebagai model sederhana / di-skip kalau jauh).
 */
class Bot(
    val id: Int,
    val modelVariant: String, // "bot_rogue.glb" / "bot_barbarian.glb" / "bot_mage.glb" dst, untuk variasi visual
    startPos: Vector3
) {
    val position = Vector3(startPos)
    var yaw = 0f
    var health = 100f
    var state = BotState.PATROL
    var isAlive = true
        private set

    val weapons = WeaponManager(listOf(randomBotWeapon()))

    private var patrolTarget = Vector3(startPos)
    private var stateTimer = 0f
    private var reactionDelay = Random.nextFloat() * 0.4f // biar nembaknya ga robotik instan

    private val moveSpeed = 3.0f
    private val sightRange = 45f
    private val attackRange = 35f
    private val fieldOfViewDeg = 100f

    companion object {
        fun randomBotWeapon(): WeaponData =
            WeaponData.ALL[Random.nextInt(WeaponData.ALL.size)]
    }

    /**
     * Update AI satu bot. playerPos/playerAlive untuk deteksi target, zoneCenter+zoneRadius
     * untuk logic battle royale (lari masuk zone kalau di luar), obstacleLineOfSight = lambda
     * cek LOS sederhana (disediakan game layer, bisa pakai raycast dunia atau selalu true kalau
     * world masih flat/terbuka).
     */
    fun update(
        delta: Float,
        playerPos: Vector3,
        playerAlive: Boolean,
        zoneCenter: Vector3,
        zoneRadius: Float,
        hasLineOfSight: (Vector3, Vector3) -> Boolean,
        onShoot: (Bot, Vector3, Vector3) -> Unit // (shooter, origin, dir)
    ) {
        if (!isAlive) return
        stateTimer += delta

        val distToZoneCenter = Vector3(position.x - zoneCenter.x, 0f, position.z - zoneCenter.z).len()
        if (distToZoneCenter > zoneRadius) {
            state = BotState.FLEE_ZONE
        } else if (state == BotState.FLEE_ZONE) {
            state = BotState.PATROL
        }

        val distToPlayer = if (playerAlive) position.dst(playerPos) else Float.MAX_VALUE
        val canSeePlayer = playerAlive && distToPlayer < sightRange && hasLineOfSight(position, playerPos)

        when (state) {
            BotState.FLEE_ZONE -> {
                moveToward(zoneCenter, delta)
            }
            BotState.PATROL -> {
                if (canSeePlayer) {
                    state = BotState.CHASE
                } else {
                    if (position.dst(patrolTarget) < 1.5f || stateTimer > 8f) {
                        patrolTarget = Vector3(
                            position.x + Random.nextFloat() * 20f - 10f,
                            position.y,
                            position.z + Random.nextFloat() * 20f - 10f
                        )
                        stateTimer = 0f
                    }
                    moveToward(patrolTarget, delta)
                }
            }
            BotState.CHASE -> {
                if (!canSeePlayer) {
                    state = BotState.PATROL
                } else if (distToPlayer <= attackRange) {
                    state = BotState.ATTACK
                } else {
                    moveToward(playerPos, delta)
                }
            }
            BotState.ATTACK -> {
                if (!canSeePlayer) {
                    state = BotState.PATROL
                } else if (distToPlayer > attackRange * 1.2f) {
                    state = BotState.CHASE
                } else {
                    faceToward(playerPos)
                    reactionDelay -= delta
                    if (reactionDelay <= 0f) {
                        val dir = Vector3(playerPos).sub(position).nor()
                        // sedikit spread biar bot ga selalu headshot
                        dir.x += MathUtils.random(-0.03f, 0.03f)
                        dir.y += MathUtils.random(-0.02f, 0.02f)
                        dir.nor()
                        val fired = weapons.update(delta, triggerHeld = true, reloadPressed = false, isMoving = false)
                        if (fired) onShoot(this, Vector3(position).add(0f, 1.6f, 0f), dir)
                        if (weapons.currentMagAmmo() <= 0 && !weapons.isReloading) {
                            weapons.update(delta, triggerHeld = false, reloadPressed = true, isMoving = false)
                        }
                    }
                }
            }
            BotState.DEAD -> { /* no-op */ }
        }

        if (state != BotState.ATTACK && weapons.isReloading.not() && weapons.currentMagAmmo() < weapons.current.magSize) {
            weapons.update(delta, triggerHeld = false, reloadPressed = true, isMoving = false)
        }
    }

    private fun moveToward(target: Vector3, delta: Float) {
        val dir = Vector3(target.x - position.x, 0f, target.z - position.z)
        if (dir.len2() < 0.0001f) return
        dir.nor()
        position.x += dir.x * moveSpeed * delta
        position.z += dir.z * moveSpeed * delta
        faceToward(target)
    }

    private fun faceToward(target: Vector3) {
        yaw = MathUtils.atan2(target.x - position.x, target.z - position.z) * MathUtils.radDeg
    }

    fun takeDamage(amount: Float): Boolean {
        if (!isAlive) return false
        health -= amount
        if (health <= 0f) {
            health = 0f
            isAlive = false
            state = BotState.DEAD
            return true // mati di hit ini
        }
        return false
    }
}
