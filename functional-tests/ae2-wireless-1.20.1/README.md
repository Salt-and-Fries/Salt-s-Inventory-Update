# Focused AE2 Wireless Terminal checks

These scripts drive the excluded production UI observer in a disposable
Minecraft 1.20.1 world. They are not included in release JARs and are not a
complete functional or gameplay suite. The network setup clears fixture
inventory, places blocks and grants charged terminals; use a disposable audit
profile, not an existing player world.

The observer needs the AE2 native-screen widget/slot metadata supplied by
`tools/prepare-production-fabric-audit.py` or the equivalent Forge production
observer. The recorded runs also inspect native desktop geometry and the
wireless repository. Publisher JARs are unchanged.

After preparing a profile with the subject mod, required dependencies, Core
and Compatibility, pass its observer output directory explicitly:

```text
python functional-tests/ae2-wireless-1.20.1/setup-network.py fabric <observer-output>
python functional-tests/ae2-wireless-1.20.1/run-focused.py fabric <observer-output>
```

Use `forge` for the other loader. `check-provider.py` isolates the provider
transfer regression using a valid fixture pattern; `final-views.py` captures
all five interfaces, including the native top-right return tabs. It also
captures the newer Forge release's native wireless settings page.

The broad focused driver crafts an ordinary recipe, checks native Trash and
filter navigation, encodes a real pattern, installs it in a connected provider,
then withdraws and reinserts it through Pattern Access. The final regression
specifically confirms that client-side provider slots use native provider-ID
actions rather than ordinary menu-slot clicks.
