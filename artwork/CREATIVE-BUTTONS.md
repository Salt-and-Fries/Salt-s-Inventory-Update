# Creative inventory paging buttons

Edit `shared-resources/assets/salts_inventory_update/textures/gui/creative_buttons.png`.
This single source is packaged for every Minecraft version and loader. Rebuild after editing;
do not edit generated copies in `build` directories.

Keep the atlas at **256 × 256 pixels**. Each button occupies **11 × 12 pixels**;
coordinates below are measured from the top-left, starting at zero.

| State | Previous (x, y) | Next (x, y) |
| --- | --- | --- |
| Normal | 0, 0 | 11, 0 |
| Hovered | 22, 0 | 33, 0 |
| Disabled/background | 0, 12 | 11, 12 |

Only these six cells are sampled. The rest of the atlas is unused. The disabled
cells are also drawn beneath the active buttons, matching the existing renderer.

The starter is an unmodified copy of Fabric API's creative paging atlas from
fabric-item-group-api-v1 4.0.14+1802ada577, distributed under Apache-2.0.
Its license is included in `shared-resources/LICENSE-creative-buttons-fabric`.
It is loaded under Salt's own namespace; Fabric is not required.
