# Shared texture sources

## Change

`artwork/shared-resources/` is the editable production texture root:

- `core/`: 43 PNGs, including the existing shared creative paging sheet and 42 sheets formerly duplicated across six Minecraft versions.
- `controlify/`: 18 PNGs and the existing editable PDN source, formerly housed under the 26.3 version tree.
- `compatibility/`: reserved for extension-owned textures. Current integration artwork comes from the installed upstream mods; their native version-specific textures are not copied or replaced.

Every Core, Compatibility and Controlify loader module declares its own shared directory as a main resource source. Gradle therefore tracks it for resource processing, JAR inputs and source archives. Runtime resource paths are unchanged. Editor files and the empty-folder marker are excluded. Controlify's optional artwork exporters and its parity checker now use the shared directory; its sounds and language files keep their existing source.

The normal root and individual module checks run `verifySharedTextures`, which rejects foreign Core/Controlify texture namespaces and renewed per-version copies.

## Focused verification

- All **36** `processResources` targets passed across the six Minecraft versions, both loaders and three mod JAR families.
- `python tools/verify-shared-textures.py --resources --jars` passed: all **61** shared PNGs match processed resources and existing release JAR texture entries byte for byte, with no cross-mod texture leakage.
- Compared the migration against a pre-move snapshot: every original texture/editor file is byte-identical, and all 36 distribution JAR texture maps are unchanged. The move removes 210 redundant Core PNG copies.
- An unused temporary PNG copied into the shared Core folder propagated through `:26.3:fabric:processResources`. Removing that source and rerunning the task removed the generated output too. Both checks passed and the probe was cleaned up.
- `verifySharedTextures` passed through Gradle. Representative 26.3 Fabric Core and Controlify source JARs rebuilt and contain exactly their respective 43/18 shared PNGs, with identical bytes and no PDN files.
- Controlify's read-only artwork check passed for its 16 required decoded PNG textures, preserving manual palettes, alpha and backups. Its existing feature/asset parity audit passed for all twelve companion targets against `dist/`.
- The existing source-parity audit passed after being updated to check the central control atlas rather than six version copies.

Evidence is stored under ignored `build/shared-textures/`: `before.json`, `process-resources.log`, `incremental.json`, `incremental-add.log`, `incremental-remove.log`, `source-archives.log`, `source-archive-verification.json`, `source-parity.log` and `controlify-parity.json`.

## Coverage limits

This is a resource-location/build-input change. No gameplay or complete functional suite was run. The full root build and all 36 production JAR rebuilds were not repeated; all resource targets and two source archives were exercised, and current production texture contents were compared with the new masters. Existing `dist/` JARs were not rewritten because no texture contents changed. Editing a shared PNG requires rebuilding and replacing the affected installed/uploaded JARs.
