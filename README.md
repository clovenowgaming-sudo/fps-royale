# FPS Royale (Android, FPP)

Game tembak-tembakan 3D First Person: senjata nyata (AK47, pistol, SMG, shotgun, sniper),
mode **Battle Royale offline** lawan sampai 99 bot (+ kamu = 100 total), dan mode
**Multiplayer Online** (lewat server relay sendiri). Dibangun pakai libGDX + Kotlin +
gdx-gltf (loader GLB/glTF langsung, tanpa perlu konversi model).

## Cara build APK (lewat GitHub Actions — gratis, otomatis)

1. Push folder ini ke repo GitHub kamu:
   ```
   git init
   git add .
   git commit -m "FPS Royale initial commit"
   gh repo create fps-royale --public --source=. --push
   ```
2. Buka tab **Actions** di repo GitHub-nya — workflow `Build APK` otomatis jalan.
3. Setelah selesai (±5-10 menit), buka run-nya → bagian **Artifacts** → download
   `fps-royale-debug-apk.zip` → di dalamnya ada `app-debug.apk`.
4. Pindahkan ke HP, install (aktifkan "Install dari sumber tidak dikenal" kalau diminta).

## Cara main mode Offline (Battle Royale vs 100 bot)

Langsung dari menu utama pilih **Offline Battle Royale** — 99 bot otomatis spawn
tersebar di map, zona mengecil tiap ~35 detik (mirip battle royale pada umumnya).
Jumlah bot bisa diubah di `GameSettings.botCount` (default 99).

## Cara main mode Online Multiplayer (butuh server sendiri)

Multiplayer online **butuh server yang nyala terus** — ini bukan sesuatu yang bisa
otomatis ada di dalam APK, karena APK cuma jalan saat HP kamu nyala & app dibuka.
Langkahnya:

1. Install JDK 17 di PC/laptop/VPS yang mau dipakai jadi server.
2. Compile server (hanya 2 file, ringan):
   ```
   kotlinc app/src/main/java/com/vinz/fpsroyale/net/GameServer.kt \
           app/src/main/java/com/vinz/fpsroyale/net/Protocol.kt \
           -include-runtime -d server.jar
   ```
3. Jalankan: `java -jar server.jar 7777`
4. **Main di WiFi yang sama** → di app, masukkan IP lokal PC (misal `192.168.1.5:7777`).
   **Main dari internet luar** → perlu port-forward 7777 di router kamu, atau sewa
   VPS murah (DigitalOcean/Contabo/dsb, ~$4-5/bulan) yang punya IP publik, lalu
   masukkan IP publik VPS itu di app.

Server ini adalah **relay sederhana** (broadcast posisi & tembakan ke semua client
lain) — cocok untuk main bareng teman (8-16 orang), belum ada matchmaking publik
otomatis atau anti-cheat.

## Yang sudah berfungsi

- Kontrol FPP: gerak (virtual joystick kiri), look+tembak (drag/tap kanan), lompat, reload (tombol R di keyboard/debug)
- 5 senjata dengan model .glb asli: AK47 (auto), Pistol (semi), SMG (auto), Shotgun (pump), Sniper (semi) — lihat `CREDITS.md`
- Ammo per-senjata, reload timer, fire-rate per senjata
- Bot AI: patrol → kejar → nembak, otomatis lari ke zona kalau di luar zona aman
- Zona battle royale yang mengecil otomatis
- Setting grafik 3 tingkat (Low/Medium/High) — pengaruh ke jarak render & jumlah bot yang digambar detail
- SFX tembakan/reload/langkah (sintesis, lihat `CREDITS.md`)
- Modul multiplayer client+server (socket TCP polos)

## Yang BELUM/disederhanakan (perlu kamu kembangkan lanjut)

- Dunia masih flat/kosong (belum ada map 3D dengan gedung/cover) — tambahkan model environment sendiri
- Line-of-sight bot masih selalu `true` (belum ada raycast collision ke geometri map, karena map belum ada)
- Animasi reload/jalan/aim player sendiri (viewmodel) belum di-trigger otomatis ke klip animasi GLTF — perlu dipetakan manual sesuai nama animasi di file model (lihat komentar di `FPSGame.kt` bagian `syncBotTransforms`)
- Mode online belum sinkronkan render pemain lain secara visual (baru kirim/terima data posisi)
- Model senjata "pistol" memakai model generik (lihat CREDITS.md) — ganti kalau mau pistol yang lebih detail

## Struktur proyek

```
app/src/main/java/com/vinz/fpsroyale/
  FPSGame.kt            - render loop utama, HUD, wiring semua sistem
  MainActivity.kt        - entry point Android
  core/Player.kt         - kontrol FPP, gravity, kamera
  core/GameSettings.kt   - preset grafik & setting global
  weapons/WeaponData.kt  - data 5 senjata
  weapons/WeaponManager.kt - ammo, reload, fire-rate, switch senjata
  ai/Bot.kt              - AI musuh (state machine)
  ai/BattleRoyaleManager.kt - spawn 100 bot + zona menyusut
  net/Protocol.kt        - format pesan multiplayer
  net/MultiplayerClient.kt - client TCP (jalan di dalam APK)
  net/GameServer.kt      - server TCP (dijalankan terpisah di PC/VPS)
```
