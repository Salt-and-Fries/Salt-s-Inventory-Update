# Salt's Inventory Update — Core 0.1.6

- Fixes darkened native windows and dark rectangles around transparent tabs on Minecraft 1.21.1.
- Compatible conventional mod containers can open automatically as desktop windows, retaining their native artwork, animations, controls and real inventory slots.
- Automatic windows now use an SIU title-and-controls bar above the original mod artwork and a blank bar below it, preserving native side borders and colors. Manually designed and API windows retain their existing appearance.
- Added public native-window templates for adapting selected parts of an existing interface. Integrations can move or hide sections, add their own controls, keep attached panels outside the frame, and choose standard slot backgrounds.
- Added compact native-window sizing and scrollbars when content cannot fit on screen.
- Added an in-game native-interface inspector and reloadable local layout profiles through `/saltsinventory nativeui`.
- Native windows share desktop inventory interactions, tooltips, recipe-book handling and source lifetime checks.
- Fixes native windows losing a picked-up item when the original menu broadcasts during the click, and preserves Forge 1.20.1 extended-menu opening data.
- Native recipe books fit beside their desktop window and exterior controls, with reliable single-click toggling after reopening.
- Keeps extended-menu opening data and custom native-menu synchronization working with newer Fabric releases.
- Retains the separate native background pass used by newer Minecraft screens, including native slot artwork when Compatibility is absent.
- Saved ordinary container sizes now belong to their menu type, preventing unrelated containers at the same location from inheriting incorrect dimensions.
- These additions are available across all supported Minecraft versions and loaders. Existing API window definitions retain their priority.

Core works independently. Use matching Minecraft and loader builds when installing the optional Compatibility 0.1.3 or Controlify 0.1.1 companions. Fabric requires Fabric API.

Use these same notes for every Core 0.1.6 artifact.
