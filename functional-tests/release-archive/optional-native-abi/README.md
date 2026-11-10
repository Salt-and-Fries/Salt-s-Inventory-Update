# Optional native ABI guard probe

This is an isolated JVM unit fixture for the actual `NativeDesktopPatchPlugin`
class inside each of the six current shipping Compatibility jars. It does not
start Minecraft or Gradle. It grants no menu, fallback rendering, startup,
other-mod initialization, multiplayer or arbitrary upstream-patch acceptance.

From the repository root, after an ordinary build has produced the native
compiler/classpath manifest and all current distribution jars:

```powershell
python functional-tests/release-archive/optional-native-abi/run.py --run unique-run-id
```

`--java-root` explicitly selects another installed JDK able to load the shipping
Java 25 classes. The default is the build manifest's 26.3 compiler path. Every
run records the selected executable hashes, dependency classpath, original and
staged source, compiled test classes, actual shipping plugin class hashes,
commands, output and current candidate identity. IDs cannot be reused.

The Mixin environment and bytecode provider are synthetic seams. Before the
original `onLoad` call, the fixture deliberately seeds an active flag and a
stale previous-hook entry. For both client and server sides it supplies five
negative prerequisites: an absent AE2 marker via ClassNotFoundException or
IOException, an unavailable native contract via either exception, and empty
original members that violate the packaged ABI. These are unit preconditions;
no actual upstream jar, game outcome, menu state or original assertion changes.

Each process verifies that it loaded the plugin from the exact public jar,
that the original implementation leaves hooks inactive and rejects the stale
and native targets, and that mismatch paths emit the original clear warning.
Absent optional AE2 is a quiet early return. The SLF4J test provider captures
calls rather than suppressing them. Retained cache entries are reported;
inactive hook selection is the acceptance condition.

The first 60-scenario run is preserved in
`output/release-readiness/optional-native-abi-probe-01/manifest.json`. Positive
native ABI loading and actual UI behavior belong to their separately recorded
Minecraft profiles. Replaying this harness does not add distinct UI coverage.
