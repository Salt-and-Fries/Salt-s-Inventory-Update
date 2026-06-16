# Salt's Inventory Update Plan

## Current Target

Fabric `26.1.2` is the main target version for the first implementation pass. Other supported versions and loaders can remain in the project, but the initial feature work should be proven against Fabric `26.1.2` first.

## Core Goal

Salt's Inventory Update will completely rework Minecraft inventory interaction around movable windows. Containers, the player inventory, equipment, crafting, and character status will be separated into focused UI windows instead of being locked into the vanilla single-screen layout.

The mod should make inventory management feel like a flexible desktop-style interface while preserving Minecraft's core item interactions.

## Windowed Containers

Every container should open as its own window.

Each container window should support:

- Moving the window around the screen.
- Closing the window.
- Minimizing the window.
- Resizing the window when the container type supports it.
- Focusing the window so it can receive quick-moved items.

Container windows should be generated dynamically from the container's slot positions. The window layout should respect the container's actual slots rather than assuming every container is a simple rectangular grid.

Every window should have a top bar:

- The container label appears on the top left.
- The close, minimize, and focus buttons appear on the far right.

Only one window can be focused at a time.

## Single Player Inventory Window

The player inventory should no longer be duplicated underneath every opened container.

Instead:

- Opening the player inventory opens one standalone player inventory window.
- Opening a container opens only that container's window.
- The player inventory window does not include a duplicate hotbar row.
- Multiple windows can be open at the same time.
- Players can drag items between open windows.

When one window has focus enabled, shift-clicked items from any open inventory or container should quick-move into the focused container when possible.

Example:

- Opening a chest should show only the chest's `3x9` slot window.
- The player inventory remains its own separate window.
- Items can be dragged between the chest window and player inventory window.
- If the chest window is focused, shift-clicked player inventory items should try to move into the chest.

## Hotbar As An Interactive Inventory Object

The player's actual hotbar should become an interactive inventory object.

Instead of showing a hotbar row inside the inventory window, the on-screen hotbar is the real set of hotbar slots. Items should be movable into, out of, and within the hotbar using the same windowed inventory interaction model.

Hotbar behavior:

- The hotbar remains visible as the player's active item bar.
- The hotbar slots act as inventory slots when inventory interaction is active.
- Inventory windows should interact directly with the real hotbar slots.
- The player inventory window should not render a separate hotbar row.

When no containers are open, holding `Alt` should activate mouse control on the screen so the player can interact with hotbar items.

## Player Inventory Rework

The player inventory should be redesigned as a dynamic, scrollable window.

The visible player inventory layout should be:

- `6x3` visible slots.
- A scrollbar on the right.

The player's total inventory size should be able to grow over time. The `6x3` view is only the visible page, not the permanent maximum inventory size.

This means the player inventory system needs to support:

- More slots than are currently visible.
- Scrolling through extra slots.
- Stable item movement between visible and off-screen inventory slots.
- Future upgrades or progression systems that add more player inventory slots.

## Character, Armor, Status, And Crafting Window

The armor slots, player visual, and inventory crafting grid should be removed from the main player inventory window.

These features should move to a separate character/status window opened with the `C` key.

The `C` key window should include:

- Armor slots.
- Player model preview.
- Health display.
- Hunger display.
- XP and level display.
- Active potion effects.
- The `2x2` inventory crafting grid.

This window should act as the dedicated place for player equipment, character status, and small crafting.

## Movement, Background, And Mouse Control

The inventory interface should be inspired by Vintage Story's inventory flow, where UI windows can remain open while the player still has some world control.

When any inventory container or inventory window is open:

- The background should darken to show that inventory UI is active.
- The player can still move with `WASD`.
- Inventory windows stay visible on top of the game view.

Mouse and camera behavior:

- With a container or inventory window open, the mouse controls the UI by default.
- Holding `Alt` while a container or inventory window is open gives mouse control back to the player camera while keeping the UI elements open.
- Releasing `Alt` returns mouse control to the inventory UI.
- Holding `Alt` with no containers open activates screen mouse control so the player can move hotbar items.

## Resizing Rules

Resizable container windows should not scale slot size.

Instead, resizing should change how many slots are visible inside the window:

- Slots keep their normal size.
- The window determines how much of the slot layout is visible.
- A scrollbar appears on the right when the full slot layout does not fit.
- The slot grid or slot area should fit inside the current window size.

Some containers have specific slot positions that cannot be safely compressed, rearranged, or dynamically shrunk. Because of that, resizing should be controlled by a whitelist.

Resizing policy:

- Whitelisted containers can be resized.
- Non-whitelisted containers keep a fixed size.
- Special layouts should remain fixed unless explicitly supported later.

## Sort Into Nearby Chests

The player inventory window should include a "sort into nearby chests" button.

When clicked, the button should transfer matching items from the player's inventory into nearby chests.

Sorting behavior:

- Scan nearby chests.
- Check which items already exist inside those chests.
- Move matching items from the player inventory into those chests.
- Do not move items into chests that do not already contain the matching item.

The goal is to make storage cleanup fast while avoiding random dumping into unrelated chests.

## Implementation Priorities

Recommended first implementation order:

1. Establish the Fabric `26.1.2` client UI foundation.
2. Build the base movable window system.
3. Render container slots inside generated windows.
4. Split player inventory from container screens.
5. Make the real hotbar an interactive inventory object instead of rendering a duplicate inventory hotbar row.
6. Add item dragging between windows and hotbar slots.
7. Add focused-window quick-move behavior.
8. Add darkened inventory background, `WASD` movement support, and `Alt` mouse/camera control switching.
9. Rework the player inventory into a `6x3` scrollable view.
10. Add the `C` key character/status/crafting window.
11. Add resize support for whitelisted containers.
12. Add the nearby chest sorting button and transfer behavior.

## Open Design Questions For Later

These decisions can be finalized before implementation:

- Which container types should be resizable in the first version.
- How far the nearby chest sorting scan should reach.
- Whether minimized windows should dock to an edge, taskbar, or compact floating header.
- Whether window positions and sizes should persist between sessions.
- How focused-window quick-move should behave when the focused container is full.
- How expanded player inventory slots are unlocked or added over time.
- Whether hotbar mouse control with no open containers should require holding `Alt` or support a toggle option later.
- Whether players should be able to interact with inventory windows while continuing to attack, use items, or only move with `WASD`.
