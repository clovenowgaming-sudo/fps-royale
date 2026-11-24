# Credits & Lisensi Aset

Semua model & tekstur di bawah ini diambil dari sumber gratis yang mengizinkan
pemakaian komersial/non-komersial. Simpan file ini kalau mau redistribusi APK.

## Senjata (`app/src/main/assets/models/guns/`)

| File | Asal | Catatan |
|---|---|---|
| `ak47.glb` | Low-poly weapon pack oleh **amaraha** (itch.io: free-low-poly-weapons-pack), dipakai lewat proyek open-source **Jeh3no/Godot-simple-FPS-weapon-system** (MIT) | Gratis untuk project game, jangan di-redistribusi ulang "sebagai milik sendiri" — kreditkan amaraha |
| `pistol.glb` (dari `pew.glb`) | sama seperti di atas (model generik, cocok dipakai sbg pistol starter) | Ganti dengan model pistol lain kalau mau tampilan beda |
| `smg.glb` (dari `mac10.glb`) | sama seperti di atas | — |
| `sniper.glb` (dari `awp.glb`) | sama seperti di atas | — |
| `shotgun.glb` | sama seperti di atas | — |

## Karakter player & bot (`app/src/main/assets/models/player/`)

| File | Asal | Lisensi |
|---|---|---|
| `player_knight.glb`, `bot_rogue.glb`, `bot_barbarian.glb`, `bot_mage.glb` | **KayKit Character Pack: Adventurers** oleh Kay Lousberg (kaylousberg.com), via github.com/KayKit-Game-Assets | **CC0 1.0** (public domain) — bebas pakai komersial, kredit opsional |

Catatan: karakternya bergaya fantasy (ksatria/rogue/mage), bukan tentara modern,
tapi sudah **rigged + banyak animasi** (idle/jalan/lari/serang) bawaan — jadi animasi
jalan & tembak bot sudah berfungsi dari sananya. Kalau mau ganti ke model tentara
modern, cari model GLB/FBX lain yang rigged dengan skeleton humanoid serupa lalu
ganti path di `FPSGame.kt` (`loadBotAssets()`).

## SFX (`app/src/main/assets/sfx/`)

Semua file `.wav` di-**generate secara sintesis** pakai Python (noise burst +
tone + envelope decay) — bukan rekaman, jadi 100% bebas lisensi dan bisa
diganti kapan saja dengan SFX asli kalau kamu punya.

## Ikon aplikasi

Dibuat generik (lingkaran merah + crosshair putih) programatically, bukan jiplakan logo apa pun.

---

**Kalau mau ganti model manual** (misal mau model AK47/pistol yang lebih detail/foto-realistis):
1. Cari file `.glb` atau `.gltf` (format ini yang didukung langsung tanpa konversi)
2. Taruh di `app/src/main/assets/models/guns/` (ganti nama sesuai yang dipakai di `WeaponData.kt`)
3. Push ulang ke GitHub, build otomatis jalan lagi

Situs rekomendasi buat cari model CC0 lain: `kenney.nl`, `quaternius.com`, `poly.pizza`,
`opengameart.org` (filter lisensi CC0), GitHub org `KayKit-Game-Assets`.
