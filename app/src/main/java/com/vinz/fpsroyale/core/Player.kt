package com.vinz.fpsroyale.core

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.Vector3
import com.vinz.fpsroyale.weapons.WeaponManager

/**
 * Representasi player dalam mode First Person (FPP murni — tidak ada mode third person).
 * Gerakan pakai sistem sederhana: kecepatan horizontal dari joystick kiri,
 * look dari drag joystick kanan / layar, gravity manual, ground-check via raycast dunia.
 */
class Player(val camera: PerspectiveCamera) {

    val position = Vector3(0f, 1.75f, 0f) // tinggi mata ~1.75m
    val velocity = Vector3()
    var yaw = 0f
    var pitch = 0f

    var health = 100f
    var isAlive = true
    var isCrouching = false
    var isAiming = false
    var isGrounded = true

    val weapons = WeaponManager()

    private val walkSpeed = 4.2f
    private val crouchSpeed = 2.0f
    private val aimSpeedMultiplier = 0.55f
    private val jumpVelocity = 5.2f
    private val gravity = -14f

    var moveX = 0f // -1..1 strafe
    var moveZ = 0f // -1..1 maju/mundur
    var wantsJump = false

    fun lookDelta(dx: Float, dy: Float, sensitivity: Float, invertY: Boolean) {
        yaw -= dx * sensitivity
        val dyS = if (invertY) -dy else dy
        pitch = (pitch - dyS * sensitivity).coerceIn(-89f, 89f)
    }

    fun update(delta: Float, groundHeight: Float) {
        if (!isAlive) return

        val speed = when {
            isAiming -> walkSpeed * aimSpeedMultiplier
            isCrouching -> crouchSpeed
            else -> walkSpeed
        }

        val yawRad = Math.toRadians(yaw.toDouble())
        val forwardX = -Math.sin(yawRad).toFloat()
        val forwardZ = -Math.cos(yawRad).toFloat()
        val rightX = Math.cos(yawRad).toFloat()
        val rightZ = -Math.sin(yawRad).toFloat()

        val moveLenSq = moveX * moveX + moveZ * moveZ
        val normalize = if (moveLenSq > 1f) 1f / Math.sqrt(moveLenSq.toDouble()).toFloat() else 1f

        velocity.x = (forwardX * moveZ + rightX * moveX) * normalize * speed
        velocity.z = (forwardZ * moveZ + rightZ * moveX) * normalize * speed

        if (isGrounded && wantsJump) {
            velocity.y = jumpVelocity
            isGrounded = false
        }
        wantsJump = false

        velocity.y += gravity * delta
        position.x += velocity.x * delta
        position.z += velocity.z * delta
        position.y += velocity.y * delta

        val standHeight = if (isCrouching) 1.1f else 1.75f
        val floorY = groundHeight + standHeight
        if (position.y <= floorY) {
            position.y = floorY
            velocity.y = 0f
            isGrounded = true
        }

        camera.position.set(position)
        camera.direction.set(forwardX, (-Math.sin(Math.toRadians(pitch.toDouble()))).toFloat(), forwardZ).nor()
        camera.up.set(0f, 1f, 0f)
        camera.update()
    }

    fun takeDamage(amount: Float) {
        if (!isAlive) return
        health -= amount
        if (health <= 0f) {
            health = 0f
            isAlive = false
        }
    }

    fun respawn(spawnPos: Vector3) {
        health = 100f
        isAlive = true
        position.set(spawnPos)
        velocity.setZero()
    }
}
