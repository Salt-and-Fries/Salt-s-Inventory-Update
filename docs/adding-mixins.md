# Adding Mixins

This project does not enable mixins by default. Add them only when you have at least one `@Mixin` class for a target; that keeps clean starter builds quiet.

## 1. Add A Mixin Config

Create `versions/<minecraft-version>/common/src/main/resources/salts_inventory_update.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.salts_inventory_update.mixin",
  "compatibilityLevel": "JAVA_${java_version}",
  "mixins": [],
  "client": [],
  "server": [],
  "injectors": {
    "defaultRequire": 1
  }
}
```

## 2. Register It In Fabric

Add this to `versions/<minecraft-version>/fabric/src/main/resources/fabric.mod.json`:

```json
"mixins": [
  "${mod_id}.mixins.json"
]
```

## 3. Register It In Forge Or NeoForge

Add this to `mods.toml` or `neoforge.mods.toml`:

```toml
[[mixins]]
config = "${mod_id}.mixins.json"
```
