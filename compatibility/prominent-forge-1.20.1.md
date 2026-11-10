# Prominent Base forge windows — Minecraft 1.20.1 Fabric

This adapter targets the original embedded **Prominent Base 4.1.5** release.
It implements the Primordial Forge reinforcement/upgrade screen and armor
selection screen. The Cinder/Serkonid trader now has its own
[native shop adapter](prominent-shop-1.20.1.md).

## Appearance

The upgrade window uses the original 178 × 18 stone strip as its draggable
header. SIU controls occupy the right end. The complete original stage panel,
material requirements and upgrade button remain directly underneath, without
a second SIU frame. The full window measures 178 × 135 GUI pixels.

Armor selection keeps its original short colored nameplate, animated carousel,
four selectable armor pieces and Forge button. A 248-pixel copy of the same
stone header sits above it; only the center section extends, preserving both
rounded ends. The entire 248 × 181 interface moves together.

Both headers retain a live native Cinders balance on the left. This relocates
the original display-corner balance into the movable window. Native player
inventory artwork/slots are omitted; SIU's existing Inventory remains separate.
No replacement armor artwork or crafting UI is drawn.

## Native behavior and boundaries

The mod's MenuType constructors and original Owo screens remain in use. Each
menu is attached to its actual owning player through Owo's existing attachment
method. The two original `owo:local_packet` messages are scoped to their owning
authenticated SIU sessions. The bridge checks message type, size, complete
decoding and armor-piece range, then invokes the original Owo decoder and
native action. The native requirements, ingredient consumption, currency,
progression timer, crafting animation and delayed item delivery stay authoritative.
When a successful upgrade requests its native close, a narrowly targeted mixin
invalidates only that detached upgrade menu. SIU's normal validity sweep closes
the owning session; the separate Inventory window remains open. Outside a live
desktop action, the original vanilla close is used unchanged.

These menus have no storage slots of their own, so their hidden player slots
are not exposed to container sorting. The armor menu's original live block
entity and reach checks remain in force.

The Owo root's full-screen dim surface is blanked only for the native screens
owned by this adapter. Owo clipping uses SIU's existing public native render
target dimensions during capture and the real display outside it. This avoids
the half-height materials panel caused by scissors using the display dimensions
while rendering into a smaller texture. Core and its public API are unchanged.
The armor screen's two absolutely positioned Owo components are also recentered
when GUI dimensions change: its invisible four-piece hit area and native Forge
button follow the same original centering formulas. This keeps clicks aligned
and prevents the button disappearing after a live GUI-scale change.

## Test fixture

The focused fixture runs a production-namespace Fabric client with original
audited release jars, Core 0.1.6 and Compatibility 0.1.3. It stages the unmodified
embedded Base jar separately from its parent modpack initializer. Two original
parent data definitions are restored byte-for-byte in an excluded data-only
fixture resource jar. These resources are not shipped with Compatibility.

The forge's material providers are the original audited Paladins, MC Dungeons
Armors, BetterNether, BetterEnd and Chipped releases and required dependencies.
No native code, UI texture, recipe or requirement is patched for the fixture.
An excluded observer reads state; operator commands seed disposable progression
and items. Native button actions and SIU controls are exercised through screen
pointer, click, scroll and drag input.

Focused results and screenshots are recorded in `output/prominent-forge-implemented`.
The focused checks cover the two dedicated templates, dragging, all four armor
piece selections, side-click and wheel carousel navigation, native missing-item
refusal, a successful stage upgrade (all actual ingredients consumed, original
60-second timer started and only the forge session closed), and a successful
Lightbringer helmet craft (all five ingredients consumed, 150 Cinders spent,
the original delayed animation and the actual helmet received). The original
screen recalculates recipe availability when a piece is selected; the test
reselects after seeding its ingredients, matching that native behavior.

Visual checks use GUI scales 2 and 3, with a separate Inventory open. Close and
lock controls are checked on the native headers. Fabric remapping and Forge
reobfuscation pass; Forge is only an optional-mod build/isolation check here,
not a claim that this embedded Fabric release runs on Forge.

This is not a complete functional/gameplay suite or dedicated-server multiplayer
qualification. Other mod releases, resource packs, all 17 armor recipes,
controller navigation and the separately implemented trader are outside these forge checks.
The disposable single-block fixture does not qualify full multiblock
construction or the surrounding gameplay progression.
