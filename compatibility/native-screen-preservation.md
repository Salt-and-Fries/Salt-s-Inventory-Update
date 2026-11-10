# Native-screen preservation checks

The six native-only Project Everything entries were opened with shipping Core
0.1.6 and Compatibility 0.1.3. Their original screens own rendering and input;
none of the observed interfaces created an SIU window or desktop session.
No production code or release-jar change was needed.

| Mod | Checked release | Minecraft / loader | Observed interfaces and focused controls |
| --- | --- | --- | --- |
| DungeonZ | 1.0.6 | 1.20.1 Fabric | Portal operator editor, portal/difficulty/options, gate operator editor and compass selection; Done, Escape and reopening |
| ElevatorMod | 1.9.1 | 1.20.1 Forge | Elevator Options; facing selection, directional/arrow controls, persistence and reopening |
| Sortilege | 9.0 | 1.20.1 Fabric | Empty and populated Knowledge Book; next/previous pages, keyboard navigation, Escape and reopening |
| Armor Statues | 21.1.0 | 1.21.1 NeoForge | Rotation, poses, style, position, alignment and equipment tabs; text entry and Show Arms reach the server; Escape and reopening |
| Things | 0.3.3+1.20 | 1.20.1 Fabric | Displacement Tome; create a named destination, reopen, teleport with one charge deducted and close |
| Wraith Waystones | 3.3.3+mc1.20.1 | 1.20.1 Fabric | Waystone destination/search/settings, rename persisting after reopening, Pocket Wormhole and Abyss Watcher; Escape and reopening |

The focused results contain **108 assertions across 16 observed screen types**.
Frames retain their native centered layout and scale. In particular, a menu
class alone is not evidence that it should become a window: these readers and
selectors can use handled screens without providing mutable inventory storage.
Armor Statues' native equipment tab also remains within its original editor.
No whole-mod blacklist or new public API rule was introduced.

Original publisher jars and shipping SIU jars were staged without rewriting
their classes. Fabric and Forge used production launchers; the NeoForge check
used its named launcher. Only the excluded observation/input driver was built
for these checks. Native actions were reached through ordinary block/item/entity
interactions and UI input; no target screen was constructed by the driver.
Fixture items and blocks were seeded in disposable worlds.

## Evidence and limits

[Verification, target hashes and current screenshot paths](../output/native-screen-preservation/verification.json)
link each result to its original capture. Per-mod `checks.json` files preserve
the command/state history. Setup attempts, corrected test expectations and
unverified gameplay probes remain recorded separately from passing checks.
Release checksums and the previous bundle hash were verified unchanged.

This was a focused native-screen check, not a complete gameplay suite or a
claim about every Minecraft/loader release. DungeonZ Join was clicked, but its
empty-world attempt later reached the native death screen; successful dungeon
generation, party entry and completion were not certified. Compass selection
was checked without running its structure search. Armor Statues' conditional
Vanilla Tweaks datapack view was not activated; its retained inheritance ends
at the native Minecraft `Screen`, outside automatic container conversion.
