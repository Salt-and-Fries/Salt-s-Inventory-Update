# Chipped 1.20.1 focused runtime checks

These checks use pinned Chipped 3.0.7 with SIU Core 0.1.6 and Compatibility 0.1.3. They are not a full gameplay suite. Keep the original native-only capture as a design reference, never as SIU evidence. Record staged SHA-256 hashes and Forge remapping lineage.

- Open all seven source blocks and verify the expected registered Chipped definition, title, bounds and safe layout, then capture each empty window.
- Exercise the source picker with an eligible inventory stack and duplicates; verify no item/cursor movement. Check popup dismissal, bounds and keyboard focus; ordinary inventory pickup remains ordinary.
- Verify all four preview modes, including a tall or connected block; check their visible silhouettes and controls.
- Select a variant and exercise search, no matches, wheel/scrollbar and every part of the final visible row. Check selection and preview change through actual UI input.
- Craft once and Shift-Craft All through the actual visible control, verifying server inventory counts, unaffected nonmatching stacks and other containers, cursor and native post-craft selection reset.
- Replace, move or consume a selected source stack before crafting and verify the stale selection is invalidated rather than creating items. Exercise duplicate matching stack selection and hotbar mapping.
- Open two different workbenches simultaneously; independently change their selections and remove or leave range of one source, including each MAIN/SIDE half and reopen. Verify the unaffected session persists.
- Repeat seven opens and a real picker/variant/craft operation on Forge, including evidence of original-to-named QA remap lineage.
