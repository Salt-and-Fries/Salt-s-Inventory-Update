# Focused Prominent forge checks

Targets the unmodified embedded Prominent Base 4.1.5 on Minecraft 1.20.1
Fabric in the production intermediary namespace. This is an excluded fixture;
its observer, original parent data-only resources and material mods are not
included in the compatibility release jar.

Prepare with `prepare.py` using the original audited
`build/remaining-ui-audit/fixtures/pr-base-forge-materials.json`, Core 0.1.6,
the current Compatibility 0.1.3 jar, `--side integrated` and a fresh `--run`.
Use the generated `launch.json` without changing native subject jars.

Once `ready.json` exists, run `check.py <run-directory> visuals`, `upgrade`,
then `armor`, sequentially. They exercise only these two screens. The armor
setup waits for any real running upgrade timer; it does not cancel or modify
that timer. Operator commands seed disposable progression, items and currency.
Button actions, window controls, selection, wheel navigation and dragging use
ordinary screen input. The pointer command also updates the simulated mouse
position before rendering, because upstream's upgrade button checks its
render-time hover flag before accepting a click.

The observer reads actual server components and inventory contents. A successful
armor test checks all five consumed ingredients, the exact 150-Cinders debit
and the real `prominent:armor/paladin_helmet` delivered after the original
animation. The upgrade test checks material consumption, the native timer and
closure of only its owning detached session. Live scale changes are included
because upstream computes its absolute armor button positions only in build().

`publish.py <run-directory>` requires all 25 focused assertions to pass and the
current release jar to match the staged, tested hash. It copies screenshots,
scope notes, build and original-release lineage alongside that exact jar.

No complete gameplay suite runs here. Other recipes/releases, resource packs,
controllers, full multiblock construction and dedicated-server multiplayer are
outside this focused check. The Cinder/Serkonid trader is deferred.
