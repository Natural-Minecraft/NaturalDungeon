# 🏰 Panduan Lengkap: Membuat Dungeon dari Nol

Tutorial ini akan membimbing kamu membuat 1 dungeon lengkap dari awal sampai bisa dimainkan.

---

## 📋 Persiapan

**Yang dibutuhkan:**
- WorldEdit (untuk region selection)
- WorldGuard (untuk safe zone)
- MythicMobs (untuk custom mobs/boss)

---

## Step 1: Bangun Arena Fisik 🔨

### 1.1 Konsep Layout
```
┌─────────────────────────────────┐
│  [SAFE ZONE]  ← Spawn & Respawn │
│       ↓                         │
│  [ARENA 1]    ← Wave 1-3        │
│       ↓                         │
│  [ARENA 2]    ← Wave 4-6 + Boss │
└─────────────────────────────────┘
```

### 1.2 Build di Minecraft
1. Buat **Safe Zone** (ruangan spawn player, 5x5 minimal)
2. Buat **Arena** (tempat fight, 15x15 minimal)
3. Pastikan ada jalan dari safe zone ke arena
4. (Opsional) Buat arena kedua untuk Stage 2

> 💡 Tips: Gunakan barrier blocks untuk mencegah player kabur!

---

## Step 2: Setup WorldGuard Regions 🛡️

### 2.1 Protect Safe Zone
```
//wand
# Select pojok 1 dan pojok 2 safe zone
//expand vert
/rg create dungeon_forest_safe
/rg flag dungeon_forest_safe pvp deny
/rg flag dungeon_forest_safe mob-spawning deny
/rg flag dungeon_forest_safe invincibility allow
```

### 2.2 Protect Arena
```
# Select arena area
/rg create dungeon_forest_arena
/rg flag dungeon_forest_arena pvp deny
/rg flag dungeon_forest_arena mob-spawning deny
```

---

## Step 3: Generate Config Otomatis ⚡

### 3.1 Berdiri di Safe Zone
Posisikan dirimu di tengah safe zone.

### 3.2 Jalankan Generator
```
/dg create forest_cave WAVE_DEFENSE
```

Ini akan membuat file:
`plugins/NaturalDungeon/dungeons/forest_cave.yml`

---

## Step 4: Edit Config Manual ✏️

Buka `forest_cave.yml` dan sesuaikan:

```yaml
display-name: "&2&lForest Cave"
world: "world"      # ← Ganti dengan world dungeon
max-players: 4
cooldown: 3600      # 1 jam cooldown

difficulties:
  normal:
    total-stages: 1
    max-deaths: 3
    
    stages:
      1:
        waves:
          - mobs: "ZOMBIE:5,SKELETON:3"
            delay: 5
          - mobs: "ZOMBIE:8,SPIDER:4"
            delay: 5
          - mobs: "CREEPER:3,ZOMBIE:10"
            delay: 5
        
        # PENTING: Isi koordinat yang benar!
        locations:
          1:
            safe-zone: "dungeon_forest_safe"  # Nama region WG
            mob-spawns:
              - "world,100,65,200,0,0"   # x,y,z,yaw,pitch
              - "world,105,65,205,0,0"
              - "world,95,65,198,0,0"
        
        has-boss: true
        boss-id: "ForestGuardian"  # ID MythicMobs
        boss-spawn: [102.5, 65, 202.5]

loot:
  completion:
    - material: DIAMOND
      amount: 3
      chance: 0.5
    - material: GOLD_INGOT
      amount: 10
      chance: 0.8
```

---

## Step 5: Scan Spawn Points (Shortcut) 🔍

Kalau malas isi koordinat manual:

```
# Berdiri di tengah arena
/dg scan forest_cave
```

Ini akan auto-detect lokasi spawn dan tambahkan ke config!

---

## Step 6: Populate Loot Otomatis 💎

```
/dg loot autogen forest_cave 5
```

Tier 5 = mid-game loot (iron, gold, some diamonds)

---

## Step 7: Setup MythicMobs Boss 👹

Buat file `plugins/MythicMobs/Mobs/ForestGuardian.yml`:

```yaml
ForestGuardian:
  Type: ZOMBIE
  Display: '&2&lForest Guardian'
  Health: 500
  Damage: 8
  Options:
    MovementSpeed: 0.3
    PreventOtherDrops: true
  Skills:
    - skill{s=groundSlam} @self ~onTimer:100
  Drops:
    - DIAMOND 5-10 1
```

---

## Step 8: Test! 🎮

```
/dg reload
/dg            # Buka GUI
```

Pilih dungeon → pilih difficulty → **Enter**!

---

## ✅ Checklist Final

- [ ] Safe zone region dibuat
- [ ] Arena region dibuat  
- [ ] Config file ada di `dungeons/`
- [ ] Koordinat spawn benar
- [ ] MythicMobs boss dibuat
- [ ] Test main 1x sampai selesai

---

## 🐛 Troubleshooting

| Problem | Solution |
|:---|:---|
| "World not found" | Cek nama world di config |
| Player tidak bisa spawn | Cek region safe-zone ada |
| Mob tidak spawn | Cek koordinat mob-spawns |
| Boss tidak muncul | Cek boss-id sama dengan MythicMobs |

---

**Selamat!** Kamu sudah punya 1 dungeon yang bisa dimainkan! 🎉
