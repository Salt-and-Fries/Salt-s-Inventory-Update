# Gear-window visual follow-up: Minecraft 1.20.1

Compatibility **0.1.3**, using the existing Core **0.1.6** public API. This follow-up changes the six Fabric gear-workstation windows from the third Project Everything batch. It supersedes their visual pages in the earlier eight-integration report.

The subsequent [pixel-alignment PDF](../output/pdf/siu-gear-window-alignment-1.20.1.pdf) supersedes this report's tab positions and grindstone top edge. Tabs now start at the window's left edge without the extra vertical pixel; both grindstone views retain their top outline row. Its 27 passing focused checks, visual review and final artifact hashes are recorded in [alignment validation](../output/project-everything-gear-alignment/validation.json), with the build and packaging evidence under `build/project-everything-gear-alignment/`. The six layout tests, both packaging tasks and packaged API-boundary check passed. Gameplay coverage remains limited to these six windows on Fabric 1.20.1 in the mapped development fixture.

- **Unlink Gear:** retains the full grindstone artwork, including the left support outside the real slot cells. Its crop now begins at the same native position as Repair & Disenchant.
- **Upgrade Gear:** retains the full native armor-stand preview, with space for both arms, head, feet and equipped armor. The original container's right bevel is excluded.
- **All three workstation tab pairs:** the selected tab's original LibZ connector is rendered over the frame's top bevel using `renderOverlay`. The selected tab joins the window; inactive tabs remain behind its border. Native icon artwork, tab selection and authenticated session transitions are preserved.

The inactive left tab retains its native lower four rows behind the frame, filling the notch without changing the window's rounded corner. Focused evidence for that refinement is under `output/project-everything-tab-overlap/` and `build/project-everything-tab-overlap/`.

The production changes are in Compatibility's `NativeBatchLayouts` and `TabbedGearTemplate`. Core and Controlify production code are unchanged. Both 1.20.1 Compatibility JARs are rebuilt because the layout source is shared; these gear mods are available on the Fabric target.

## Focused verification

The Fabric remap and Forge reobfuscation tasks passed. All **six** `NativeBatchLayoutsTest` tests passed, including regression coverage for artwork outside slots, the full preview region and exclusion of the old container bevel. Build log: `build/project-everything-gear-visual-fixes/build-02.log`.

The final default-metadata packaging pass also passed (`production-packaging.log` in that directory), as did the packaged API-boundary check. Both production JARs were refreshed in `dist/compatibility/`, along with `SHA256SUMS.txt`; their hashes are in `production-artifacts.json` and the runtime validation record. The Fabric gameplay fixture used Loader 0.19.5. Entry-by-entry comparison confirms its class and non-metadata resource bytes match the final Fabric distribution; only loader-declaration/build-provenance metadata differs. The oldest declared Loader version was not separately tested.

GPT-6.1 Terra high performed the affected-window gameplay checks and screenshot review. All 27 focused menu/layout/bounds/transition checks passed. The final visual review confirmed both grindstone supports, complete empty/equipped armor-stand silhouettes, no old container bevel beside the preview, and no horizontal seam through either selected-tab position in any of the three pairs. See the [changed-windows PDF](../output/pdf/siu-gear-window-visual-fixes-1.20.1.pdf) and [runtime validation record](../output/project-everything-gear-visual-fixes/validation.json) for captured states, artifact hashes and coverage limits. Only the six changed windows, including the equipped preview state, belong in this report.

These are focused visual, tab-routing and bounds checks in a mapped development runtime, not a complete functional suite or dedicated-server certification. The earlier [integration validation](next-eight-1.20.1-validation.md) records the separate native recipe/item-operation checks.
