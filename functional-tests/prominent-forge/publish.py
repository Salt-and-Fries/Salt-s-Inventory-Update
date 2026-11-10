"""Package the exact tested compatibility jar and focused in-game evidence."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[2]
SHA=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("run",type=Path)
    args=parser.parse_args();run=args.run
    results=[]
    for phase,count in [("upgrade",6),("armor",14),("visuals",5)]:
        rows=json.loads((run/"focused-evidence"/phase/"results.json").read_text())
        assert len(rows)==count and all(r["passed"] for r in rows),(phase,rows)
        results.extend(dict(phase=phase,**r) for r in rows)
    jar=ROOT/"compatibility/versions/1.20.1/fabric/build/libs/salts_inventory_compat-1.20.1-Fabric-0.1.3.jar"
    staged=json.loads((run/"staged-artifacts.json").read_text())
    matches=[a for a in staged["artifacts"] if a.get("filename")==jar.name]
    assert len(matches)==1 and matches[0]["sha256"]==SHA(jar),matches
    with zipfile.ZipFile(jar) as z:
        assert not any("siu_reference" in n or "NativeUiReference" in n for n in z.namelist())
    core=ROOT/"versions/1.20.1/fabric/build/libs/salts_inventory_update-1.20.1-Fabric-0.1.6.jar"
    assert SHA(core)=="4822c251556e35b9f7eea5103f83791e2f58f6aa154701b247fed051c1e22673"
    out=ROOT/"output/prominent-forge-implemented";out.mkdir(exist_ok=True)
    shutil.copy2(jar,out/jar.name)
    for name in ["forge-upgrade-with-inventory","forge-armor-with-inventory",
                 "armor-selection-final-gui2","armor-selection-final-gui3",
                 "forge-upgrade-final-gui2","forge-upgrade-final-gui3"]:
        shutil.copy2(run/"screenshots"/(name+".png"),out/(name+".png"))
    for source,name in [(run/"staged-artifacts.json","staged-artifacts.json"),
                        (ROOT/"build/prominent-forge-windows/build06.log","build.log"),
                        (ROOT/"build/remaining-ui-audit/original-resources/data-lineage.json","original-parent-data-lineage.json"),
                        (ROOT/"compatibility/prominent-forge-1.20.1.md","implementation-notes.md")]:
        shutil.copy2(source,out/name)
    (out/"focused-checks.json").write_text(json.dumps(results,indent=2)+"\n")
    manifest={"minecraft":"1.20.1","loader":"Fabric","prominentbase":"4.1.5",
              "runtime":"production intermediary, original release subject jars",
              "jar":jar.name,"sha256":SHA(jar),"core_sha256":SHA(core),
              "focused_passes":len(results),"full_suite_run":False,
              "deferred":"Cinder/Serkonid trader",
              "run":str(run.resolve()),
              "limits":["No dedicated-server multiplayer or other releases/loaders",
                        "One successful armor recipe, not every armor recipe",
                        "No full multiblock construction/gameplay qualification",
                        "No alternate resource packs or controller navigation"]}
    (out/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    print(json.dumps(manifest,indent=2))


if __name__=="__main__":main()
