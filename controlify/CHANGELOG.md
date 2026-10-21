# Controlify companion changelog

## 0.1.1

Standalone notes: [Controlify 0.1.1](../changelogs/changelog-controlify-0.1.1.md).

- Rebuilds all artifacts against Core API 6 without removed window-feature calls. The focused window-cleanup validation supersedes the metadata-only comparison below.

- Requires Core 0.1.5 across all twelve Minecraft/loader targets, fixing the exact-version conflict on Minecraft 1.20.1 and keeping all companion releases aligned with the current Core patch.
- Uses Core's alternative-workspace onboarding behavior so the regular-window tutorial does not automatically interrupt first-world entry. Manual help remains available, including without an active controller.
- Works alongside the optional Compatibility 0.1.2 extension for the matching Minecraft version and loader.
- Retains the existing controller workspace, crafting and recipe wheels, native keyboard, inventory interactions and selected status-effects tray design.
- The preceding Core 0.1.4.2 release set passed 42 packaged gameplay profiles across all twelve targets, including fresh-world onboarding, focused interaction/rendering checks and four native JEI/REI integration profiles. That earlier API5 build only changed release labels and dependency metadata; it is historical evidence. The current API6 rebuild and focused runtime checks are documented in the [window cleanup validation](../functional-tests/WINDOW-CLEANUP-VALIDATION.md). See the [prior release validation report](docs/release-0.1.1-validation.md) for its executed coverage and limits.

## 0.1.0

- Initial independently versioned controller companion for all twelve Minecraft/loader targets.
- Provides the controller workspace, native input and keyboard adapters, crafting and recipe wheels, controller preferences, sounds and status-effects tray through Core's public API.
