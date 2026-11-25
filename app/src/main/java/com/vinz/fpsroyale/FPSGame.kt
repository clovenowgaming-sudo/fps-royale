package com.vinz.fpsroyale

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import com.badlogic.gdx.audio.Sound
import com.vinz.fpsroyale.core.GameSettings
import com.vinz.fpsroyale.core.GraphicsQuality
import com.vinz.fpsroyale.core.Player
import com.vinz.fpsroyale.ai.BattleRoyaleManager
import com.vinz.fpsroyale.ai.Bot
import com.vinz.fpsroyale.ai.MatchPhase
import com.vinz.fpsroyale.net.MultiplayerClient
import com.vinz.fpsroyale.net.NetMessage
import com.vinz.fpsroyale.weapons.WeaponData
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider
import net.mgsx.gltf.scene3d.shaders.PBRDepthShaderProvider
import kotlin.random.Random

/** Mode yang bisa dipilih dari menu utama. */
enum class GameMode { OFFLINE_BATTLE_ROYALE, ONLINE_MULTIPLAYER }

class FPSGame : ApplicationAdapter() {

    private lateinit var camera: PerspectiveCamera
    private lateinit var sceneManager: SceneManager
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var shapes: ShapeRenderer

    private lateinit var player: Player
    private lateinit var battleRoyale: BattleRoyaleManager
    private val multiplayer = MultiplayerClient()

    private var gameMode: GameMode = GameMode.OFFLINE_BATTLE_ROYALE
    private var matchStarted = false

    // asset cache: path -> SceneAsset mentah, dipakai untuk bikin Scene baru per instance
    private val gunAssets = HashMap<String, SceneAsset>()
    private val botAssets = HashMap<String, SceneAsset>()
    private val gunScenesInWorld = HashMap<Int, Scene>() // botId -> scene senjata yang dipegang
    private val botScenes = HashMap<Int, Scene>()
    private var viewmodelScene: Scene? = null // senjata yang terlihat di tangan player sendiri

    private val sfx = HashMap<String, Sound>()

    private val groundHeight = 0f
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lookTouchId = -1
    private var moveTouchId = -1

    // tombol tembak semi-auto: biar 1 tap = 1 peluru utk senjata non-auto
    private var firePressedEdge = false

    override fun create() {
        camera = PerspectiveCamera(GameSettings.fieldOfView, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat()).apply {
            near = 0.1f
            far = 500f
        }

        val shaderConfig = PBRShaderProvider.createDefaultConfig().apply {
            numBones = 60 // model KayKit butuh 41 tulang, kasih margin ke 60
            numDirectionalLights = 1
            numPointLights = 0
        }
        val depthShaderConfig = PBRShaderProvider.createDefaultDepthConfig().apply {
            numBones = 60
        }
        sceneManager = SceneManager(PBRShaderProvider(shaderConfig), PBRDepthShaderProvider(depthShaderConfig))
        sceneManager.setCamera(camera)
        val light = DirectionalLightEx().apply {
            direction.set(-1f, -0.8f, -0.4f).nor()
            color.set(Color.WHITE)
            intensity = 3f
        }
        sceneManager.environment.add(light)
        sceneManager.environment.set(PBRColorAttribute.createAmbientLight(Color(0.35f, 0.35f, 0.4f, 1f)))

        batch = SpriteBatch()
        font = BitmapFont() // font bawaan libGDX, cukup buat HUD angka ammo/health
        font.data.setScale(1.6f)
        shapes = ShapeRenderer()

        player = Player(camera)
        battleRoyale = BattleRoyaleManager()

        loadGunAssets()
        loadBotAssets()
        loadSfx()

        Gdx.input.inputProcessor = null // input ditangani manual lewat polling di render(), lebih gampang buat multi-touch custom

        // TODO: ganti dengan menu utama (pilih Offline/Online) kalau sudah dibikin.
        // Untuk sekarang match offline langsung auto-start biar begitu buka app, langsung main.
        startOfflineBattleRoyale()
    }

    private fun loadGunAssets() {
        for (w in WeaponData.ALL) {
            try {
                val handle = Gdx.files.internal(w.modelPath)
                gunAssets[w.id] = GLBLoader().load(handle)
            } catch (e: Exception) {
                Gdx.app.error("FPSGame", "Gagal load model senjata ${w.modelPath}: ${e.message}")
            }
        }
    }

    private fun loadBotAssets() {
        val variants = listOf("bot_rogue.glb", "bot_barbarian.glb", "bot_mage.glb", "player_knight.glb")
        for (v in variants) {
            try {
                botAssets[v] = GLBLoader().load(Gdx.files.internal("models/player/$v"))
            } catch (e: Exception) {
                Gdx.app.error("FPSGame", "Gagal load model karakter $v: ${e.message}")
            }
        }
    }

    private fun loadSfx() {
        val names = listOf(
            "ak47_shot", "pistol_shot", "smg_shot", "shotgun_shot", "sniper_shot",
            "reload", "empty_click", "footstep", "hitmarker", "zone_warning"
        )
        for (n in names) {
            try {
                sfx[n] = Gdx.audio.newSound(Gdx.files.internal("sfx/$n.wav"))
            } catch (e: Exception) {
                Gdx.app.error("FPSGame", "Gagal load sfx $n: ${e.message}")
            }
        }
    }

    fun startOfflineBattleRoyale() {
        gameMode = GameMode.OFFLINE_BATTLE_ROYALE
        battleRoyale.startMatch()
        player.respawn(Vector3(0f, 0f, 0f))
        matchStarted = true
        rebuildBotScenes()
    }

    fun connectOnline(ip: String, port: Int, name: String) {
        gameMode = GameMode.ONLINE_MULTIPLAYER
        multiplayer.connect(ip, port, playerId = "p_${Random.nextInt(100000)}", playerName = name)
        player.respawn(Vector3(0f, 0f, 0f))
        matchStarted = true
    }

    private fun rebuildBotScenes() {
        // bersihkan scene lama (match sebelumnya) sebelum spawn ulang
        botScenes.values.forEach { sceneManager.removeScene(it) }
        gunScenesInWorld.values.forEach { sceneManager.removeScene(it) }
        botScenes.clear()
        gunScenesInWorld.clear()
        for (bot in battleRoyale.bots) {
            val asset = botAssets[bot.modelVariant] ?: continue
            val scene = Scene(asset.scene)
            sceneManager.addScene(scene)
            botScenes[bot.id] = scene
            val gunAsset = gunAssets[bot.weapons.current.id]
            if (gunAsset != null) {
                val gunScene = Scene(gunAsset.scene)
                sceneManager.addScene(gunScene)
                gunScenesInWorld[bot.id] = gunScene
            }
        }
        // viewmodel senjata player sendiri (selalu tampil di depan kamera, FPP)
        val playerGunAsset = gunAssets[player.weapons.current.id]
        if (playerGunAsset != null) {
            viewmodelScene?.let { sceneManager.removeScene(it) }
            val vm = Scene(playerGunAsset.scene)
            sceneManager.addScene(vm)
            viewmodelScene = vm
        }
    }

    // ---------------------------------------------------------------------
    // INPUT (polling manual: layar kiri = gerak(joystick), layar kanan = look+tembak)
    // ---------------------------------------------------------------------
    private fun handleInput(delta: Float) {
        val w = Gdx.graphics.width
        val numPointers = 3 // cukup untuk gerak+look+tembak bersamaan

        var moveX = 0f; var moveZ = 0f
        var lookDX = 0f; var lookDY = 0f
        var triggerHeld = false
        var jumpTap = false

        for (p in 0 until numPointers) {
            if (!Gdx.input.isTouched(p)) continue
            val x = Gdx.input.getX(p).toFloat()
            val y = Gdx.input.getY(p).toFloat()

            if (x < w / 2f) {
                // --- sisi kiri layar: virtual joystick gerak ---
                if (Gdx.input.justTouched() || moveTouchId == -1) moveTouchId = p
                if (moveTouchId == p) {
                    // area joystick dianggap radius 150px dari titik sentuh pertama (disederhanakan: pakai delta dari tengah layar kiri)
                    val centerX = w / 4f
                    val centerY = Gdx.graphics.height / 2f
                    val dx = (x - centerX).coerceIn(-150f, 150f) / 150f
                    val dy = (y - centerY).coerceIn(-150f, 150f) / 150f
                    moveX = dx
                    moveZ = dy // y layar ke bawah = mundur, nanti dibalik di Player kalau perlu
                }
            } else {
                // --- sisi kanan layar: look (drag) + tembak (tap/hold) ---
                triggerHeld = true
                if (lookTouchId == -1) {
                    lookTouchId = p
                    touchStartX = x; touchStartY = y
                } else if (lookTouchId == p) {
                    lookDX = x - touchStartX
                    lookDY = y - touchStartY
                    touchStartX = x; touchStartY = y
                }
            }
        }
        if (moveTouchId != -1 && !Gdx.input.isTouched(moveTouchId)) moveTouchId = -1
        if (lookTouchId != -1 && !Gdx.input.isTouched(lookTouchId)) lookTouchId = -1

        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) jumpTap = true // debug keyboard (desktop/emulator)

        player.moveX = moveX
        player.moveZ = -moveZ
        player.wantsJump = jumpTap
        player.lookDelta(lookDX * 0.25f, lookDY * 0.25f, GameSettings.sensitivity, GameSettings.invertY)

        val isMoving = kotlin.math.abs(moveX) > 0.05f || kotlin.math.abs(moveZ) > 0.05f
        val reloadPressed = Gdx.input.isKeyJustPressed(Input.Keys.R)
        val fired = player.weapons.update(delta, triggerHeld, reloadPressed, isMoving)

        if (fired) {
            playSfx(player.weapons.current.sfxShot)
            performShoot(player.camera.position, player.camera.direction, isPlayer = true)
            if (gameMode == GameMode.ONLINE_MULTIPLAYER) {
                multiplayer.send(
                    NetMessage.Shoot(
                        "me", player.camera.position.x, player.camera.position.y, player.camera.position.z,
                        player.camera.direction.x, player.camera.direction.y, player.camera.direction.z,
                        player.weapons.current.id
                    )
                )
            }
        } else if (triggerHeld && player.weapons.currentMagAmmo() <= 0 && !player.weapons.isReloading) {
            playSfx("empty_click")
        }
    }

    private val rayHelper = Ray(Vector3(), Vector3())

    private fun performShoot(origin: Vector3, dir: Vector3, isPlayer: Boolean) {
        if (!isPlayer) return
        rayHelper.origin.set(origin)
        rayHelper.direction.set(dir).nor()
        val weapon = player.weapons.current
        var closestDist = weapon.range
        var hitBot: Bot? = null

        for (bot in battleRoyale.bots) {
            if (!bot.isAlive) continue
            val toBotDist = rayBoundingSphereHit(rayHelper, bot.position.cpy().add(0f, 1f, 0f), 0.9f)
            if (toBotDist != null && toBotDist < closestDist) {
                closestDist = toBotDist
                hitBot = bot
            }
        }

        hitBot?.let {
            val died = it.takeDamage(weapon.damage)
            playSfx("hitmarker")
            if (died) {
                val scene = botScenes.remove(it.id)
                scene?.let { s -> sceneManager.removeScene(s) }
                gunScenesInWorld.remove(it.id)?.let { s -> sceneManager.removeScene(s) }
            }
        }
    }

    /** Raycast-sphere sederhana: return jarak hit kalau kena, null kalau meleset. */
    private fun rayBoundingSphereHit(ray: Ray, center: Vector3, radius: Float): Float? {
        val toCenter = Vector3(center).sub(ray.origin)
        val proj = toCenter.dot(ray.direction)
        if (proj < 0) return null
        val closestPoint = Vector3(ray.direction).scl(proj).add(ray.origin)
        val distSq = closestPoint.dst2(center)
        if (distSq > radius * radius) return null
        return proj
    }

    private fun playSfx(pathOrKey: String) {
        val key = pathOrKey.substringAfterLast('/').substringBeforeLast('.')
        sfx[key]?.play(GameSettings.sfxVolume)
    }

    // ---------------------------------------------------------------------
    // MAIN LOOP
    // ---------------------------------------------------------------------
    override fun render() {
        val delta = Gdx.graphics.deltaTime.coerceAtMost(1f / 20f) // clamp biar fisika gak lompat kalau lag

        if (matchStarted && player.isAlive) {
            handleInput(delta)
            player.update(delta, groundHeight)

            if (gameMode == GameMode.OFFLINE_BATTLE_ROYALE) {
                battleRoyale.update(
                    delta, player.position, player.isAlive,
                    hasLineOfSight = { _, _ -> true }, // dunia masih terbuka/flat: LOS selalu true (ganti dgn raycast collision map nanti)
                    onBotShoot = { bot, origin, dir -> handleBotShoot(bot, origin, dir) }
                )
                if (battleRoyale.isOutsideZone(player.position)) {
                    player.takeDamage(5f * delta) // damage zona per detik
                }
                syncBotTransforms()
            } else {
                drainMultiplayerMessages()
                syncOwnStateToNetwork(delta)
            }
        }

        val gl = Gdx.gl
        gl.glClearColor(0.55f, 0.65f, 0.75f, 1f) // langit sederhana (ganti skybox kalau mau)
        gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        sceneManager.update(delta)
        sceneManager.render()

        renderHud()
    }

    private fun handleBotShoot(bot: Bot, origin: Vector3, dir: Vector3) {
        playSfx(bot.weapons.current.sfxShot)
        rayHelper.origin.set(origin)
        rayHelper.direction.set(dir).nor()
        val distToPlayer = rayBoundingSphereHit(rayHelper, player.position.cpy().add(0f, 0f, 0f), 0.6f)
        if (distToPlayer != null && distToPlayer < bot.weapons.current.range) {
            player.takeDamage(bot.weapons.current.damage)
        }
    }

    private fun syncBotTransforms() {
        for (bot in battleRoyale.bots) {
            if (!bot.isAlive) continue
            val scene = botScenes[bot.id] ?: continue
            scene.modelInstance.transform.setToTranslation(bot.position).rotate(Vector3.Y, bot.yaw)
            gunScenesInWorld[bot.id]?.modelInstance?.transform?.set(scene.modelInstance.transform)
        }
        // viewmodel senjata player: selalu nempel di depan-bawah kamera (gaya FPS klasik)
        viewmodelScene?.let { vm ->
            val offset = Vector3(camera.direction).scl(0.4f).add(0f, -0.25f, 0f)
                .add(Vector3(camera.direction).crs(Vector3.Y).nor().scl(0.22f))
            val pos = Vector3(camera.position).add(offset)
            vm.modelInstance.transform.setToTranslation(pos)
            vm.modelInstance.transform.rotate(Vector3.Y, -player.yaw)
        }
    }

    private fun drainMultiplayerMessages() {
        var msg = multiplayer.incoming.poll()
        while (msg != null) {
            when (msg) {
                is NetMessage.Hit -> if (msg.victimId == "me") player.takeDamage(msg.damage)
                is NetMessage.Shoot -> { /* TODO: render tembakan pemain lain + cek hit ke diri sendiri */ }
                else -> { /* STATE/DEATH/LEAVE pemain lain: update daftar remote players (disederhanakan utk versi awal) */ }
            }
            msg = multiplayer.incoming.poll()
        }
    }

    private var netSendTimer = 0f
    private fun syncOwnStateToNetwork(delta: Float) {
        netSendTimer += delta
        if (netSendTimer < 0.05f) return // kirim ~20x/detik, cukup buat gerak halus tanpa banjir bandwidth
        netSendTimer = 0f
        multiplayer.send(
            NetMessage.State(
                "me", player.position.x, player.position.y, player.position.z,
                player.yaw, player.pitch, player.health, player.weapons.current.id
            )
        )
    }

    // ---------------------------------------------------------------------
    // HUD (ammo, health, info zona, FPS, tombol setting grafik)
    // ---------------------------------------------------------------------
    private fun renderHud() {
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "HP: ${player.health.toInt()}", 24f, Gdx.graphics.height - 24f)
        font.draw(
            batch,
            "${player.weapons.current.displayName}  ${player.weapons.currentMagAmmo()} / ${player.weapons.currentReserveAmmo()}" +
                if (player.weapons.isReloading) "  (RELOADING...)" else "",
            24f, Gdx.graphics.height - 60f
        )
        if (gameMode == GameMode.OFFLINE_BATTLE_ROYALE) {
            font.draw(batch, "Tersisa: ${battleRoyale.botsAliveCount + 1} / ${GameSettings.botCount + 1}", 24f, Gdx.graphics.height - 96f)
            font.draw(batch, "Zona: ${battleRoyale.zoneRadius.toInt()}m", 24f, Gdx.graphics.height - 128f)
            when (battleRoyale.phase) {
                MatchPhase.PLAYER_WON -> font.draw(batch, "MENANG! Semua musuh tumbang.", Gdx.graphics.width / 2f - 150f, Gdx.graphics.height / 2f)
                MatchPhase.PLAYER_DIED -> font.draw(batch, "KALAH. Tekan restart.", Gdx.graphics.width / 2f - 100f, Gdx.graphics.height / 2f)
                else -> {}
            }
        } else {
            font.draw(batch, if (multiplayer.isConnected) "Online: Terhubung" else "Online: Menghubungkan...", 24f, Gdx.graphics.height - 96f)
        }
        font.draw(batch, "Grafik: ${GameSettings.quality.label}", Gdx.graphics.width - 220f, Gdx.graphics.height - 24f)
        batch.end()

        // crosshair sederhana (+ di tengah layar)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = Color.WHITE
        val cx = Gdx.graphics.width / 2f; val cy = Gdx.graphics.height / 2f
        shapes.line(cx - 10, cy, cx + 10, cy)
        shapes.line(cx, cy - 10, cx, cy + 10)
        shapes.end()
    }

    fun cycleGraphicsQuality() {
        GameSettings.quality = GraphicsQuality.next(GameSettings.quality)
        camera.far = GameSettings.quality.renderDistance
    }

    override fun resize(width: Int, height: Int) {
        sceneManager.updateViewport(width.toFloat(), height.toFloat())
    }

    override fun dispose() {
        sceneManager.dispose()
        gunAssets.values.forEach { it.dispose() }
        botAssets.values.forEach { it.dispose() }
        sfx.values.forEach { it.dispose() }
        batch.dispose()
        font.dispose()
        shapes.dispose()
        multiplayer.disconnect()
    }
}
