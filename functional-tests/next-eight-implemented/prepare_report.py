"""Curate only real game captures and crop their full window groups for the PDF."""
import json,hashlib,shutil
from pathlib import Path
from PIL import Image
root=Path("build/project-everything-eight-implemented/gameplay")
out=Path("output/project-everything-eight-implemented");(out/"screenshots").mkdir(parents=True,exist_ok=True);(out/"crops").mkdir(parents=True,exist_ok=True)
F="1.20.1-fabric-eight-integration-03";G="1.20.1-forge-eight-integration-03";V="1.20.1-fabric-eight-variants-01"
entries=[
(F,"fabric-drawer-single","Single drawer - survival"),(F,"fabric-drawer-double","Double drawer - survival"),(F,"fabric-drawer-quad","Quad drawer - survival"),
(F,"fabric-drawer-single-creative","Single drawer - creative"),(F,"fabric-drawer-double-creative","Double drawer - creative"),(F,"fabric-drawer-quad-creative","Quad drawer - creative"),
(F,"fabric-ossuary","Ossuary - recipe selection"),(F,"fabric-urn","Urn"),(F,"fabric-small-urn","Small urn"),(F,"fabric-coffin","Coffin"),(F,"fabric-sarcophagus","Sarcophagus"),
(F,"fabric-end-smelter","End Stone Smelter"),(F,"fabric-smelter-book","End Stone Smelter - recipe book"),(F,"fabric-end-furnace","End Stone Furnace"),
(F,"fabric-rune-altar","Rune Crafting Altar"),(F,"fabric-reforge-entry","Repair and name"),(F,"fabric-anvil-renamed","Repair and name - result and cost"),(F,"fabric-reforge","Reforge Gear"),
(F,"fabric-smith-entry","Upgrade Gear"),(F,"fabric-link-gear","Link Gear"),(F,"fabric-link-success","Link Gear - linked gem"),(F,"fabric-grind-entry","Repair and Disenchant"),(F,"fabric-unlink-gear","Unlink Gear"),
(V,"fabric-unlink-ready","Unlink Gear - result"),(F,"fabric-fletching","Fletching Table - populated"),(V,"fabric-chunk-selected","Chunk Loader - selected chunk"),
(G,"forge-ossuary","Ossuary - Forge"),(G,"forge-rune-result","Rune Crafting Altar - Forge result"),(G,"forge-combiner","Combiner - Forge"),(G,"forge-combiner-book","Combiner - Forge recipe book")]
cache={};manifest=[]
for run,name,label in entries:
 directory=root/run
 if run not in cache:cache[run]=json.loads((directory/"observations.json").read_text())
 matches=[x for x in cache[run] if x.get("command",{}).get("capture")==name]
 if not matches:raise ValueError("Missing runtime observation: "+name)
 obs=matches[-1];client=obs["client"];src=directory/"screenshots"/(name+".png")
 if not src.is_file():raise FileNotFoundError(src)
 img=Image.open(src);sx=img.width/client["guiWidth"];sy=img.height/client["guiHeight"]
 windows=client.get("allWindows",[])
 if not windows:raise ValueError("No hosted window for "+name)
 b=windows[-1]["bounds"];x1=b["x"];y1=b["y"];x2=x1+b["width"];y2=y1+b["height"]
 if any(t in name for t in ("reforge","link-gear","link-success","grind-entry","smith-entry","unlink","anvil")):y1-=25
 for w in client.get("desktopWindows",[]):
  if w.get("recipeVisible"):
   r=w["recipeBounds"];x1=min(x1,r["x"]);y1=min(y1,r["y"]);x2=max(x2,r["x"]+r["width"]);y2=max(y2,r["y"]+r["height"])
 margin=9;left_margin=1 if name=="fabric-link-success" else margin;rect=(max(0,int((x1-left_margin)*sx)),max(0,int((y1-margin)*sy)),min(img.width,int((x2+margin)*sx)),min(img.height,int((y2+margin)*sy)))
 img.crop(rect).save(out/"crops"/(name+".png"));shutil.copyfile(src,out/"screenshots"/(name+".png"))
 manifest.append(dict(label=label,loader="forge" if run.startswith("1.20.1-forge") else "fabric",run=run,screenshot=str((out/"screenshots"/(name+".png")).resolve()),crop=str((out/"crops"/(name+".png")).resolve()),crop_pixels=rect,source_sha256=hashlib.sha256(src.read_bytes()).hexdigest(),sequence=obs["sequence"]))
(out/"screenshot-manifest.json").write_text(json.dumps({"scope":"Actual 1.20.1 runtime screenshots; crops retain original pixels", "captures":manifest},indent=2))
print("Prepared",len(manifest),"actual screenshot crops")
