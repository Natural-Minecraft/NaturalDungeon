# 🏰 NaturalDungeon Setup Tutorial

Complete guide to create a dungeon from scratch.

---

## 📋 Requirements

- WorldEdit (region selection)
- WorldGuard (safe zones)
- MythicMobs (custom mobs/boss)

---

## Step 1: Build Arena 🔨

```
┌─────────────────────────────────┐
│  [SAFE ZONE]  ← Spawn & Respawn │
│       ↓                         │
│  [ARENA]      ← Waves + Boss    │
└─────────────────────────────────┘
```

1. Build **Safe Zone** (5x5 min)
2. Build **Arena** (15x15 min)
3. Connect with hallway

---

## Step 2: WorldGuard Regions 🛡️

```bash
# Safe Zone
//wand → select area → //expand vert
/rg create dungeon_forest_safe
/rg flag dungeon_forest_safe pvp deny
/rg flag dungeon_forest_safe mob-spawning deny

# Arena
/rg create dungeon_forest_arena
/rg flag dungeon_forest_arena mob-spawning deny
```

---

## Step 3: Generate Config ⚡

Stand in safe zone, then:
```bash
/dg create forest_cave WAVE_DEFENSE
```

Creates: `plugins/NaturalDungeon/dungeons/forest_cave.yml`

---

## Step 4: Auto-Scan Spawns 🔍

Stand in arena center:
```bash
/dg scan forest_cave
```

---

## Step 5: Auto-Generate Loot 💎

```bash
/dg loot autogen forest_cave 5
```

Tier 1-10 (5 = mid-game loot)

---

## Step 6: Edit Config ✏️

`dungeons/forest_cave.yml`:
```yaml
display-name: "&2&lForest Cave"
world: "world"
max-players: 4

difficulties:
  normal:
    total-stages: 1
    max-deaths: 3
    stages:
      1:
        waves:
          - mobs: "ZOMBIE:5,SKELETON:3"
            delay: 5
        locations:
          1:
            safe-zone: "dungeon_forest_safe"
        has-boss: true
        boss-id: "ForestGuardian"
```

---

## Step 7: Create MythicMobs Boss 👹

`plugins/MythicMobs/Mobs/ForestGuardian.yml`:
```yaml
ForestGuardian:
  Type: ZOMBIE
  Display: '&2&lForest Guardian'
  Health: 500
  Damage: 8
```

---

## Step 8: Test! 🎮

```bash
/dg reload
/dg
```

Select dungeon → Enter!

---

## 🐛 Troubleshooting

| Problem | Solution |
|:---|:---|
| World not found | Check world name in config |
| Can't spawn | Check safe-zone region exists |
| Mobs don't spawn | Check mob-spawns coordinates |
| Boss missing | Check boss-id matches MythicMobs |

---

## Quick Commands Reference

| Command | Description |
|:---|:---|
| `/dg create <id> <type>` | Generate config template |
| `/dg scan <id>` | Auto-detect spawn points |
| `/dg loot autogen <id> <tier>` | Populate loot tables |
| `/dg reload` | Reload all configs |
| `/dg` | Open dungeon GUI |
