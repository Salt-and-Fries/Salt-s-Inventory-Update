# Create native-fidelity repairs - Minecraft 1.20.1

Implemented the 19 confirmed findings from the original-versus-SIU visual review.
The native UI is the baseline, including its intentional output overlap and
Category Editor field ending. No public API or Core implementation was changed
by this repair. Fabric and Forge share the changed Compatibility sources.

The compositions now keep whole native working panels, action bands, bevels,
field caps, material joins and control order. Schematicannon settings remain
inline with the process. The Blueprint arrow, requester tray/hook, stock board,
and nested editor rails are complete. Creative ammo uses the original charcoal
and purple skin. Native controller key art replaces the shortened added labels.
Original-scale companion artwork remains alongside the window; narrow GUI
viewports retain it in the scrollable canvas. The category preview uses the
native item renderer to avoid copying the parent modal shade.

Validation: 88 latest focused interaction assertions, 25 captured-state checks,
and two JVM geometry tests passed. Geometry checks include complete cells and
inverse input routing plus visible native controls from 23 reference states.
Affected runtime checks cover storage, policies, text/save, inline settings,
schematic processing, categories, stock requests, schedule suggestions, fuzes
and actual GUI scale 4 at 1280x960. This was not the complete functional/gameplay
suite. Fresh runtime and screenshot evidence is Fabric only; both loader builds
passed. Earlier exploratory QA results remain in the assertion history.

The 29-page comparison PDF includes 25 native/updated states and a repair record
for every review ID: `output/pdf/siu-create-native-fidelity-fixed-comparisons.pdf`.
Full images, original-release hashes, focused assertions, pixel checks and JARs
are under `output/create-native-fidelity-fixes/`. Eleven selected static artwork
comparisons match the original screenshots exactly; this does not claim whole
window pixel identity or exhaustive dynamic-content coverage.
