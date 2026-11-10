# Focused Universal Graves checks

`probe.py` drives the existing disposable production Fabric observation bridge.
Its `act`, `slot`, `inventory_slot`, `list_open`, `server` and `check` helpers
exercise ordinary client input; `command` creates test-world fixtures. Adjust
`RUN` to a freshly prepared, ready Universal Graves runtime before reuse.
Do not point it at a personal world.

The recorded run uses `integrated-graves02` and the remapped Compatibility JAR
from `build03.log`. `native-graves01` supplies the no-SIU reference. Exact
subject and runtime inputs are in the output manifest and staged-artifact
records.

The focused scenario was run incrementally:

1. Create a survival death grave with 7 diamonds and 16 gold. `/graves` opens
   the native list; clicking its entry opens the explicit storage window.
2. Check the read-only view, Back, `/graves modify`, ordinary withdrawal,
   insertion rejection, Back while carrying loot and separate inventory
   transfer. Back returns carried items to inventory. Hold a regular compass
   and click Info; check that it becomes exactly one native grave compass.
   Quick Pickup should recover the gold and restore the now-empty native list.
3. Create a 41-stack death grave using 36 distinctly named stone stacks,
   four armor pieces and one offhand torch. Direct interaction opens page one;
   Next exposes five more stacks and Previous restores the first 36. Withdraw
   boots from page two, store them in inventory and check the changing roles.
4. Unlock before dragging. Check Remove Protection's two-click confirmation,
   GUI scales 2/3, and Quick Pickup with one occupied inventory cell. Sum real
   grave loot and player inventory, excluding footer display icons: the
   initial 671 individual items must all be conserved.
5. Close/reopen, open the modifying list remotely, Fetch once and verify the
   original grave remains, then Fetch again and verify relocation. Reopen the
   same grave and verify its native Info location and retained stack. Confirm
   Break with two clicks. Test an unrelated vanilla chest.
6. Create a fresh small grave for presentation. Open player inventory beside
   it, capture the full game screen and check that right-clicking Info cannot
   pick up the display icon.

The raw ledger retains three mistaken assertions and their corrected checks.
It is not a complete functional or gameplay suite. In particular, dedicated
multiplayer, another owner's permissions, payment and teleport configurations,
alternate gestures and resource-pack variants were not exercised.

The observation bridge's generated `results.json` is a generic screenshot
runner marker. The manifest and per-check evidence, rather than that marker,
describe the actual integration and functional-check scope.
