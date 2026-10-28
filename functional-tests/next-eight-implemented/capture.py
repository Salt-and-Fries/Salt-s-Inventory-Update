"""Focused eight-integration in-world captures; run only against a ready QA client."""
import argparse, json, sys, traceback
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "project-everything-ten"))
from send import send

CASES = {
 "fabric": [
  ("drawer-single", "extended_drawers:single_drawer", True),
  ("drawer-double", "extended_drawers:double_drawer", True),
  ("drawer-quad", "extended_drawers:quad_drawer", True),
  ("ossuary", "graveyard:ossuary", False),
  ("urn", "graveyard:red_urn", False),
  ("small-urn", "graveyard:small_red_urn", False),
  ("coffin", "graveyard:oak_coffin", False),
  ("sarcophagus", "graveyard:sarcophagus", False),
  ("gravestone", "graveyard:gravestone", False),
  ("rune-altar", "runes:crafting_altar", False),
  ("end-smelter", "betterend:end_stone_smelter", False),
  ("end-furnace", "betterend:end_stone_furnace", False),
  ("reforge-entry", "minecraft:anvil", False),
  ("smith-entry", "minecraft:smithing_table", False),
  ("grind-entry", "minecraft:grindstone", False),
  ("fletching", "minecraft:fletching_table", False),
  ("chunk-loader", "additionz:chunk_loader", False)],
 "forge": [("ossuary", "graveyard:ossuary", False), ("urn", "graveyard:red_urn", False), ("small-urn", "graveyard:small_red_urn", False), ("coffin", "graveyard:oak_coffin", False), ("sarcophagus", "graveyard:sarcophagus", False), ("gravestone", "graveyard:gravestone", False), ("rune-altar", "runes:crafting_altar", False), ("combiner", "deep_aether:combiner", False)]}
SEEDS = {"ossuary": [(0,"minecraft:bone_block",32)],
 "fletching": [(0,"minecraft:flint",16),(1,"minecraft:stick",16),(2,"minecraft:feather",16)],
 "chunk-loader": [(0,"minecraft:coal",16)],
 "combiner": [(0,"aether:golden_amber",16),(1,"deep_aether:frozen_goldenleaf_berries",16),(2,"deep_aether:bio_crystal",16)]}

def run(loader, run_id, include):
 out=Path("build/project-everything-eight-implemented/gameplay") / f"1.20.1-{loader}-{run_id}"
 report=[]
 def cmd(kind, **args): return send(out,kind,**args)
 def save(): (out/"eight-capture-results.json").write_text(json.dumps(report,indent=2))
 cmd("command",command="gamemode survival @p")
 cmd("command",command="recipe give @p *")
 for name, block, crouch in CASES[loader]:
  if include and name not in include: continue
  row={"case":name,"block":block,"checks":[],"captures":[]};report.append(row)
  try:
   cmd("fixture",block=block,ticks=12)
   state=cmd("crouch-open" if crouch else "open",ticks=20)
   cmd("dismiss-help")
   if name in SEEDS:
    state=cmd("seed",items=[dict(slot=i,item=item,count=count) for i,item,count in SEEDS[name]],ticks=12)
   state=cmd("capture",capture=f"{loader}-{name}",ticks=12)
   row["captures"].append(state["screenshot"])
   row["screen"]=state["client"]["screen"]
   row["windows"]=state["client"]["allWindows"]
   if name == "gravestone":
    row["checks"].append({"name":"native gravestone editor", "passed": state["client"]["screen"].endswith("GravestoneScreen") and not state["client"]["allWindows"]})
   else:
    windows=state["client"]["allWindows"]
    row["checks"].append({"name":"one hosted window", "passed":len(windows)==1})
    if windows:
     b=windows[0]["bounds"];w=state["client"]["guiWidth"];h=state["client"]["guiHeight"]
     row["checks"].append({"name":"window inside screen", "passed": b["x"]>=0 and b["y"]>=0 and b["x"]+b["width"]<=w and b["y"]+b["height"]<=h})
     state=cmd("move",dx=-45,dy=25); b=state["client"]["allWindows"][0]["bounds"]
     row["checks"].append({"name":"moved window inside screen", "passed": b["x"]>=0 and b["y"]>=0 and b["x"]+b["width"]<=w and b["y"]+b["height"]<=h})
   cmd("close")
  except Exception as e:
   row["error"]=str(e); traceback.print_exc()
  save();print(name, row.get("error", "captured"),flush=True)
 print("Capture pass complete. Inspect results and perform interaction-specific follow-ups.",flush=True)

if __name__=="__main__":
 p=argparse.ArgumentParser();p.add_argument("--loader",choices=["fabric","forge"],required=True);p.add_argument("--run",required=True);p.add_argument("--include",help="Comma-separated case names");a=p.parse_args();run(a.loader,a.run,set(a.include.split(",")) if a.include else set())
