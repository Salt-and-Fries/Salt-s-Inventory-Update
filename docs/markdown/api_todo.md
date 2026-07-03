# Salt Desktop API ToDo

This file tracks modder-requested API improvements and compatibility pain points before a larger API pass. Keep the main API guide focused on current behavior; add future-facing requests here until there is enough feedback to design the next batch cleanly.

## Intake Format

For each request, record:

- Mod or use case
- What the author is trying to build
- Current workaround
- Desired API or docs change
- Risk level: docs-only, helper-only, or public API surface

## Candidate API Features

### Cropped Background Helper

Use case: furnace-like or machine-like menus often have one vanilla-style GUI texture where the machine UI is above the 4x9 player inventory. Salt windows should not render the duplicated player inventory, so authors want to reuse only the machine portion of that PNG inside Salt's window frame.

Current workaround: call `DesktopRenderContext.texture(...)` with a source rectangle, then render real slots with `texturelessSlot(...)` if the cropped texture already includes slot backgrounds.

Possible improvement: add a clearer helper such as `DesktopWidgets.renderTextureRegion(...)`, `DesktopWidgets.renderCroppedBackground(...)`, or a shorter `DesktopRenderContext.textureRegion(...)` overload.

Risk: helper-only unless the render context method set changes.

### Clipping Or Scissor Helper

Use case: scrollable panels, recipe lists, terminal grids, and cropped texture areas sometimes need child content clipped to a rectangle.

Current workaround: avoid drawing outside the panel or manually calculate visible rows.

Possible improvement: expose a scoped clipping helper in the render context so API windows can safely clip custom content without touching Minecraft internals.

Risk: public API surface.

### Standard Machine Widgets

Use case: furnace-like menus commonly need arrows, flames, energy bars, fluid bars, and recipe-book buttons.

Current workaround: draw custom texture regions manually and send menu buttons or payloads for interactions.

Possible improvement: provide optional widget helpers for progress arrows, vertical meters, recipe-book buttons, and common hover states.

Risk: helper-only, but needs careful version testing around recipe-book components.

### Container Slot Convenience Helpers

Use case: mod authors often think in container slot indexes while Salt needs menu slot ids for click routing.

Current workaround: use `containerSlot(...)`, `containerSlotHit(...)`, or `menuSlotId(...)`, then render with `slot(Slot, x, y)` or `texturelessSlot(menuSlotId, x, y)`.

Possible improvement: add explicit render helpers for container-slot indexes, such as `renderContainerSlot(containerSlotIndex, x, y)` and `texturelessContainerSlot(containerSlotIndex, x, y)`.

Risk: helper-only.

### Integration Diagnostics

Use case: authors need to know why a menu was not captured, why a slot hit was ignored, or why a payload failed.

Current workaround: enable Salt's detailed desktop logging and inspect logs.

Possible improvement: add a small diagnostics checklist command or API-visible debug messages for registration, server support, slot hit validation, and payload dispatch.

Risk: docs/helper only unless a structured diagnostics API is exposed.

## Documented In Current Guide

- Compatibility limits and common showstoppers.
- Rework checklist for existing menus and screens.
- Cropped background example using `texture(...)` plus `texturelessSlot(...)`.
