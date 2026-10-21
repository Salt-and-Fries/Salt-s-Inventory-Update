"""Validate packaged in-world evidence against every exact controller target/pin."""
import argparse
from collections import Counter
import hashlib
import importlib.util
import json
import math
import re
import struct
import tomllib
from pathlib import Path
from zipfile import ZipFile
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
LOADER_NAMES = {"fabric": "Fabric", "forge": "Forge", "neoforge": "NeoForge"}


def companion_versions(path, loader, core_version, companion_version, mod_id='salts_inventory_controlify'):
    """Check independent release labels against the actual packaged Core dependency."""
    with ZipFile(path) as archive:
        if loader == "fabric":
            metadata = json.loads(archive.read("fabric.mod.json"))
            assert metadata['id'] == mod_id, f'{path}: incorrect companion identity'
            version = metadata["version"]
            required_core = metadata["depends"]["salts_inventory_update"]
            expected_core = "=" + core_version
        else:
            resource = "META-INF/mods.toml" if loader == "forge" else "META-INF/neoforge.mods.toml"
            metadata = tomllib.loads(archive.read(resource).decode("utf-8"))
            version = next(mod["version"] for mod in metadata["mods"]
                           if mod["modId"] == mod_id)
            dependencies = metadata["dependencies"][mod_id]
            required_core = next(dep["versionRange"] for dep in dependencies
                                 if dep["modId"] == "salts_inventory_update")
            expected_core = f"[{core_version}]"
        assert version == companion_version, f"{path}: packaged and loaded companion versions differ"
        assert required_core == expected_core, f"{path}: packaged companion requires a different Core release"


def host_motion_isolation(report, name):
    """Accept unfiltered successful evidence; validate the bounded opt-in QA policy when disclosed."""
    isolation = report.get("feature26Probe", {}).get("hostSdlMotionIsolation")
    if isolation is None:
        return {"applied": False, "status": "not-applied"}
    game = name.split("-", 1)[0]
    assert game in ("26.1.2", "26.2", "26.3"), f"{name}: host motion isolation is scoped to the modern disposable QA fixtures"
    assert isolation.get("policy") == "disposable-synthetic-controller-motion-only" \
        and isolation.get("scope") == "unfinished-run/current-installed-synthetic-controller/loaded-world-player", \
        f"{name}: host SDL isolation does not disclose its exact disposable test scope"
    assert isolation.get("nativeDirectPointerAndControllerUnchanged") is True \
        and isolation.get("buttonsKeysWheelsForwarded") is True, \
        f"{name}: isolation must retain the native controller/pointer, button, key and wheel routes"
    passed = {row["name"] for row in report.get("results", []) if row["status"] == "PASS"}
    assert "disposable-QA-host-motion-isolation-active" in passed, \
        f"{name}: isolation scope was not verified after installing the actual synthetic controller"
    count, events = isolation.get("blockedMotionEvents"), isolation.get("events")
    assert type(count) is int and count >= 0 and isinstance(events, list) and len(events) == min(count, 64), \
        f"{name}: missing total or bounded SDL motion observations"
    for event in events:
        backend_type = 1024 if game == "26.3" else "glfw_cursor_motion"
        assert event.get("type") == backend_type and isinstance(event.get("scenario"), str) and event["scenario"], \
            f"{name}: isolation recorded a non-motion event or a scenario outside its scope"
        assert all(event.get(key) is True for key in ("syntheticControllerCurrent", "worldPlayerLoaded", "unfinishedRun")), \
            f"{name}: isolated host motion outside the installed disposable controller/world scope"
        native_fields = ("windowId", "which", "timestamp") if game == "26.3" else ("windowId", "timestamp")
        position_fields = ("x", "y", "xrel", "yrel") if game == "26.3" else ("x", "y")
        assert all(type(event.get(key)) is int for key in native_fields), \
            f"{name}: missing native SDL device/window/timestamp metadata"
        assert all(isinstance(event.get(key), (int, float)) and math.isfinite(event[key]) for key in position_fields), \
            f"{name}: missing finite native SDL position/relative motion observations"
    return {"applied": True, "status": "qa-only-disposable-synthetic-controller", "blocked_motion_events": count,
            "recorded_events": len(events), "native_controller_pointer_routes_unchanged": True}


COVERAGE = {
    "permanent windows/default opening": ["inventory-focused-by-default", "new-chest-selected", "mandatory-inventory-character"],
    "native pointer and exact pair transfers": ["stock-cursor-moved-right", "stock-DPAD-transformed-slot-snap", "server-exact-pair-and-conservation"],
    "radial highlight/focus/bumpers": ["joystick-highlight-character", "character-now-focused", "RB-changed-only-selected", "LB-reverses-selection"],
    "gameplay gating": ["no-world-movement-side", "no-world-movement-radial", "actual-world-movement-tucked"],
    "tuck lifecycle/new opening/restore": ["window-lifetimes-preserved", "barrel-added-existing-workspace", "new-window-keeps-gameplay", "restored-side-mode"],
    "individual close and protection": ["only-highlighted-window-closed", "inventory-cannot-close", "character-cannot-close"],
    "configured hold threshold": ["same-core-hold-duration", "side-hold-honors-threshold", "close-all-server-sessions-disposed"],
    "HUD/animation configuration": ["forced-hud-offhand", "disabled-animation-immediate-target"],
    "native result callbacks": ["crafting-native-storage-output-conserved", "furnace-result-conservation-and-XP", "loom-native-inputs-consumed-once-output-replenished"],
    "native/custom UI routing": ["protruding-client-UI-button-clicked", "modal-outside-PRESS-dismissed-once", "client-UI-close-callback-once", "UI-state-identities-not-recreated"],
    "actual controller/server": ["actual-controlify-driver", "real-input-bindings-created", "salt-server-handshake"],
}

# The original 26.3 checks are also required for the modern ports under --feature-parity.
FEATURE_COVERAGE_26_3 = {
    "native close completes on the first configured input edge": [
        "radial-B-closes-on-first-processed-configured-edge",
        "radial-B-target-closed-before-native-handler-return",
        "side-B-closes-on-first-processed-configured-edge",
        "side-B-target-closed-before-native-handler-return"],
    "companion sound settings native controls": [
        "sound-overhaul-companion-entry-native-footer-visible", "sound-overhaul-default-off-hover-silent",
        "sound-overhaul-native-standard-opt-in-audible", "sound-overhaul-native-quiet-opt-in-audible",
        "sound-overhaul-native-settings-scroll-and-narration", "sound-overhaul-native-slider-click-accepted",
        "sound-overhaul-native-slider-updates-live-custom-mix", "sound-overhaul-custom-mix-persists",
        "sound-overhaul-small-viewport-scroll-keeps-footer-clear", "sound-overhaul-scrolling-reaches-last-native-setting",
        "sound-overhaul-restore-defaults-off-hover-off", "sound-overhaul-native-footer-resize-preserves-two-controls",
        "sound-overhaul-original-native-screen-restored"],
    "companion sounds default off before explicit opt-in": [
        "sound-overhaul-new-file-default-off", "sound-overhaul-new-file-off-no-native-dispatch",
        "sound-overhaul-audio-tests-explicitly-opt-in-quiet"],
    "companion sound quiet minimal and muted mixes": [
        "sound-overhaul-minimal-mutes-browsing-keeps-actions", "sound-overhaul-minimal-navigation-request-silent",
        "sound-overhaul-minimal-action-request-retained", "sound-overhaul-minimal-only-action-dispatched",
        "sound-overhaul-mod-master-request-suppressed", "sound-overhaul-mod-master-no-native-dispatch",
        "sound-overhaul-disabled-hover-request-suppressed", "sound-overhaul-disabled-hover-no-native-dispatch"],
    "semantic sound priority and isolated preview": [
        "sound-overhaul-result-queued-over-navigation", "sound-overhaul-same-tick-one-result-no-navigation",
        "sound-overhaul-preview-exactly-one-native-cue", "sound-overhaul-preview-stop-clears-only-preview"],
    "rapid navigation interrupts native audio": [
        "sound-overhaul-mod-master-fast-navigation-suppressed", "sound-overhaul-every-fast-update-native-cue",
        "sound-overhaul-fast-navigation-three-cues-under-200ms", "sound-overhaul-fast-navigation-stops-previous-before-play",
        "sound-overhaul-fast-navigation-keeps-result-priority-policy", "sound-overhaul-fast-navigation-no-idle-repeats"],
    "native framed contextual pair": ["contextual-live-destination-below-ingredients", "contextual-panels-use-two-actual-Core-native-frames",
        "contextual-destination-real-native-planks-slot", "contextual-destination-real-native-cake-slot",
        "contextual-character-real-native-destination-slot", "contextual-recipe-cards-native-layer-below-opening",
        "contextual-source-and-destination-native-pair-rendered"],
    "textured live contextual popout": ["contextual-textured-opening-shares-native-source-frame", "contextual-opening-starts-at-actual-side-source",
        "contextual-transition-fixture-explicitly-enables-animations", "contextual-transition-fixture-restores-prior-animation-setting",
        "contextual-transition-config-restore-keeps-live-native-pair",
        "contextual-settled-materials-wheel-live-container-order", "contextual-native-container-uses-moving-input-pose",
        "contextual-procedural-guard-positive-control", "contextual-same-release-native-A-addresses-moving-slot",
        "contextual-same-release-native-cursor-still-enabled", "contextual-release-reverses-shared-live-frame",
        "contextual-same-release-LB-selects-exact-neighbor",
        "contextual-LT-replaces-reverse-without-stale-overlay", "contextual-popout-reopen-retains-original-side-pair",
        "contextual-every-custom-shape-is-native-decoded-PNG", "contextual-card-tab-category-letter-texture-states-rendered",
        "contextual-textured-ring-highlight-fixed-UV-sector-tracks-selection", "contextual-overlay-no-procedural-shape-fallback",
        "contextual-live-source-transitions-and-native-draws-valid"],
    "creative cursor clearing and hotbar-only deletion": ["creative-cursor-A-cleared-without-catalog-grant", "creative-cursor-Y-cleared-without-catalog-grant", "creative-Y-delete-preserves-carried-and-other-hotbar", "creative-hotbar-delete-guide-native-Y", "creative-Y-other-window-retains-ordinary-transfer", "creative-inventory-tab-Y-hotbar-retains-ordinary-transfer", "creative-inventory-tab-Y-storage-retains-ordinary-transfer"],
    "composed native controller glyphs": ["native-guide-ordinary-chest-single-whole-dpad-icon", "native-guide-ordinary-chest-adjacent-bumpers-clean-font-spacing"],
    "bounded Craft Stack native transactions": ["contextual-Craft-Stack-64-native-cap-and-conservation", "contextual-Craft-Stack-16-native-cap-and-conservation", "contextual-Craft-Stack-1-native-cap-and-conservation", "contextual-Craft-Stack-whole-batches-native-cap-and-conservation", "contextual-Craft-Stack-limited-supply-native-cap-and-conservation", "contextual-Craft-Stack-limited-destination-native-cap-and-conservation", "contextual-Craft-Stack-64-old-Y-no-craft", "contextual-Craft-Stack-64-retains-live-preview"],
    "compact horizontal native controls": ["compact-native-guide-short-horizontal-items", "compact-native-cursor-uses-one-plain-stick-glyph", "compact-native-recipe-sticks-have-one-undirected-glyph-each", "compact-guide-constrained-two-rows-small-band", "compact-guide-resize-preserves-native-pair", "compact-native-guides-all-modes-with-plain-stick-glyphs"],
    "textured animated tuck": ["textured-tuck-normal-frame-native-blit", "textured-tuck-selected-frame-native-blit", "textured-tuck-both-atlas-frames-decoded-distinct", "textured-tuck-intermediate-animation-observed", "textured-tuck-native-highlight-shrinks-smoothly", "textured-tuck-native-selection-returns-before-release"],
    "associated inactive native item previews": ["active-SIDE-pair-render-full-native-menus", "inactive-SIDE-previews-render-associated-native-items", "active-LT-focus-and-highlight-render-full-native-menus", "inactive-LT-previews-render-associated-native-items"],
    "alphabetic continuous recipe ring": ["alphabet-native-catalog-is-localized-name-order", "alphabet-catalog-retains-every-native-recipe-id", "alphabet-markers-only-present-initials-at-first-recipe", "alphabet-actual-native-labels-match-current-catalog", "alphabet-actual-labels-separated-and-content-bounded", "alphabet-anchor-first-recipe-native-ticks-preserved"],
    "subtle constrained native UI sounds": ["workspace-ui-muted-native-action-still-operates", "workspace-ui-native-source-mute-suppresses-play", "workspace-ui-native-master-mute-suppresses-play", "workspace-ui-mute-respects-native-preferences", "workspace-ui-neutral-wheel-no-repeating-sounds", "workspace-ui-identical-rejection-is-quiet", "workspace-ui-already-active-tab-is-quiet", "workspace-ui-appropriate-native-actions-have-subtle-sounds", "workspace-ui-native-sounds-global-and-action-rate-limited", "workspace-ui-native-sound-source-volume-pitch-constrained"],
    "compact inventory": ["workspace-all-player-storage-and-hotbar-visible", "workspace-hotbar-gap-is-compact", "workspace-hotbar-inside-compact-inventory"],
    "deferred tooltip and adaptive native cursor": ["deferred-item-tooltip-at-screen-cursor", "stock-cursor-independent-chest-slot-scale", "actual-native-cursor-uses-guide-pane-scale", "guide-and-inventory-require-distinct-cursor-scales", "actual-native-cursor-changes-to-inventory-slot-scale"],
    "recipe guide fitting and native controls": ["guide-opened-through-native-controller-click", "guide-expanded-fitted-panes-bounded", "guide-expanded-fitted-panes-separate", "native-DPAD-guide-toggle", "native-DPAD-guide-filter", "native-DPAD-guide-search", "native-DPAD-guide-category", "native-DPAD-guide-next-page", "native-DPAD-guide-recipe", "native-DPAD-expanded-guide-choice", "actual-expanded-recipe-choices-populated", "native-recipe-choice-click-consumed", "guide-choice-fitted-panes-bounded", "guide-native-toggle-closed", "legacy-native-guide-compatible-with-contextual-opt-out"],
    "persistent tucked names": ["tabs-rendered-after-transition", "persistent-tucked-tabs-every-live-window-name", "disabled-animation-tabs-rendered", "disabled-animation-tabs-permanent-inventory-and-character", "new-container-tucked-tab-every-live-window-name"],
    "native all-container normal and force sort": ["sort-source-is-middle-focused-chest", "normal-sort-matches-beyond-displayed-pair", "double-Y-native-edge-within-threshold", "force-sort-moves-unmatched-to-empty-open-container-conserves-all"],
    "radial single-window close and immediate side close": ["only-highlighted-window-closed", "radial-B-closes-highlighted-immediately", "radial-held-B-after-closable-does-not-close-all", "radial-held-B-does-not-close-all", "radial-held-B-protects-mandatory-windows", "radial-rebound-close-owns-A", "side-close-all-returned-gameplay", "close-all-server-sessions-disposed", "side-B-closes-immediately", "side-B-returned-gameplay"],
    "fresh opening trigger and first interaction": ["first-controller-sample-retains-opening-LT-latch", "first-workspace-B-cleanly-returns-gameplay", "first-world-interaction-reopens-after-close", "opening-held-LT-stays-side", "opening-LT-release-retains-side", "fresh-LT-after-opening-enters-radial"],
    "large focus and neighboring previews": ["ghost-fixture-has-many-native-and-client-windows", "ghost-animation-round-trip-keeps-selection", "radial-highlight-much-larger-than-thumbnails", "radial-focus-much-larger-than-thumbnails"],
    "logical brewing navigation": ["brewing-staggered-middle-bottle-reached", "brewing-next-bottle-reached"],
    "creative hotbar actions": ["creative-A-one-item-no-carried-stack", "creative-Y-legal-stack-no-carried-item", "creative-full-hotbar-preserves-existing-stacks", "native-guide-creative-stack-rebound-labels-and-glyphs", "native-guide-creative-stack-actual-rebound-glyph", "creative-rebound-old-Y-no-hotbar-mutation", "creative-rebound-inventory-action-adds-legal-stack", "creative-remap-keeps-native-left-click-SOUTH", "creative-remap-keeps-native-shift-click-NORTH", "native-guide-creative-select-rebound-labels-and-glyphs", "native-guide-creative-select-actual-rebound-glyph", "creative-rebound-old-SOUTH-no-hotbar-mutation", "creative-rebound-select-action-adds-exactly-one"],
    "creative list and right-stick scroll": ["creative-catalog-has-rigid-items-and-tabs", "creative-DPAD-scrolls-one-row-before-hotbar", "creative-DPAD-scroll-keeps-cursor-in-catalog", "creative-right-stick-drift-does-not-scroll", "creative-right-stick-scroll-rate-controlled",
        "creative-right-stick-full-deflection-faster-without-pause", "creative-right-stick-neutral-release-no-stale-repeat",
        "creative-right-stick-gentle-deflection-scrolls", "creative-right-stick-deflection-controls-speed",
        "creative-right-stick-proportional-release-no-stale-repeat"],
    "native creative Search keyboard and rebinding": [
        "creative-search-public-api-selects-search-tab", "creative-search-guide-uses-native-X-glyph",
        "creative-search-X-opens-native-KeyboardWidget", "creative-search-opening-X-does-not-backspace-query",
        "creative-search-native-OSK-updates-Core-query", "creative-search-native-query-filters-real-catalog",
        "creative-search-physical-text-path-updates-query", "creative-search-native-backspace-edits-query",
        "creative-search-A-field-uses-same-native-keyboard", "creative-search-guide-follows-rebound-native-glyph",
        "creative-search-old-X-does-not-open", "creative-search-rebound-opens-native-keyboard",
        "creative-search-wrong-tab-does-not-open", "creative-search-wrong-window-does-not-open",
        "creative-search-neutral-restores-normal-catalog-navigation", "creative-search-query-cleared-and-tab-restored"],
    "native virtual mouse simultaneous creative Search Enter clicks": [
        "creative-search-VM-Enter-native-keyboard-open", "creative-search-VM-A-X-Enter-closes-keyboard-only",
        "creative-search-VM-A-X-both-native-click-bindings",
        "creative-search-VM-Enter-preserves-workspace-windows-and-sessions", "creative-search-VM-Enter-preserves-workspace-items-and-cursor",
        "creative-search-VM-Enter-preserves-workspace-no-sort-craft-drop-or-grant",
        "creative-search-VM-Enter-neutral-clears-physical-quarantine", "creative-search-VM-Enter-neutral-clears-native-mouse-drag",
        "creative-search-VM-Enter-neutral-restores-catalog-navigation"],
    "creative Search keyboard owns input until neutral": [
        "creative-search-outside-mouse-and-scroll-consumed", "creative-search-keyboard-isolates-underlying-input-windows-and-sessions",
        "creative-search-keyboard-isolates-underlying-input-items-and-cursor", "creative-search-keyboard-isolates-underlying-input-no-sort-craft-drop-or-grant",
        "creative-search-B-returns-exact-parent", "creative-search-held-triggers-wait-for-neutral",
        "creative-search-held-exit-isolates-underlying-input-windows-and-sessions", "creative-search-held-exit-isolates-underlying-input-items-and-cursor",
        "creative-search-held-exit-isolates-underlying-input-no-sort-craft-drop-or-grant",
        "creative-search-neutral-exit-preserves-workspace-windows-and-sessions", "creative-search-neutral-exit-preserves-workspace-items-and-cursor",
        "creative-search-neutral-exit-preserves-workspace-no-sort-craft-drop-or-grant", "creative-search-keyboard-server-items-and-cursor-preserved"],
    "crafting contextual radial": ["contextual-source-is-hovered-crafting-table", "contextual-all-unlocked-recipes-in-continuous-carousel", "contextual-ingredient-container-open-but-not-displayed", "contextual-wheel-real-native-ingredient-count", "contextual-category-next-native-binding", "contextual-category-row-renders-every-category", "contextual-craftable-filter-enabled-native-binding", "contextual-one-craft-ingredients-output-cursor-conserved", "contextual-noncraftable-no-inventory-mutation", "contextual-full-destination-no-consumption-no-leak", "contextual-native-cake-output-remainders-conserved", "contextual-release-same-side-pair", "contextual-release-native-cursor-preserved"],
    "continuous recipe carousel": ["contextual-right-stick-addresses-individual-recipe", "contextual-right-stick-moves-one-adjacent-recipe", "contextual-right-stick-continuous-fractional-position", "contextual-carousel-visible-adjacent-window", "contextual-left-stick-selects-displayed-adjacent-card", "contextual-held-RB-five-recipes-per-second", "contextual-held-LB-five-recipes-per-second", "contextual-held-bumpers-keep-side-pair", "contextual-fixed-right-stick-preserves-fine-offset", "contextual-right-stick-inward-return-does-not-shift-wheel", "contextual-recipe-large-deadzone-retains-outer-and-inner-selection", "contextual-left-stick-inward-return-does-not-shift-selection"],
    "live recipe hover and origin conservation": ["contextual-enter-clears-input-without-granting-derived-result", "contextual-hover-physically-stages-from-hidden-open-container", "contextual-hover-change-restores-unused-exact-origin", "contextual-release-restores-unused-exact-origin", "contextual-hover-shapes-available-ingredients-across-three-open-sources", "contextual-missing-ghosts-red-translucent-over-real-native-inputs", "contextual-hover-LT-interruption-restores-exact-components-and-origin", "contextual-hover-full-origin-safe-fallback-without-loss-or-duplication", "contextual-hover-source-close-restores-before-native-menu-disposal", "contextual-full-storage-rejected-preview-closes-native-wheel", "contextual-full-storage-rejected-preview-preserves-input-and-derived-output", "contextual-full-storage-fixture-covers-all-actual-eligible-slots", "contextual-full-return-fixture-covers-all-actual-eligible-slots", "contextual-hover-all-full-return-preserves-one-native-item-entity"],
    "native cooking hover lifecycle": ["contextual-cooking-enter-clears-owned-input-output-and-preserves-fuel", "contextual-cooking-cleared-real-output-retains-native-XP-callback", "contextual-cooking-hover-borrows-input-from-exact-origin", "contextual-cooking-hover-native-consumption-before-refill", "contextual-cooking-consumed-unit-not-returned-as-same-variant-refill"],
    "hover authenticated interruption and recovery": ["contextual-hover-genuinely-stale-click-rejected-before-preview-cleanup", "contextual-hover-genuinely-stale-click-retains-active-native-wheel", "contextual-hover-stale-resync-retains-native-window-and-side-pair", "contextual-hover-first-interrupting-click-applies-and-restores", "contextual-hover-first-interrupting-quickmove-applies-and-restores", "contextual-hover-stale-BEGIN-rejected-inactive-and-closes-wheel", "contextual-hover-stale-BEGIN-held-RT-does-not-repeat-BEGIN", "contextual-hover-stale-BEGIN-does-not-mutate-owned-items", "contextual-hover-stale-BEGIN-safe-native-retry-physical-native-input", "contextual-full-storage-held-RT-does-not-repeat-BEGIN", "contextual-hover-real-server-lease-expiry-closes-wheel", "contextual-hover-expired-lease-restores-exact-origin-on-native-tick"],
    "recipe wheel alphabet and layers": ["alphabet-aim-first-letter-displays-first-recipe-at-top", "alphabet-aim-middle-letter-displays-first-recipe-at-top", "alphabet-native-labels-use-inside-and-outside-lanes", "contextual-navigation-native-layer-above-opening", "contextual-world-dimming-preserved-through-all-wheel-modes", "contextual-body-text-readable-on-native-window-chrome"],
    "native bottom guide and hovered remapped recipe action": ["native-guide-supported-table-labels-and-glyphs", "native-guide-table-result-labels-and-glyphs", "contextual-result-slot-activates-hovered-wheel", "native-guide-table-background-labels-and-glyphs", "contextual-background-activates-hovered-wheel", "native-guide-unsupported-barrel-labels-and-glyphs", "native-guide-quick-action-hidden-off-supported-pane", "contextual-unsupported-pane-does-not-activate-wheel", "contextual-rebound-native-guide-actual-binding-glyph", "contextual-rebound-old-RT-no-longer-opens-wheel", "contextual-rebound-action-opens-hovered-wheel", "native-guide-rebound-recipe-wheel-labels-and-glyphs", "native-guide-screen-preference-off-no-render", "native-guide-screen-preference-restored-labels-and-glyphs"],
    "native workspace guide modes and conditional actions": ["native-guide-opening-release-first-labels-and-glyphs", "native-guide-opening-LT-suppresses-hold", "native-guide-opening-release-cleared-labels-and-glyphs", "native-guide-ordinary-chest-labels-and-glyphs", "native-guide-native-item-actions-and-glyphs", "native-guide-radial-protected-labels-and-glyphs", "native-guide-protected-highlight-hides-close", "native-guide-radial-tuck-labels-and-glyphs", "native-guide-radial-closable-sort-labels-and-glyphs", "native-guide-rebound-close-protected-labels-and-glyphs", "native-guide-close-owned-focus-action-hidden", "native-guide-craftable-recipe-labels-and-glyphs", "native-guide-unavailable-recipe-keeps-craft-and-stack", "native-guide-creative-catalog-labels-and-glyphs", "native-guide-creative-tabs-labels-and-glyphs", "native-guide-creative-tab-both-sticks-glyphs"],
    "native gameplay guides around tucked previews": ["native-guide-tucked-top-columns-clear-tabs-and-previews", "native-guide-tucked-restore-uses-inventory-glyph", "native-guide-tucked-transition-columns-clear-preview", "native-guide-bottom-preference-keeps-original-pose", "native-guide-gameplay-preference-off-no-render", "native-guide-hidden-HUD-no-render", "native-guide-tucked-preferences-restored-native-render", "native-guide-outside-tuck-restores-native-pose-and-inventory"],
    "removed pane icon and manual control instructions": ["native-guide-removed-pane-book-never-drawn", "native-guide-old-manual-controls-never-drawn", "native-guide-main-panes-clear-bottom-lists", "native-guide-layout-viewport-is-observed-content-area", "native-guide-contextual-surfaces-clear-bottom-lists", "native-guide-tall-fixture-is-actually-tall", "native-guide-tall-window-labels-and-glyphs", "native-guide-tall-window-native-close-isolated", "native-guide-tall-cleanup-preserves-saved-source", "native-guide-tall-cleanup-restores-exact-pair"],
    "controller native recipe-book replacement": ["legacy-opt-out-renders-native-vanilla-book-toggle", "contextual-presentation-hides-vanilla-book-sprites-including-furnace-ghosts"],
    "Character native 2x2 crafting": ["contextual-character-native-2x2-station", "contextual-character-excludes-3x3-recipes", "contextual-character-open-container-ingredient-count", "contextual-character-native-craft-output-conserved"],
    "creative contextual radial": ["creative-RT-wheel-kind", "creative-RT-stick-selected-tab", "creative-RT-freezes-underlying-cursor", "creative-RT-DPAD-selected-tab", "creative-RT-native-A-selected-real-tab", "creative-RT-release-retains-side-workspace", "creative-RT-active-native-tab-rendered", "creative-RT-active-highlight-distinct-from-hover", "creative-RT-active-highlight-follows-native-selection", "creative-RT-no-tab-selected-status"],
}
for station in ("furnace", "blast-furnace", "smoker"):
    FEATURE_COVERAGE_26_3[f"native cooking guides {station}"] = [f"native-guide-cooking-hover-{station}-labels-and-glyphs", f"native-guide-cooking-{station}-labels-and-glyphs"]
    FEATURE_COVERAGE_26_3[f"native contextual {station}"] = [
        f"contextual-{station}-{proof}" for proof in (
            "native-cooking-wheel", "native-empty-input-output-preview", "open-only-input-count",
            "native-input-fuel-loaded-from-open-container", "real-furnace-progress-preview",
            "native-processing-produces-output", "actual-output-preview",
            "actual-live-native-menu-input-progress", "actual-live-native-menu-output-slot",
            "hover-stages-input-without-taking-fuel", "consumed-hover-input-is-not-restored",
        )
    ]
FEATURE_CAPTURES_26_3 = {
    "46-creative-search-native-osk",
    "45-controller-sound-settings", "45b-controller-sound-settings-constrained",
    "43-native-red-translucent-missing-ingredients", "44-letter-selection-centers-first-recipe",
    "42-native-destination-crafted-planks", "42b-native-destination-crafted-cake", "42c-character-native-destination-output",
    "41a-textured-wheel-opening-live-source", "41b-textured-wheel-settled-native-container", "41c-textured-wheel-reverse-live-container",
    "37-compact-guide-native-item-actions", "37b-compact-guide-constrained-viewport", "37c-compact-guide-recipe-plain-sticks",
    "38-textured-tuck-normal", "38b-textured-tuck-selected", "38c-textured-tuck-shrink-transition",
    "39-associated-item-icons-side-previews", "39b-associated-item-icons-LT-wheel", "40-alphabetic-recipe-groups",
    "40b-alphabet-labels-separated-and-anchored",
    "31-continuous-recipe-carousel-category-row", "31a-selected-category-header-row",
    "31b-held-RB-fine-carousel-motion", "32-native-guide-supported-table",
    "32b-native-guide-off-supported-pane", "32c-native-guide-rebound-recipe-action",
    "33-character-native-2x2-recipe-wheel",
    "35-tucked-native-gameplay-top", "35b-tucked-native-gameplay-bottom",
    "35c-tucked-native-guide-preferences-restored", "35d-native-gameplay-restored-position",
    "36-tall-window-native-bottom-guide",
    *(f"{prefix}-{station}-{suffix}" for station in ("furnace", "blast-furnace", "smoker")
      for prefix, suffix in (("34", "native-cooking-wheel"), ("34b", "live-native-progress"), ("34c", "processed-native-output"))),
}

CONTEXTUAL_TEXTURES_26_3 = {
    "BACKDROP": (760,450),
    **{name: (32,32) for name in ("RECIPE_CARD", "UNAVAILABLE_RECIPE_CARD", "HOVERED_CARD", "CATEGORY", "SELECTED_CATEGORY",
        "CREATIVE_TAB", "HOVERED_CREATIVE_TAB", "ACTIVE_CREATIVE_TAB")},
    "WHEEL_RING": (288,288), "WHEEL_RING_HIGHLIGHT": (288,288), "CAROUSEL_OPENING": (160,90),
    "ALPHABET_LABEL": (10,12), "HIGHLIGHTED_ALPHABET_LABEL": (10,12), "ALPHABET_ANCHOR": (2,2),
    "ALPHABET_LEADER": (1,1),
}


# This inventory is independent of the production enum/probe: changed code cannot
# silently redefine its own expected event, priority, gain or cadence contract.
WORKSPACE_SOUND_ACTIONS_26_3 = {
    "OPEN": ("open", "STRUCTURE", 160, .35),
    "CLOSE": ("close", "STRUCTURE", 160, .35),
    "FOCUS": ("navigate", "NAVIGATION", 200, .55),
    "CYCLE": ("window_change", "STRUCTURE", 100, .35),
    "TUCK": ("tuck", "STRUCTURE", 160, .35),
    "RESTORE": ("restore", "STRUCTURE", 160, .35),
    "SCROLL": ("navigate", "NAVIGATION", 0, .55),
    "NAVIGATE": ("navigate", "NAVIGATION", 200, .55),
    "CAROUSEL": ("navigate", "NAVIGATION", 0, .55),
    "HOVER": ("navigate", "NAVIGATION", 200, .55),
    "CATEGORY": ("window_change", "STRUCTURE", 120, .35),
    "FILTER": ("window_change", "STRUCTURE", 140, .35),
    "CONFIRM": ("confirm", "RESULT", 120, .4),
    "TAB": ("window_change", "STRUCTURE", 120, .35),
    "SORT": ("window_change", "STRUCTURE", 160, .35),
    "REJECT": ("unavailable", "RESULT", 260, .4),
    "CRAFT_COMPLETE": ("craft_complete", "RESULT", 160, .4),
    "REQUEST": ("window_change", "STRUCTURE", 160, .35),
}


def workspace_ui_sound_evidence(feature_probe, name):
    """Validate actual native emissions, with preview outside the real policy lane."""
    sounds = feature_probe.get("workspaceUiNativeSounds", [])
    assert sounds and feature_probe.get("workspaceUiSoundSuppressedRequests", 0) >= 4, \
        f"{name}: missing native quiet/mute sound observations"
    accepted = Counter()
    real = Counter()
    events = set()
    generations = set()
    previews = []
    previous_generation = previous_at = None
    policy_generation = previous_any = previous_navigation = previous_result = previous_priority = None
    previous_kinds = {}
    for sound in sounds:
        kind, at = sound.get("action"), sound.get("atNanos")
        assert kind in WORKSPACE_SOUND_ACTIONS_26_3, f"{name}: unknown native workspace sound action {kind}"
        event, priority, interval, gain_cap = WORKSPACE_SOUND_ACTIONS_26_3[kind]
        preview, generation = sound.get("preview"), sound.get("generation")
        volume, pitch = sound.get("volume"), sound.get("pitch")
        assert type(at) is int and type(generation) is int and generation >= 0 and type(preview) is bool \
            and (previous_generation is None or generation >= previous_generation) \
            and (previous_at is None or at >= previous_at), \
            f"{name}: missing/out-of-order actual native sound timestamp, generation or preview metadata"
        assert sound.get("identifier") == "salts_inventory_controlify:ui." + event \
            and sound.get("priority") == priority and sound.get("measurement") == "native-request-fields" \
            and sound.get("source") == "UI" and sound.get("relative") is True and sound.get("looping") is False \
            and type(sound.get("delay")) is int and sound["delay"] == 0 \
            and type(volume) in (int, float) and math.isfinite(volume) and 0 < volume <= gain_cap + .000001 \
            and type(pitch) in (int, float) and math.isfinite(pitch) and abs(pitch - 1) <= .000001, \
            f"{name}: inappropriate semantic native workspace sound: {sound}"
        assert isinstance(sound.get("scenario"), str) and sound["scenario"], \
            f"{name}: native sound is missing its actual gameplay scenario"
        accepted[kind] += 1
        previous_at, previous_generation = at, generation
        if preview:
            previews.append(sound)
            # Preview stops its own prior instance and bypasses policy. It neither
            # consumes real cooldowns nor resets real history within a generation.
            continue
        events.add(sound["identifier"])
        generations.add(generation)
        real[kind] += 1
        if policy_generation != generation:
            policy_generation = generation
            previous_any = previous_navigation = previous_result = previous_priority = None
            previous_kinds = {}
        # As in the native probe, allow 2 ms for integer-millisecond policy clocks
        # and measurement at emit HEAD. Only SCROLL/CAROUSEL can bypass ordinary
        # navigation/quiet spacing following another navigation cue.
        assert kind not in previous_kinds or at - previous_kinds[kind] >= (interval - 2) * 1_000_000, \
            f"{name}: per-action native sound cadence exceeded: {kind}"
        if priority == "RESULT":
            # Results may preempt a browsing/structural cue immediately. Another
            # result still needs its own 90 ms family gap and action-specific gap.
            assert previous_result is None or at - previous_result >= 88_000_000, \
                f"{name}: native result sound family cadence exceeded"
            previous_result = at
        else:
            fast = priority == "NAVIGATION" and interval == 0
            assert previous_any is None or fast and previous_priority == "NAVIGATION" or at - previous_any >= 88_000_000, \
                f"{name}: native quiet-lane cadence exceeded"
            if priority == "NAVIGATION":
                assert fast or previous_navigation is None or at - previous_navigation >= 198_000_000, \
                    f"{name}: ordinary native navigation cadence exceeded"
                previous_navigation = at
        previous_any = at
        previous_priority = priority
        previous_kinds[kind] = at
    expected_events = {"salts_inventory_controlify:ui." + row[0] for row in WORKSPACE_SOUND_ACTIONS_26_3.values()}
    assert events == expected_events and len(events) == 9, \
        f"{name}: all nine semantic events must dispatch during real actions, independently of settings preview"
    assert dict(accepted) == feature_probe.get("workspaceUiSoundAccepted"), \
        f"{name}: native sound records and dispatch counters disagree"
    requests = feature_probe.get("workspaceUiSoundRequests", {})
    assert requests and all(key in WORKSPACE_SOUND_ACTIONS_26_3 and type(value) is int and value > 0 for key, value in requests.items()) \
        and all(requests.get(key, 0) >= count for key, count in real.items()), \
        f"{name}: real native sounds lack matching submitted action requests"
    assert len(previews) == 1 and previews[0]["action"] == "FOCUS" \
        and previews[0]["scenario"] == "sound-overhaul-one-cue-native-preview", \
        f"{name}: isolated settings preview must emit exactly its selected native cue"
    burst = [sound for sound in sounds if sound["scenario"] == "sound-overhaul-rapid-navigation-native-restarts"]
    assert len(burst) == 8 and all(sound["action"] == ("SCROLL" if index % 2 == 0 else "CAROUSEL")
        and sound["preview"] is False for index, sound in enumerate(burst)), \
        f"{name}: every fast native navigation update must emit its fresh cue"
    assert any(burst[index]["atNanos"] - burst[index - 2]["atNanos"] < 200_000_000 for index in range(2, len(burst))), \
        f"{name}: missing actual three-cue burst under 200 ms"
    stops = feature_probe.get("workspaceUiNativeSoundStops", [])
    for before, after in zip(burst, burst[1:]):
        assert type(before.get("instance")) is int and type(before.get("nativeOrder")) is int \
            and type(after.get("nativeOrder")) is int and any(stop.get("instance") == before["instance"]
                and type(stop.get("nativeOrder")) is int and before["nativeOrder"] < stop["nativeOrder"] < after["nativeOrder"]
                and stop.get("scenario") == after["scenario"] for stop in stops), \
            f"{name}: new navigation sound overlapped its previous native instance"
    return {"actual_events": sorted(events), "real_emissions": sum(real.values()), "preview_emissions": len(previews),
            "policy_generations": len(generations), "native_ui_bus": True, "pitch": 1,
            "ordinary_navigation_interval_ms": 200, "fast_navigation_interval_ms": 0,
            "fast_native_updates": len(burst), "stop_before_restart": True, "quiet_interval_ms": 90, "result_interval_ms": 90,
            "measurement_tolerance_ms": 2}


def workspace_ui_sound_resources(jar, name):
    """Bind the native event observations to the collected companion's real OGGs."""
    prefix = "assets/salts_inventory_controlify/"
    inventory = set(jar.namelist())
    for cls in ("WorkspaceUiSounds", "WorkspaceSoundSettingsScreen", "model/UiSoundPolicy", "model/WorkspaceSoundSettings"):
        assert "com/salts_inventory_update/controlify/" + cls + ".class" in inventory, \
            f"{name}: missing companion-owned sound implementation {cls}"
    sounds = json.loads(jar.read(prefix + "sounds.json").decode("utf-8-sig"))
    events = {"ui." + row[0] for row in WORKSPACE_SOUND_ACTIONS_26_3.values()}
    assert set(sounds) == events and len(sounds) == 9, f"{name}: final companion must supply all nine semantic sound events"
    manifest = json.loads(jar.read(prefix + "licenses/ui-sounds/palette-manifest.json").decode("utf-8-sig"))
    recordings = manifest.get("assets", [])
    aliases = manifest.get("event_aliases", [])
    assert len(recordings) == 7 and {"ui." + row.get("name", "") for row in recordings} == events - {"ui.open", "ui.close"} \
        and manifest.get("license") == "CC0-1.0" and manifest.get("auditioned_by_researcher") is False, \
        f"{name}: missing or misleading CC0 sound provenance"
    assert len(aliases) == 2 and {row.get("name") for row in aliases} == {"open", "close"} \
        and isinstance(manifest.get("license_scope"), str) and "event_aliases" in manifest["license_scope"], \
        f"{name}: native open/close aliases must be distinguished from the seven CC0 recordings"
    for alias in aliases:
        event = "ui." + alias["name"]
        assert sounds[event].get("sounds") == [{"name": "minecraft:ui.button.click", "type": "event"}] \
            and alias.get("event_id") == "salts_inventory_controlify:" + event and alias.get("type") == "event" \
            and alias.get("target_event") == "minecraft:ui.button.click" and alias.get("redistributed_audio") is False \
            and alias.get("provider") == "Minecraft 26.3 built-in resources" \
            and alias.get("license") == "Minecraft-provided; no audio redistribution" \
            and alias.get("native_definition") == {"sounds": ["random/click_stereo"]} \
            and alias.get("native_sounds_json_sha1") == "7975165ad4a3c0cb3c05078b914eac0f2c3b5f5f", \
            f"{name}: native open/close event alias or provenance differs from the pinned Minecraft definition"
    expected_resources = set()
    original_hashes = set()
    for recording in recordings:
        event, short = "ui." + recording["name"], recording["name"]
        entries = sounds[event].get("sounds", [])
        assert len(entries) == 1 and isinstance(entries[0], dict) \
            and entries[0].get("name") == "salts_inventory_controlify:ui/" + short \
            and entries[0].get("stream") is False, f"{name}: semantic event must use one nonstreaming OGG: {event}"
        resource = prefix + "sounds/ui/" + short + ".ogg"
        expected_resources.add(resource)
        original_hashes.add(recording["source_sha256"])
        raw = jar.read(resource)
        assert recording.get("derived_resource") == resource and raw.startswith(b"OggS") \
            and hashlib.sha256(raw).hexdigest() == recording.get("derived_sha256") \
            and len(raw) == recording.get("derived_size_bytes") and recording.get("channels") == 1 \
            and recording.get("sample_rate_hz") == 44100 and 0 < recording.get("duration_seconds", 0) <= .25, \
            f"{name}: final cue differs from its measured mono CC0 derivative: {event}"
        if short == "craft_complete":
            assert recording["duration_seconds"] <= .16, f"{name}: craft cue is too long for repeated transactions"
    assert {path for path in inventory if path.startswith(prefix + "sounds/") and path.endswith(".ogg")} == expected_resources, \
        f"{name}: unaccounted UI sound recording in the companion"
    packs = manifest.get("packs", [])
    assert len(packs) == 2, f"{name}: both original CC0 notices must be retained"
    for pack in packs:
        notice = pack.get("packaged_notice", "")
        assert notice.startswith(prefix + "licenses/ui-sounds/") and pack.get("license") == "CC0-1.0" \
            and hashlib.sha256(jar.read(notice)).hexdigest() == pack.get("license_sha256"), \
            f"{name}: original CC0 license notice missing or changed"
    assert prefix + "licenses/ui-sounds/ATTRIBUTION.md" in inventory, f"{name}: missing UI recording attribution"
    for path in inventory:
        assert "audio-source/" not in path and "/originals/" not in path \
            and not path.endswith(("build_palette.py", ".wav", ".aif", ".aiff", ".flac")), \
            f"{name}: audio authoring or PCM source leaked into final production JAR: {path}"
        if path.endswith(".ogg"):
            assert hashlib.sha256(jar.read(path)).hexdigest() not in original_hashes, \
                f"{name}: unmodified authoring original leaked into final production JAR: {path}"


def digest(path, algorithm="sha256"):
    return hashlib.new(algorithm, Path(path).read_bytes()).hexdigest()


def code_source_path(value):
    """Decode the actual native file/Forge union JAR source, excluding sibling profiles."""
    assert isinstance(value, str), "Missing native JAR CodeSource"
    if value.startswith("jar:"):
        value = value[4:]
    parsed = urlsplit(value)
    assert parsed.scheme in {"file", "union"} and not parsed.netloc \
        and not parsed.query and not parsed.fragment, f"Unexpected native CodeSource: {value}"
    path = unquote(parsed.path).replace("\\", "/")
    match = re.fullmatch(r"(.+\.jar)(?:#\d+)?(?:!/)?", path, flags=re.IGNORECASE)
    assert match, f"CodeSource is not an actual top-level JAR: {value}"
    path = match.group(1)
    if re.match(r"^/[A-Za-z]:/", path):
        path = path[1:]
    return Path(path).resolve()


def forge_lineage(results, mods, report_path, with_extension=False):
    """Legacy Forge runs named derivatives of unchanged final SRG artifacts."""
    path = results / "forge-remap-lineage.json"
    lineage = json.loads(path.read_text(encoding="utf-8-sig"))
    assert lineage["schema_version"] == 1 and lineage["operation"] == "SRG_TO_MCP", "Invalid Forge remapping operation"
    mapping = ROOT / "controlify/versions/1.20.1/forge/build/moddev/artifacts/intermediateToNamed.zip"
    assert Path(lineage["mappings"]).resolve() == mapping.resolve() and digest(mapping) == lineage["mappings_sha256"], \
        "Forge remapping used different Minecraft mappings"
    assert digest(lineage["installer_tools"]) == lineage["installer_tools_sha256"], "Forge remapping tool changed"
    assert report_path.stat().st_mtime >= path.stat().st_mtime, "Gameplay report predates Forge remap evidence"
    spec = importlib.util.spec_from_file_location("controlify_forge_remap", ROOT / "tools/remap-controlify-forge-qa.py")
    helper = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(helper)
    records = lineage["artifacts"]
    assert len(records) == len({row["mapped"] for row in records}) == 4 + with_extension, \
        "Forge must remap the exact production/upstream mods, including Extended when installed"
    original_folder = mods.parent / "original-mods"
    originals = {}
    for row in records:
        original, mapped = Path(row["original"]).resolve(), Path(row["mapped"]).resolve()
        assert original.parent == original_folder.resolve() and mapped.parent == mods.resolve() and original.name == mapped.name, \
            "Forge original/derived artifacts must remain in their isolated profile folders"
        assert digest(original) == row["original_sha256"] and digest(original, "sha512") == row["original_sha512"], \
            f"Preserved original Forge artifact changed: {original.name}"
        assert digest(mapped) == row["mapped_sha256"] and digest(mapped, "sha512") == row["mapped_sha512"], \
            f"Mapped Forge runtime artifact changed: {mapped.name}"
        helper.verify_resources(original, mapped)
        originals[mapped.name] = original
    return originals, lineage


def corrected_ui_class(data):
    """Repair only the four misdecoded UI constants; retain every instruction and other class byte."""
    assert data[:4] == b'\xca\xfe\xba\xbe'
    count = struct.unpack_from('>H', data, 8)[0]
    result, offset, index, corrected = bytearray(data[:10]), 10, 1, 0
    replacements = [(bad.encode('utf-8'), good.encode('utf-8')) for bad, good in (
        ('\u00e2\u20ac\u00a2', '\u2022'), ('\u00e2\u20ac\u00a6', '\u2026'))]
    while index < count:
        tag = data[offset]; result.append(tag); offset += 1
        if tag == 1:
            length = struct.unpack_from('>H', data, offset)[0]; offset += 2
            original = value = data[offset:offset+length]; offset += length
            for bad, good in replacements: value = value.replace(bad, good)
            corrected += value != original
            result.extend(struct.pack('>H', len(value))); result.extend(value)
        else:
            lengths = {3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
            assert tag in lengths, f'Unexpected class constant tag: {tag}'
            length = lengths[tag]; result.extend(data[offset:offset+length]); offset += length
            if tag in (5,6): index += 1
        index += 1
    result.extend(data[offset:])
    return bytes(result), corrected


def artifact_match(module,release,staged,allow_ui_text_correction=False,allow_core_description_correction=False):
    final_hash, staged_hash = digest(release), digest(staged)
    if final_hash != staged_hash and module == 'core' and allow_core_description_correction:
        from artifact_lineage import core_description_match
        return core_description_match(release,staged)
    if final_hash != staged_hash and allow_ui_text_correction:
        assert module == 'controlify' and any(f'-{game}-' in release.name for game in ('26.1.2','26.2'))
        member = 'com/salts_inventory_update/controlify/ContextualRadialOverlay.class'
        with ZipFile(staged) as before, ZipFile(release) as after:
            assert before.testzip() is None and after.testzip() is None
            assert set(before.namelist()) == set(after.namelist()), 'UI text correction changed the archive inventory'
            changed = [name for name in before.namelist() if before.read(name) != after.read(name)]
            assert changed == [member], f'Unverified production changes beyond the wheel UI text: {changed}'
            repaired, constants = corrected_ui_class(before.read(member))
            assert constants == 4 and repaired == after.read(member), \
                'UI retry lineage must preserve all class bytes except the four corrected UTF-8 constants'
        return {'mode':'four-wheel-text-constants-only', 'runtime_sha256':staged_hash,'final_sha256':final_hash,
                'changed_member':member, 'corrected_constants':constants, 'all_other_class_and_resource_bytes_identical':True,
                'requires_final_artifact_wheel_retry':True}
    assert final_hash==staged_hash, f'{module}: runtime differs from final collected artifact'
    return {'mode':'exact','runtime_sha256':staged_hash,'final_sha256':final_hash}


def legacy_feature_parity(passed, name):
    required = {
        "api-controls-discovered", "api-no-real-slots", "api-scroll-viewport", "api-native-tab-selected",
        "api-virtual-native-click", "api-scroll-one-native-row", "api-right-stick-native-scroll",
        "api-editor-keyboard-open", "api-editor-own-text-focus", "api-editor-native-typing",
        "api-keyboard-keeps-workspace", "api-keyboard-captures-item-actions", "api-keyboard-restores-input",
        "api-retained-focus-does-not-reopen-keyboard", "api-item-controls-restored",
        "native-warp-echo-observed", "native-warp-retains-controller-cursor",
        "different-native-mouse-motion-forwarded", "different-native-mouse-motion-reveals-cursor",
        "wheel-native-crafting-context", "wheel-counts-other-container-materials", "wheel-clears-existing-grid",
        "wheel-right-stick-selects-recipe", "wheel-crafts-real-native-result", "wheel-output-hotbar-first",
        "wheel-stack-obeys-64-item-limit", "wheel-small-stick-does-not-shift", "wheel-bumper-continuous-rotation",
        "wheel-parts-filter", "wheel-craftable-filter", "wheel-no-items-lost", "wheel-filter-retained",
        "cooking-native-context", "cooking-other-container-supply", "cooking-native-input-and-fuel",
        "creative-tabs-and-catalog-navigation", "creative-A-one-to-hotbar", "creative-real-cursor-before-catalog",
        "creative-catalog-clears-without-grant", "creative-Y-deletes-only-hotbar",
        "creative-shortcut-opens-keyboard", "creative-search-restores-controls",
    }
    missing = {"parity-" + proof for proof in required} - passed
    assert not missing, f"{name}: missing native feature parity evidence: {sorted(missing)}"


def validate(target, results_root, artifacts, run_profile, report_suffix="", with_extension=False, with_optional_mods=False,
             feature_parity=False, allow_completed_main_suite=False, allow_ui_text_correction=False):
    game, loader = target["minecraft"], target["loader"]
    name = f"{game}-{loader}" + report_suffix
    ui_sound_evidence = None
    report_path = results_root / name / "results.json"
    report = json.loads(report_path.read_text(encoding="utf-8-sig"))
    completed_main_only = False
    if report.get("failures") != 0 and allow_completed_main_suite:
        assert feature_parity and game in ('26.1.2','26.2') and report.get('scope','all') == 'all'
        rows = report.get('results', [])
        failed = [(i,row) for i,row in enumerate(rows) if row['status']=='FAIL']
        boundary = [i for i,row in enumerate(rows) if row['name']=='contextual-presentation-native-book-sprite-regression' and row['status']=='PASS']
        assert report.get('failures') == len(failed) == 1 and boundary and failed[0][0] > boundary[-1] \
            and failed[0][1]['name'] == 'compat-api-independent-baseline', \
            f'{name}: failure is outside the independently retried post-suite API setup'
        completed_main_only = True
    assert report.get("failures") == 0 or completed_main_only, f"{name}: gameplay failures reported"
    input_isolation = host_motion_isolation(report, name)
    records = report.get("results", [])
    assert records and all(row["status"] in ("PASS", "CAPTURED", "OBSERVED")
                           or completed_main_only and row['status']=='FAIL' and row['name']=='compat-api-independent-baseline' for row in records), \
        f"{name}: missing or unsuccessful scenario records"
    passed = {row["name"] for row in records if row["status"] == "PASS"}
    for group, names in COVERAGE.items():
        if (game == "26.3" or feature_parity) and group == "configured hold threshold":
            names = ["radial-held-B-does-not-close-all", "side-B-closes-immediately", "close-all-server-sessions-disposed"]
        missing = set(names) - passed
        assert not missing, f"{name}: missing {group} proof: {sorted(missing)}"
    if feature_parity and not game.startswith("26."):
        legacy_feature_parity(passed, name)
    if game == "26.3" or feature_parity and game in ("26.1.2", "26.2"):
        for group, names in FEATURE_COVERAGE_26_3.items():
            missing = set(names) - passed
            assert not missing, f"{name}: missing 26.3 {group} proof: {sorted(missing)}"
        feature_probe = report.get("feature26Probe", {})
        assert all(feature_probe.get(key,0)>0 for key in (
            "contextualPresentationFrames", "contextualOpeningFrames", "contextualSettledFrames", "contextualClosingFrames",
            "contextualLiveSourceFrames", "contextualNativeSourceCalls", "contextualNativeSlotDraws", "contextualNativeSourceItemDraws",
            "contextualNativeCookingFrames", "contextualNativeCookingLitFrames", "contextualNativeCookingOutputFrames",
            "contextualNativeCookingWidgetBlits", "contextualNativeCookingFlameBlits", "contextualNativeCookingArrowBlits",
            "contextualRingAlignedFrames", "contextualRingSectorChanges", "contextualRingRegionBlits", "contextualRingCoverageFrames",
            "contextualProceduralGuardControlCalls", "contextualSameReleaseNativeActions",
            "contextualLiveDestinationFrames", "contextualNativeDestinationCalls", "contextualNativeDestinationSlotDraws",
            "contextualNativeDestinationItemDraws", "contextualNativeFrameCalls", "contextualNativeFrameCoreCalls",
            "contextualNativeFrameTextureBlits", "contextualNativeFrameTitleDraws", "contextualRecipeGateFrames",
            "contextualRecipeGateCardDraws", "contextualRecipeGateStratumFrames",
        )), f"{name}: missing native live-source, textured animation, same-release action or procedural guard positive-control evidence"
        assert feature_probe.get("contextualProceduralShapeViolations")==[] and feature_probe.get("contextualPresentationViolations")==[], \
            f"{name}: procedural wheel fallback or native presentation geometry/codepath failure"
        assert feature_probe.get("contextualNativeSourcePoseViolations")==0 and feature_probe.get("contextualTextContrastViolations")==[], \
            f"{name}: native source geometry or body text contrast is invalid"
        assert feature_probe["contextualLiveSourceFrames"]==feature_probe["contextualNativeSourceCalls"], \
            f"{name}: each actual moved source must dispatch its native Core render once"
        assert feature_probe["contextualLiveDestinationFrames"]==feature_probe["contextualLiveSourceFrames"] \
            and feature_probe["contextualNativeDestinationCalls"]==feature_probe["contextualLiveDestinationFrames"], \
            f"{name}: actual live destination is missing from the presented pair"
        assert feature_probe["contextualNativeFrameCalls"]==2*feature_probe["contextualPresentationFrames"] \
            and feature_probe["contextualNativeFrameCoreCalls"]==feature_probe["contextualNativeFrameCalls"] \
            and feature_probe["contextualNativeFrameTextureBlits"]==9*feature_probe["contextualNativeFrameCalls"] \
            and feature_probe["contextualNativeFrameTitleDraws"]==feature_probe["contextualNativeFrameCalls"], \
            f"{name}: contextual panels are not using the exact ordinary Core nine-slice/title renderer"
        frame_examples=feature_probe.get("contextualNativeFrameExamples",[])
        assert {row.get("kind") for row in frame_examples}=={"WHEEL","MATERIALS"} \
            and all(row.get("title") and row.get("width",0)>0 and row.get("height",0)>0 for row in frame_examples), \
            f"{name}: missing actual dynamic native frame examples"
        assert 0<feature_probe.get("contextualMaximumRingRegionCalls",0)<=576 \
            and 0<feature_probe.get("contextualMaximumRingSectorPixels",0)<=288*288 \
            and feature_probe["contextualRingCoverageFrames"]==feature_probe["contextualRingAlignedFrames"], \
            f"{name}: fixed-UV highlight slice lacks bounded native crop coverage"
        sector=feature_probe.get("contextualLastRingSector",{})
        assert sector.get("sourceWidth")==sector.get("sourceHeight")==288 and math.isfinite(sector.get("angle",float("nan"))) \
            and 0<sector.get("halfAngle",0)<=math.pi and sector.get("recipeCount",0)>0 \
            and abs(sector["halfAngle"]-math.pi*min(10,sector["recipeCount"])/sector["recipeCount"])<1e-6, \
            f"{name}: actual native highlight sector source/angle is invalid"
        regions=sector.get("nativeRegions",[])
        assert 0<len(regions)<=576, f"{name}: actual native region evidence is absent/unbounded"
        coverage=set()
        for region in regions:
            x,y,width,height=(region[key] for key in ("x","y","width","height"))
            assert all(type(value) is int for value in (x,y,width,height)) and x>=0 and y>=0 and width>0 and height>0 \
                and x+width<=288 and y+height<=288, f"{name}: actual highlight region escapes original texture UVs"
            points={(px,py) for py in range(y,y+height) for px in range(x,x+width)}
            assert not coverage.intersection(points), f"{name}: overlapping highlight regions repeat source pixels"
            coverage.update(points)
        for y in range(288):
            for x in range(288):
                difference=math.atan2(x+.5-144,-(y+.5-144))-sector["angle"]
                margin=abs(math.atan2(math.sin(difference),math.cos(difference)))-sector["halfAngle"]
                assert abs(margin)<=1e-12 or ((x,y) in coverage)==(margin<0), \
                    f"{name}: native source coverage does not match the current variable-width selected sector"
        examples=feature_probe.get("contextualVariableRingSectors",[])
        assert len(examples)>=2 and feature_probe.get("contextualMinimumRingHalfAngle",0)>0 \
            and feature_probe.get("contextualMaximumRingHalfAngle",0)>feature_probe["contextualMinimumRingHalfAngle"], \
            f"{name}: variable highlight did not shrink/grow across actual native catalogs"
        assert all(row.get("recipeCount",0)>0 and row.get("visibleCount")==min(10,row["recipeCount"])
            and abs(row.get("halfAngle",0)-math.pi*row["visibleCount"]/row["recipeCount"])<1e-6 for row in examples), \
            f"{name}: highlight examples fail the independent visible-range formula"
        assert feature_probe.get("contextualRecipeNavigationStratumFrames",0)>0 and feature_probe.get("contextualRecipeNavigationDraws",0)>0, \
            f"{name}: native ring/letters did not render above the opening gate"
        assert all(feature_probe.get("contextualBackdropModes",{}).get(mode,0)>0
                   for mode in ("SIDE","CONTAINER_WHEEL","CONTEXTUAL_HELD","CONTEXTUAL_CLOSING")), \
            f"{name}: constant actual world dim was not proven through every wheel transition"
        assert feature_probe.get("contextualMissingGhostFrames",0)>0 \
            and feature_probe.get("contextualMissingGhostNativeItems",0)==feature_probe.get("contextualMissingGhostNativeRedWashes",-1)>0 \
            and feature_probe.get("contextualMissingGhostExamples"), f"{name}: native missing red/translucent ingredient proof absent"
        assert feature_probe.get("contextualRecipeRejectionReads",0)>=3, \
            f"{name}: native full-storage, stale-BEGIN and expired lease inactive recovery was not observed"
        packets=feature_probe.get("contextualHoverPacketEvidence",{})
        assert feature_probe.get("contextualStaleClickPackets")==1 and feature_probe.get("contextualStaleBeginPackets")==1 \
            and feature_probe.get("contextualStalePacketArmsConsumed") is True, f"{name}: selected stale packet fault injection was not observed exactly once"
        assert all(packets.get(key,{}).get("preservedAuthAndItemPayload") is True
                   and packets[key]["sentState"]==((packets[key]["actualFactoryState"]+32760)&32767)
                   and packets[key]["sentState"]!=packets[key]["actualFactoryState"] for key in ("staleClick","staleBegin")), \
            f"{name}: genuinely stale final wire state or unchanged authenticated payload proof absent"
        assert all(packets.get(key,{}).get("untouched") is True for key in ("authenticClick","authenticQuickMove")), \
            f"{name}: first authenticated native click/quickmove packet proof absent"
        stale_return=packets.get("staleClickServerReturn",{})
        assert stale_return.get("previewRetained") is True and stale_return.get("carriedEmpty") is True \
            and stale_return.get("nativeState",-1)>=0 \
            and stale_return.get("nativeState")!=stale_return.get("sentState"), \
            f"{name}: stale click did not return with its native preview preserved and an actual mismatched state"
        lease=packets.get("nativeLease",{})
        assert lease.get("nativeTicksSinceTouch",0)>60 and lease.get("endTick",0)-lease.get("touchedTick",0)==lease.get("nativeTicksSinceTouch"), \
            f"{name}: actual integrated-server lease ticks did not expire before restoration"
        cooking=packets.get("nativeCookingEntry",{})
        assert cooking.get("ironBefore",0)>=8 and cooking.get("ironAfter")==cooking.get("ironBefore") \
            and cooking.get("bedrockBefore",0)>=2 and cooking.get("bedrockAfter")==cooking.get("bedrockBefore") \
            and cooking.get("inputCount")==0 and cooking.get("outputCount")==0 and cooking.get("fuelCount")==5 \
            and cooking.get("ironLocations") and cooking.get("bedrockLocations"), \
            f"{name}: native entry input/output clearing and all-owned quantity/fuel conservation proof absent"
        text_examples=feature_probe.get("contextualReadableTextExamples",[])
        assert text_examples and feature_probe.get("contextualNativeItemCountTextDraws",0)>0 \
            and not any(row["text"].casefold() in ("crafted item","craft requested","item selected","tab selected") for row in text_examples), \
            f"{name}: readable native text examples absent or redundant status captions retained"
        def luminance(color):
            channels=[((color>>shift)&255)/255 for shift in (16,8,0)]
            linear=[value/12.92 if value<=.04045 else ((value+.055)/1.055)**2.4 for value in channels]
            return sum(value*weight for value,weight in zip(linear,(.2126,.7152,.0722)))
        background=luminance(0xFFCEBFA4)
        assert all((row["color"]>>24)==255 and (max(luminance(row["color"]),background)+.05)/(min(luminance(row["color"]),background)+.05)>=4.5
                   for row in text_examples), f"{name}: actual contextual text colors fail contrast against light native chrome"
        decoded=feature_probe.get("contextualDecodedTextures",{})
        expected_ids={f"salts_inventory_controlify:textures/gui/contextual/wheel/{key.lower()}.png" for key in CONTEXTUAL_TEXTURES_26_3}
        assert set(decoded)==expected_ids, f"{name}: actual native decode did not cover all16 required wheel textures"
        for key,(width,height) in CONTEXTUAL_TEXTURES_26_3.items():
            image=decoded[f"salts_inventory_controlify:textures/gui/contextual/wheel/{key.lower()}.png"]
            assert feature_probe.get("contextualTextureBlits",{}).get(key,0)>0 and image.get("width")==width \
                and image.get("height")==height and (key=="BACKDROP" or image.get("visible",0)>0) \
                and image.get("visible",0)+image.get("transparent",0)==width*height \
                and len(image.get("pixelSha256",""))==64, f"{name}: required wheel texture was not actually decoded/blitted: {key}"
        assert all(decoded[f"salts_inventory_controlify:textures/gui/contextual/wheel/{key.lower()}.png"].get("transparent",0)>0
                   for key in ("HOVERED_CARD","HOVERED_CREATIVE_TAB","ACTIVE_CREATIVE_TAB")), \
            f"{name}: hover/active state sprites must be transparent overlays"
        transitions=feature_probe.get("contextualPresentationTransitions",[])
        assert 0<len(transitions)<=64 and any(row.get("held") is True and 0<row.get("progress",0)<1 for row in transitions) \
            and any(row.get("held") is False and 0<row.get("progress",0)<1 for row in transitions), \
            f"{name}: bounded actually drawn opening/reverse frames are absent"
        dimensions=feature_probe.get("contextualNativeSourceDimensions",[])
        assert 0<len(dimensions)<=32 and {"CRAFTING","COOKING","CREATIVE"}.issubset({row.get("kind") for row in dimensions}), \
            f"{name}: bounded actual native source dimensions missing across menus/tabs"
        assert all(abs(row["actualWidth"]-row["frameWidth"])<=.1 and abs(row["actualHeight"]-row["frameHeight"])<=.1
                   and row["nativeWidth"]>0 and row["nativeHeight"]>0 for row in dimensions), \
            f"{name}: moved native source aspect differs from cached frame"
        assert all(feature_probe.get(key, 0) > 0 for key in (
            "compactGuideFrames", "compactGuideEntryDraws", "compactGuideNativeTextDraws", "compactGuideLayoutFrames",
            "inactivePreviewFrames", "inactiveNativeItemDraws", "activeNativePaneDraws", "texturedTuckFrames",
            "texturedTuckIntermediateFrames", "nativeAlphabetFrames", "nativeAlphabetMarkers", "nativeAlphabetTextDraws",
            "nativeAlphabetLabelBoundsFrames", "nativeAlphabetAnchorTicks",
        )), f"{name}: missing actual compact guide, item/texture or alphabet observations"
        assert feature_probe.get("compactGuideViolations") == [] and feature_probe.get("visualFeatureViolations") == [], \
            f"{name}: invalid compact layout, native icon/texture/sound code path"
        assert feature_probe.get("nativeAlphabetLabelOverlapViolations") == [] \
            and feature_probe.get("nativeAlphabetLabelBoundsViolations") == [] \
            and feature_probe.get("nativeAlphabetAnchorTicks") == feature_probe.get("nativeAlphabetMarkers"), \
            f"{name}: actual alphabet labels overlap/escape content or first-recipe anchor ticks changed"
        assert feature_probe.get("nativeAlphabetInsideLabels",0)>0 and feature_probe.get("nativeAlphabetOutsideLabels",0)>0, \
            f"{name}: actual alphabetical labels did not use both annular lanes"
        assert set(feature_probe.get("texturedTuckAtlasFrames", [])) == {0, 1}, f"{name}: both actual tuck atlas frames were not drawn"
        textures = feature_probe.get("decodedTuckTextures", {})
        expected_tuck = "salts_inventory_controlify:textures/gui/workspace/tuck_away.png"
        assert set(textures) == {expected_tuck}, f"{name}: missing actual resource-pack tuck atlas decode"
        texture = textures[expected_tuck]
        assert texture.get("width") == 64 and texture.get("height") == 32 and texture.get("visiblePixels", 0) > 0 \
            and texture.get("transparentPixels", 0) > 0 and len(set(texture.get("decodedFrameSha256", []))) == 2, \
            f"{name}: tuck texture is missing transparent/distinct 32px state frames"
        ui_sound_evidence = workspace_ui_sound_evidence(feature_probe, name)
        assert all(feature_probe.get(key, 0) > 0 for key in (
            "tabFrames", "tooltipCalls", "cursorCalls", "nativeNavigationRequests",
            "nativeWorkspaceGuideFrames", "nativeGameplayGuideFrames", "nativeTuckedTopGuideFrames",
            "nativeTuckedBottomGuideFrames", "nativeTuckedTransitionGuideFrames", "nativeContextualOverlayFrames", "nativeLayoutViewportFrames", "nativeCategoryFrames",
            "nativeCreativeTabFrames", "nativeRecipePreviewFrames", "nativeVanillaBookSpritesLegacyOptOut",
        )), \
            f"{name}: missing actual 26.3 native rendering observations"
        assert feature_probe.get("nativeActiveCreativeTabCount") == 1, \
            f"{name}: creative tab rendering did not retain exactly one native active marker"
        mode_frames = feature_probe.get("nativeWorkspaceGuideModeFrames", {})
        assert all(mode_frames.get(mode, 0) > 0 for mode in ("SIDE", "LT_RADIAL", "RECIPE", "COOKING", "CREATIVE_TABS")), \
            f"{name}: missing actual native guide mode rendering"
        assert feature_probe.get("removedPaneBookDraws", -1) == 0 and feature_probe.get("manualControlTextDraws", -1) == 0 \
            and feature_probe.get("unexpectedRemovedControlDraws") == [], \
            f"{name}: removed pane icon or manual control instructions were drawn"
        assert feature_probe.get("nativeTuckedGuideOverlapViolations") == [], \
            f"{name}: native gameplay guides overlap actual tucked tabs/previews"
        assert feature_probe.get("nativeContextualGuideOverlapViolations") == [], \
            f"{name}: actual recipe/cooking/creative surfaces overlap native bottom guides"
        assert feature_probe.get("nativeVanillaBookSpritesContextualEnabled") == 0 \
            and feature_probe.get("unexpectedVanillaBookSprites") == [], \
            f"{name}: native vanilla recipe-book button rendered while controller contextual presentation was enabled"
        captured_names = {row["name"] for row in records if row["status"] == "CAPTURED"}
        assert FEATURE_CAPTURES_26_3.issubset(captured_names), \
            f"{name}: missing refined-wheel visual evidence: {sorted(FEATURE_CAPTURES_26_3 - captured_names)}"
        sort_requests = feature_probe.get("nativeSortRequests", [])
        assert any(",force=false,sent=true" in request for request in sort_requests) \
            and any(",force=true,sent=true" in request for request in sort_requests), \
            f"{name}: missing actual accepted normal/force Core sort requests"
    if game in ("1.21.11", "26.1.2", "26.2", "26.3"):
        required = {"queued-native-fractional-vertices", "queued-native-ordinary-colors",
                    "queued-native-premultiplied-item-entity-colors", "queued-opacity-scope-restored",
                    "queued-native-opacity-survives-extraction"}
        assert required.issubset(passed), f"{name}: missing actual deferred-opacity proof"
        opacity = report["queuedOpacityProbe"]
        assert all(opacity[key] > 0 for key in ("fractionalColorCalls", "ordinaryColorCalls", "premultipliedColorCalls")), \
            f"{name}: native queued text/item/PIP colors did not fade"
        assert opacity["deferredFractionalColorCalls"] > 0, \
            f"{name}: opacity was not preserved after the extraction scope closed"
        assert 0 < opacity["minimumCapturedOpacity"] <= opacity["maximumCapturedOpacity"] < 1, \
            f"{name}: opacity probe did not observe real fractional queued rendering"
    mods = report["loadedMods"]
    assert mods["minecraft"] == game, f"{name}: wrong Minecraft runtime"
    assert {"salts_inventory_update", "salts_inventory_controlify", "controlify", "yet_another_config_lib_v3"}.issubset(mods), \
        f"{name}: missing actual required runtime mods"
    expected_version = target["controller"]["version"]
    if target.get("native_forge_port"):
        expected_version = expected_version.split("-mc", 1)[0]
    assert mods["controlify"] == expected_version or mods["controlify"].startswith(expected_version + "+"), \
        f"{name}: wrong controller runtime version {mods['controlify']}"
    assert mods["yet_another_config_lib_v3"] == target["yacl"]["version"], f"{name}: wrong YACL runtime"
    runtime_dependencies = target.get("runtime_dependencies", [])
    for dependency in runtime_dependencies:
        assert mods.get(dependency["mod_id"]) == dependency["version"], \
            f"{name}: missing original required runtime dependency {dependency['mod_id']}"
    for field in ("coreCodeSource", "companionCodeSource", "controlifyCodeSource"):
        source = report.get(field, "")
        assert ".jar" in source.lower() and "/classes/" not in source.replace("\\", "/").lower(), \
            f"{name}: {field} did not load from a production JAR: {source}"
    if run_profile == "auto":
        run_profile = "packaged-controlify-" + name if report_suffix else "packaged-controlify"
    assert "/" not in run_profile and "\\" not in run_profile and run_profile.startswith("packaged-controlify"), \
        f"{name}: invalid disposable runtime profile"
    mods_folder = ROOT / f"controlify/versions/{game}/{loader}/build/runtime-smoke/{run_profile}/mods"
    staged = list(mods_folder.glob("*.jar"))
    assert staged, f"{name}: missing staged production mods"
    optional_entries = []
    if with_optional_mods:
        optional = json.loads((results_root / name / 'optional-mods.json').read_text(encoding='utf-8-sig'))
        pins = json.loads((ROOT / 'controlify/functional-tests/mod-fixtures-26.3.json').read_text(encoding='utf8'))
        assert optional['minecraft'] == game and optional['loader'] == loader
        optional_entries = pins['profiles'][loader + '-' + optional['browser']]
        assert {(entry['version_id'],entry['file']['filename']) for entry in optional['mods']} == \
            {(entry['version_id'],entry['file']['filename']) for entry in optional_entries}, f'{name}: optional fixture pins differ'
        for entry in optional_entries:
            path = mods_folder / entry['file']['filename']
            assert path.is_file() and digest(path,'sha512') == entry['file']['hashes']['sha512'], f'{name}: modified upstream fixture {path}'
            with ZipFile(path) as jar:
                if 'fabric.mod.json' in jar.namelist() and loader == 'fabric':
                    ids = [json.loads(jar.read('fabric.mod.json'))['id']]
                elif 'META-INF/neoforge.mods.toml' in jar.namelist():
                    ids = [row['modId'] for row in tomllib.loads(jar.read('META-INF/neoforge.mods.toml').decode())['mods']]
                else: ids = []
            assert all(mod_id in mods for mod_id in ids), f'{name}: staged optional mod did not load: {ids}'
    assert len(staged) == 4 + len(runtime_dependencies) + (loader != "fabric") + with_extension + len(optional_entries), \
        f"{name}: unexpected staged mod outside the exact production/upstream/QA inventory"
    if loader != "fabric":
        qa_name = f"controller-qa-{game}-{loader}.jar"
        qa_release = ROOT / f"controlify/versions/{game}/{loader}/build/controlify-qa-artifact/{qa_name}"
        assert (report_suffix and (mods_folder / qa_name).is_file()) or qa_release.is_file() and digest(qa_release) == digest(mods_folder / qa_name), \
            f"{name}: staged QA fixture differs from the compiled opt-in artifact"
    staging_path = results_root / name / "staged-artifacts.json"
    staging = json.loads(staging_path.read_text(encoding="utf-8-sig"))
    assert staging["minecraft"] == game and staging["loader"] == loader, f"{name}: wrong launcher inventory"
    inventory = staging["artifacts"]
    assert len(inventory) == len({item["file"] for item in inventory}) == len(staged), \
        f"{name}: launcher inventory does not describe the exact staged JARs"
    assert {item["file"] for item in inventory} == {path.name for path in staged}, \
        f"{name}: stale or unrecorded staged artifact"
    for item in inventory:
        path = mods_folder / item["file"]
        assert path.stat().st_size == item["bytes"] and digest(path) == item["sha256"], \
            f"{name}: staged artifact differs from launch evidence: {item['file']}"
    allowed_outputs = {(ROOT / f"controlify/versions/{game}/{loader}/build/{folder}/controlifyQa").resolve()
                       for folder in ("classes/java", "resources")} if loader == "fabric" else set()
    assert all(Path(path).resolve() in allowed_outputs for path in staging["projectClasspath"]), \
        f"{name}: runtime loaded implementation directories instead of production JARs"
    assert report_path.stat().st_mtime >= staging_path.stat().st_mtime, \
        f"{name}: gameplay report predates the recorded launcher inventory"
    originals, lineage = forge_lineage(results_root / name, mods_folder, report_path, with_extension) if loader == "forge" else ({}, None)
    hashes = {digest(path, "sha512"): path for path in (list(originals.values()) if lineage else staged)}
    for role in ("controller", "yacl"):
        assert target[role]["sha512"] in hashes, f"{name}: original pinned {role} JAR is absent or altered"
    for dependency in runtime_dependencies:
        runtime = mods_folder / dependency["filename"]
        assert runtime.is_file() and digest(runtime, "sha512") == dependency["sha512"], \
            f"{name}: required runtime shim must be staged unchanged from the original pin"
        assert runtime.name not in originals, f"{name}: version-neutral runtime shim must not be remapped"
    matching = {}
    artifact_lineage = {}
    version = mods["salts_inventory_update"]
    modules = [("core", "salts_inventory_update"), ("controlify", "salts_inventory_controlify")]
    if with_extension:
        assert mods.get('salts_inventory_compat'), f'{name}: Extended jar did not load'
        modules.append(('compatibility','salts_inventory_compat'))
    for module, prefix in modules:
        filename = f"{prefix}-{game}-{LOADER_NAMES[loader]}-{mods[prefix]}.jar"
        release, runtime = artifacts / module / filename, mods_folder / filename
        assert release.is_file() and runtime.is_file(), f"{name}: missing final {module} production JAR"
        preserved = originals.get(filename, runtime)
        artifact_lineage[module] = artifact_match(module,release,preserved,allow_ui_text_correction and module=='controlify')
        assert report_path.stat().st_mtime >= runtime.stat().st_mtime, f"{name}: report predates its staged {module}"
        if game == "26.3" and module == "controlify":
            with ZipFile(release) as jar:
                workspace_ui_sound_resources(jar, name)
                assert all("com/salts_inventory_update/controlify/" + cls + ".class" in jar.namelist()
                    for cls in ("CreativeSearchBinding", "CreativeSearchTarget", "WorkspaceSearchKeyboard")), \
                    f"{name}: missing companion-owned native creative search implementation"
                assert not any("FeatureHostMouseIsolationMixin" in entry for entry in jar.namelist()), \
                    f"{name}: QA-only SDL motion isolation leaked into the production companion"
                assert not any("/RecipeBookHintRenderer" in entry for entry in jar.namelist()), \
                    f"{name}: removed pane recipe-book renderer remains in final production JAR"
                provider = "com.salts_inventory_update.controlify.WorkspaceControlifyEntrypoint"
                service = "META-INF/services/dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint"
                assert service in jar.namelist() and provider.replace(".", "/") + ".class" in jar.namelist(), \
                    f"{name}: native Controlify startup provider or registration is absent from final JAR"
                providers = {line.split("#", 1)[0].strip() for line in jar.read(service).decode("utf-8-sig").splitlines()}
                assert provider in providers, f"{name}: native Controlify pre-init guide registration is missing"
                tuck_png = jar.read("assets/salts_inventory_controlify/textures/gui/workspace/tuck_away.png")
                assert tuck_png.startswith(b"\x89PNG\r\n\x1a\n") and struct.unpack(">II",tuck_png[16:24]) == (64,32), \
                    f"{name}: final companion is missing its editable two-frame tuck PNG"
                for element,dimensions in CONTEXTUAL_TEXTURES_26_3.items():
                    png=jar.read(f"assets/salts_inventory_controlify/textures/gui/contextual/wheel/{element.lower()}.png")
                    assert png.startswith(b"\x89PNG\r\n\x1a\n") and struct.unpack(">II",png[16:24])==dimensions, \
                        f"{name}: final required contextual PNG missing/wrong size: {element}"
                assert not any(f"assets/salts_inventory_controlify/textures/gui/contextual/wheel/{retired}.png" in jar.namelist()
                    for retired in ("panel","wheel_panel","arc_boundary","outer_marker")), \
                    f"{name}: retired panel/boundary textures remain in the final26.3 companion"
        if module in ('controlify', 'compatibility'):
            companion_versions(release, loader, version, mods[prefix], prefix)
        matching[module] = digest(release)
    expected_sources = {
        "coreCodeSource": mods_folder / f"salts_inventory_update-{game}-{LOADER_NAMES[loader]}-{version}.jar",
        "companionCodeSource": mods_folder / f"salts_inventory_controlify-{game}-{LOADER_NAMES[loader]}-{mods['salts_inventory_controlify']}.jar",
        "controlifyCodeSource": mods_folder / hashes[target["controller"]["sha512"]].name,
    }
    if with_extension and "extensionCodeSource" in report:
        expected_sources["extensionCodeSource"] = mods_folder / f"salts_inventory_compat-{game}-{LOADER_NAMES[loader]}-{mods['salts_inventory_compat']}.jar"
    for field, expected in expected_sources.items():
        assert code_source_path(report[field]) == expected.resolve(), \
            f"{name}: {field} did not load its actual preserved profile JAR"
    captures = [row for row in records if row["status"] == "CAPTURED"]
    for row in captures:
        image = results_root / name / "screenshots" / (row["name"] + ".png")
        assert image.is_file() and image.read_bytes().startswith(b"\x89PNG\r\n\x1a\n"), \
            f"{name}: missing actual screenshot {row['name']}"
    assert captures, f"{name}: no visual evidence captured"
    summary = {
        "minecraft": game, "loader": loader, "failures": 0,
        "records": len(records), "passes": sum(row["status"] == "PASS" for row in records),
        "screenshots": len(captures), "elapsed_ms": report["elapsedMs"],
        "artifact_sha256": matching, "artifact_lineage":artifact_lineage, "report": str(report_path.relative_to(ROOT)),
        "host_sdl_motion_isolation": input_isolation,
        "validated_scope": "main suite before independent API setup" if completed_main_only else "all recorded scenarios",
        "report_failures": report.get('failures'),
        "requires_successful_final_artifact_api_retry": completed_main_only,
    }
    if ui_sound_evidence is not None:
        summary["workspace_ui_sounds"] = ui_sound_evidence
    if lineage:
        summary["runtime_remapping"] = {"operation": lineage["operation"], "mappings_sha256": lineage["mappings_sha256"],
                                       "mapped_artifact_sha256": {Path(row["mapped"]).name: row["mapped_sha256"]
                                                                  for row in lineage["artifacts"]}}
    if runtime_dependencies:
        summary["runtime_dependencies"] = [{"mod_id": dependency["mod_id"], "version": dependency["version"],
                                             "filename": dependency["filename"], "sha512": dependency["sha512"]}
                                            for dependency in runtime_dependencies]
    print(f"Packaged runtime verified: {name}, {summary['passes']} PASS records, {len(captures)} screenshots, final artifact lineage checked")
    return summary


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--targets", type=Path, default=ROOT / "controlify/targets.json")
    parser.add_argument("--results-root", type=Path, default=ROOT / "build/controlify-qa")
    parser.add_argument("--artifacts", type=Path, default=ROOT / "dist/controlify-test")
    parser.add_argument("--run-profile", default="packaged-controlify")
    parser.add_argument("--report-suffix", default="", help="Suffix of an independently retained full report, e.g. -extended-jei")
    parser.add_argument("--with-extension", action="store_true")
    parser.add_argument("--with-optional-mods", action="store_true")
    parser.add_argument("--feature-parity", action="store_true", help="Require the new cross-version feature pass, including native legacy recipe/keyboard scenarios")
    parser.add_argument("--target", action="append", default=[], help="MC:loader for a focused run; omit to require all12")
    parser.add_argument("--summary", type=Path, help="Write a machine-readable validated evidence inventory")
    args = parser.parse_args()
    targets = json.loads(args.targets.read_text(encoding="utf-8-sig"))["targets"]
    expected = {(game, loader) for game in ("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2", "26.3")
                for loader in ("fabric", "forge" if game == "1.20.1" else "neoforge")}
    keys = [(target["minecraft"], target["loader"]) for target in targets]
    assert len(keys) == len(set(keys)) == 12 and set(keys) == expected, "Runtime inventory must describe the complete12-target matrix"
    selected = {tuple(name.split(":")) for name in args.target} if args.target else expected
    assert selected and selected.issubset(expected), f"Unsupported runtime targets: {selected - expected}"
    assert not any(character in args.report_suffix for character in '/\\'), 'Report suffix cannot contain a path'
    reports = [validate(target, args.results_root.resolve(), args.artifacts.resolve(), args.run_profile,
                        args.report_suffix,args.with_extension,args.with_optional_mods,args.feature_parity)
               for target in targets if (target["minecraft"], target["loader"]) in selected]
    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        args.summary.write_text(json.dumps({"schema_version": 1, "targets": reports}, indent=2) + "\n", encoding="utf-8")
    print(f"Packaged gameplay inventory verified: {len(reports)} targets")
