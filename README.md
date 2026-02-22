# 🏰 NaturalDungeon v2.0.7 (Grand Overhaul)

Sebuah pengalaman dungeon instanced yang sepenuhnya dirombak, menghadirkan standar "Premium & Dynamic" ke dalam NaturalSMP.

## ✨ Fitur Utama v2.0.7

### 🏛️ Dungeon Hub & Lobby Berbasis Hologram
*   **Visual Board**: Daftar statistik server, tantangan mingguan, dan status antrian portal disajikan via Hologram (API `TextDisplay` modern, no-dependencies).
*   **Party Finder**: Bermain bersama menjadi lebih mudah dengan command `/party` terintegrasi.

### ⚡ Sistem Dynamic Difficulty
*   Pilih tingkat kesulitan dari **Easy** hingga **Hell**.
*   **Environmental & Mechanic Modifiers**: Setiap kesulitan mengubah gameplay secara unik, dengan peningkatan damage musuh, tambahan nyawa, dan modifier visual tersendiri!

### 🌀 Roguelike Branching Paths
*   Setiap dungeon berjalan dalam format Stage semi-roguelike.
*   Pemain di dalam Party akan melakukan *Voting* untuk memilih cabang jalan selanjutnya: Combat Room, Puzzle Room, atau Safe Room Shop.
*   Random Events: 15% Chance untuk menemukan *Treasure Goblin*, *Elite Mob*, atau *Hidden Chest*.

### 🐉 Multi-Phase Boss System & Cutscenes
*   **Epic Boss Battles**: Bos yang berubah fase di persentase HP tertentu (Enrage, Desperation) dengan variasi 10 macam *Boss Skills* (Laser, Shield, Summon, Arena Shrink dll).
*   **Cinematic Entry**: Efek kebutaan, Title API sinematik, dan sound design memukau setiap kali Bos muncul.

### 📊 Admin Setup Experience: Visual First
Dungeon creation dibuat *sebegitu mudahnya* sampai tidak perlu membuang waktu melihat dokumentasi panjang:
*   **Interactive Setup Wizard** (`/nd create`): Atur setting dungeon hanya dengan klik GUI.
*   **Visual Stage Builder**: Tentukan batas arena dengan partikel preview yang jelas di dalam game.
*   **Wave Editor & Mob Picker**: Drag & drop wave mobs langsung. Mendukung integrasi *MythicMobs*, *Oraxen*, dan Custom Mobs.
*   **Smart Diagnostics & Auto-Fix**: Ada config atau region yang salah? Jalankan `/nd diag` dan tekan "🔨 Auto-Fix" untuk membetulkannya seketika!
*   **Live Testing Mode**: Mainkan dungeon dalam mode Admin-Demigod (Skip Wave, Insta-heal, Kill-All).

### � Party Role Synergy & Mastery
*   Role khusus untuk anggota Party: **Tank** (Extra HP, Aggr), **DPS** (Bonus Strike), **Healer** (Aura regeneration 8 block).
*   **Mastery System**: Selesaikan dungeon berulang kali untuk naik level *Mastery*, dan panjat papan **Legendary Season Rankings** di tiap musiman.

---

## 📑 Daftar Perintah Spesial (Command)

| Perintah | Deskripsi Utama | Izin Tambahan (Permission) |
| :--- | :--- | :--- |
| `/dungeon` | Membuka Menu Utama NaturalDungeon | `naturaldungeon.use` |
| `/party` | Membuka Dashboard Sosial Party | `naturaldungeon.party` |
| `/nd admin` | Membuka **Ultimate Admin Dashboard** (Status, Editor) | `naturaldungeon.admin` |
| `/nd test <id>` | Menjalankan Dungeon dalam *Live Test Mode* | `naturaldungeon.admin` |
| `/nd sethub` | Menyetel letak spawn lobby The Hub | `naturaldungeon.admin` |
| `/nd hub setboard`| Setup papan hologram portal, stats, weekly | `naturaldungeon.admin` |

---
**© 2026 NaturalSMP Development Team**
_Code Quality & System Architecture Refactored to NaturalCore Standards._
