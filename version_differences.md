# Version Differences

## Sort into open containers

All five Minecraft versions and all ten Fabric/Forge/NeoForge targets expose the same sorting button. Normal click routes exact item types into other open storage; Shift continues through item tags, focused storage, then initially empty storage. Hotbar, equipment, crafting inputs, machine inputs, and fuel are excluded. Furnaces expose their result slot; brewing stands expose the three bottle slots. Sophisticated Storage and Backpacks expose storage slots through their existing compatibility adapter, excluding upgrade and settings slots.

Every control atlas is 132×33, with 11×11 cells. Zero-based columns 10 and 11 contain normal and Shift sorting icons; the three existing rows remain normal, hover, and pressed. Copy edits from the 26.2 atlas to the other version atlases to retain texture parity. Hover text is “Sort into open containers” or “Force sort into open containers” according to Shift state.

The 1.20.1 port uses its existing authenticated packet envelope and NBT-aware stack comparison. Later versions use their existing typed mutation queue and component-aware stack comparison. All negotiate the optional sorting capability before exposing the control and use snapshot routing with bounded, verified slot transfers.

Sorting regression and build commands are documented in `functional-tests/SORT-WINDOWS-VALIDATION.md`.

## Native container open state and sounds

All five Minecraft versions and all ten Fabric/Forge/NeoForge targets recognize live Salt sessions when vanilla chests, barrels, and ender chests recheck their openers. Minecraft retains responsibility for opening/closing sounds, block state, and lid animation; Salt does not synthesize duplicate sound events. Double/trapped chests use the chest ownership path, while shulker boxes retain their native menu start/stop lifecycle.

Each detached ender-chest menu binds to its own physical block, even though the player's ender inventory is shared. Closing or validating one window therefore cannot act on a different ender chest opened later. When Salt capture is disabled, ordinary menus retain native ownership and lifecycle behavior. Runtime coverage and remaining checks are recorded in `functional-tests/CONTAINER-LIFECYCLE-VALIDATION.md`.

## Shared creative paging texture

All versions and loaders use Salt's own `textures/gui/creative_buttons.png` for creative-tab previous/next buttons. Edit the single 256Ã—256 source under `artwork/shared-resources/assets/salts_inventory_update/textures/gui/`; every loader packages it. The button cells are 11Ã—12 pixels; all targets use the matching 11-pixel stride and 22-pixel hovered-pair offset to avoid clipping. See `artwork/CREATIVE-BUTTONS.md` for their coordinates.

## EMI support

Minecraft 1.21.1 and earlier versions support EMI integration. Minecraft versions newer than 1.21.1 do not support EMI because EMI does not currently provide compatible releases for those versions.

## Sophisticated Backpacks and Sophisticated Storage

Salt's desktop-window integration for Sophisticated containers is available on every Forge and NeoForge target that Salt builds: Minecraft 1.20.1 Forge plus Minecraft 1.21.1, 1.21.11, 26.1.2, and 26.2 NeoForge. Every Fabric target continues to use the complete native Sophisticated screens and has no Sophisticated dependency, metadata entry, mixin registration, or behavior change.

The Forge/NeoForge integration composes Sophisticated containers into a single Salt frame rather than embedding a second native window. Backpacks and standard storage prefer an initial size that exposes every storage row that fits on the current desktop, then use Salt's responsive, slot-snapped layout and scrollbar when the complete inventory cannot fit. Their no-scroll width follows the exact hosted slot grid instead of retaining Sophisticated's native outer margins, and reserves a scrollbar gutter only while scrolling is actually required; existing saved windows discard either obsolete width once without replacing other user sizing. Releasing an overly tall resize snaps the frame back to the useful storage-row count instead of retaining empty rows. A compact Sophisticated toolbar sits at the left above the raised storage grid; expanding or focusing its search control temporarily hides the sort and transfer controls so the search field can use the available width. Native Sophisticated upgrade slots and enable controls sit in a spaced, Salt-framed rail attached to the left side, with the rail's right edge tucked behind the main frame so it reads as a tab. Every visible right-side native tab replaces its original tab plate with an individual Salt window frame, fitted to the native icon width, whose left edge tucks beneath the main window; an opened panel receives its own expanded Salt frame while retaining Sophisticated's native icon, controls, and behavior. When the closed right-side tabs are taller than the normal container body, the base window grows only enough to meet the bottom of that closed tab stack; opening an expandable panel does not permanently enlarge it. Regular settings windows use the exact storage-grid body without the native right margin or former template footer; the original save, load, and export controls instead occupy the same tucked-under Salt-framed left tab design. Their temporary name fields retain native focus behavior without changing the base window's position when they appear near a desktop edge. Limited Barrels and their settings retain enough fixed body space for their specialized slot and capacity rendering, growing only when their closed tab stack requires it. Native sorting, searching, transfers, tanks, ghost and memory slots, upgrade panels, and settings/template controls keep their original behavior without a duplicated title, frame, or player-inventory area.

Salt and Sophisticated slot rendering use one canonical item origin, keeping items, hover highlights, and the native textures inside opened side panels aligned. Side-panel slot clicks and stack dragging use the same authenticated detached-container session as the main storage grid.

Mouse item gestures also share the desktop rather than stopping at loader boundaries. Left- and right-drag previews update live across ordinary Salt and Sophisticated slots, and the exact previewed set is committed as one authenticated operation when the button is released. Double-click collection scans eligible slots in the visible Salt windows and the player inventory, while shift-double-click keeps Sophisticated's native slot and upgrade rules. Item and menu mutations use one authenticated lane; native optimistic controls wait for that lane instead of changing unsent local state. Multi-slot gestures snapshot physical handler identity as well as contents, reject handler rebinding or aliases during callbacks, and reconcile the shared cursor on failure so rapid clicks, swaps, throws, and native controls cannot mutate the wrong detached session.

While one of these detached container sessions is alive, its physical Sophisticated Storage block remains visibly open. Sophisticated's own ownership rules are evaluated against Salt sessions during opener rechecks, covering chests (including paired chests), barrels and Limited Barrels, shulker-style storage, and main/settings navigation without keeping unrelated storage open.

While a top-level item Backpack from the player's main inventory, hotbar, offhand, or worn chest slot is open in a Salt window, that exact source Backpack is locked for the lifetime of its Salt session and cannot be clicked, moved, swapped, dropped, recipe-transferred, or used through the inventory, equipment view, hotbar, or HUD. The lock is released when the last Salt session for that source closes; other Backpack items and placed Backpacks are unaffected. This protection is also exclusive to Forge and NeoForge and does not change Fabric behavior.

Main/settings navigation replaces the existing Salt window and retains its position and pin state. Fabric continues to use the complete native Sophisticated screens instead of this composed layout.

Supported menu IDs:

- `sophisticatedbackpacks:backpack` and its settings menu.
- `sophisticatedstorage:storage`.
- `sophisticatedstorage:limited_barrel`.
- `sophisticatedstorage:settings`.
- `sophisticatedstorage:limited_barrel_settings`.

The Sophisticated Storage Decoration Table is intentionally excluded and continues to use its native screen.

Compatibility floors and validation versions are:

- Minecraft 1.20.1 Forge 47.4.0: Sophisticated Core `1.20.1-1.3.84.2308`, Backpacks `1.20.1-3.24.67.2109`, and Storage `1.20.1-1.4.86.2131`.
- Minecraft 1.21.1 NeoForge 21.1.229: Sophisticated Core `1.21.1-1.4.90.2299`, Backpacks `1.21.1-3.25.78.2107`, and Storage `1.21.1-1.5.91.2127`.
- Minecraft 1.21.11 NeoForge 21.11.42: Sophisticated Core `1.21.11-1.4.97.2313`, Backpacks `1.21.11-3.25.84.2111`, and Storage `1.21.11-1.5.102.2130`.
- Minecraft 26.1.2 NeoForge 26.1.2.59-beta: Sophisticated Core `26.1.2-1.4.104.2314`, Backpacks `26.1.2-3.25.90.2106`, and Storage `26.1.2-1.5.112.2123`.
- Minecraft 26.2 NeoForge 26.2.0.53-beta: Sophisticated Core `26.2-1.4.101.2276`, Backpacks `26.2-3.25.90.2091`, and Storage `26.2-1.5.108.2088`.

These mods remain optional in released Salt artifacts; development runs can opt in with `-PincludeSophisticatedRuntime=true`. `-PincludeJeiRuntime=false` controls Gradle's optional dependency, not manually installed jars: check the client's `mods` directory for stale JEI jars if Sophisticated reports an incompatible JEI version. The JavaExec cleanup hook does not cover every loader's run-task implementation. When JEI is enabled on NeoForge, Gradle supplies versions that satisfy the selected Sophisticated Core releases; Fabric keeps its existing independent JEI versions and remains unchanged by the Sophisticated integration.

Runtime validation progress and remaining coverage are recorded in `functional-tests/SOPHISTICATED-VALIDATION.md`. Successful builds alone do not validate version-specific mixin targets or full interaction parity.
