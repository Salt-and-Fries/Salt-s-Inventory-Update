# Salt's Inventory Update

**Salt's Inventory Update is early in development.** Please report any issues you find, especially UI bugs, item movement problems, multiplayer weirdness, or compatibility issues.

Salt's Inventory Update reworks Minecraft inventory management into a flexible desktop-style interface. Instead of every container taking over the whole screen and repeating your player inventory underneath it, inventories open as separate movable windows that can stay on screen together.

![mutiple chests open at one time](https://cdn.modrinth.com/data/cached_images/933d2a9239a50fee4eb8e202b122e51c3ce839f0.png)

## Desktop-Style Inventory Windows

Containers, your player inventory, creative inventory, and character controls open as individual windows. Windows can be moved, minimized, focused, pinned, locked, and, where supported, resized.

This makes it easier to compare inventories, move items between containers, keep useful windows nearby, and organize your screen around the way you actually play.

![Chest open and a stone cutter open](https://cdn.modrinth.com/data/cached_images/120e65b09b84b770020f2b05ee90cd067cec52fb.png)

## A Cleaner Player Inventory

The player inventory is split away from container screens. Your main inventory appears as its own window, while the hotbar stays as the actual hotbar on the HUD instead of being duplicated inside every inventory screen.

The result is less repeated UI, more usable screen space, and a more natural workflow when moving items between storage, tools, and your active hotbar.

## Interactive Hotbar And Offhand

When the Salt desktop is active, the real hotbar becomes interactive. You can move items into, out of, and around the hotbar directly, and the offhand slot is also available beside it.

You can also hold the `Change Mouse Focus` key (`Alt` by default) with no inventory windows open to interact with the hotbar using the mouse.

![Hovering over an item in the Hotbar](https://cdn.modrinth.com/data/cached_images/af42081e77bc109e89813cb53fe41765d0b72fda.png)

## Character Window

Armor, player preview, health, hunger, XP, potion effects, and the 2x2 crafting grid are moved into a dedicated Character Window opened with `C`.

This keeps the main inventory focused on storage while still giving you quick access to equipment, status, and small crafting.

![Character window and Inventory window](https://cdn.modrinth.com/data/cached_images/fdfa1efb7a2bddb23435554166aa0b22e4d98af0.png)

## Optional Expandable Inventory

Salt's Inventory Update includes an optional expandable inventory system. When enabled in the config, players can spend XP levels to unlock extra inventory slots over time.

Extra slots save with the player, sync in multiplayer, and behave like real inventory storage for item movement, searching, dropping, and clearing.

## Move While Managing Inventory

The inventory desktop is designed to feel less like a full pause screen. You can keep windows open while still moving around, and hold the configurable `Change Mouse Focus` key (`Alt` by default) to give the mouse back to camera control when needed.

This is especially useful when sorting loot, working around storage rooms, or managing items while staying aware of the world.

## Pinning, Ghost Pins, And Saved Layouts

Windows can be pinned so they reopen where you left them. Optional ghost pins let windows collapse into translucent previews instead of disappearing completely, making frequently used containers easier to return to.

Window positions, sizes, lock states, and pin modes are saved per world or server.

![ghost pin preview over the world](https://cdn.modrinth.com/data/cached_images/f7e79607a37db0969c02358c25c42dcdc1ccf9e8.png)

## Configurable Experience

The mod includes an in-game config screen available through:

- `/saltsinventory config`
- `/salts_inventory config`
- Mod Menu on Fabric

You can toggle the mod, enable expandable inventory, change window placement behavior, allow resizing, enable snapping, configure ghost pins, adjust ghost opacity, and tune the hold-to-close-all timing.

## Compatibility And API

Salt's Inventory Update includes a desktop window API for other developers who want to make their own container UIs compatible with Salt's window system.

The API lets mods register custom desktop windows, render their own UI inside Salt windows, handle custom inputs, sync server payloads, use virtual item grids, support resizing, and save per-window state.

API docs: https://salt-and-fries.github.io/Salt-s-Inventory-Update/

Developers are encouraged to try the API and report anything that makes compatibility difficult.

## JEI Compatibility

When Just Enough Items is installed, Salt adds a dedicated JEI desktop window opened with `H`. It keeps JEI browsing inside Salt's movable window system instead of sending you back to a full-screen menu.

The JEI window supports JEI search syntax, ingredient tabs, Favorites, Recent lookups, recipe and uses pages, category and crafting-station tabs, recipe history, `R`/`U` hover lookups, bookmarks, and Move Items buttons for compatible open Salt crafting windows.

JEI is optional. Salt still launches normally without JEI installed, and the JEI window simply stays unavailable until JEI is present.

## Mod Compatibility

Salt's Inventory Update includes built-in compatibility work for Tom's Simple Storage, including desktop-style support for storage terminals, crafting terminals, filters, links, emitters, and filing cabinets.

Unsupported modded screens should fall back to their normal UI instead of being forced into a Salt window.

## Multiplayer Notes

For multiplayer, Salt's desktop features need server-side support from the mod. If a remote server does not support Salt desktop networking, the mod will disable the desktop behavior for that server instead of trying to desync inventory state.

## Why Use It?

Salt's Inventory Update is for players who want inventory management to feel more flexible, persistent, and powerful. It helps when organizing storage, comparing containers, crafting, managing equipment, and moving items around without constantly reopening full-screen menus.
