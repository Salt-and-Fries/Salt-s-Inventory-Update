"""Focused Chipped station-opening captures in an already-running SIU QA fixture."""
import argparse,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent.parent/"project-everything-ten"))
from send import send
p=argparse.ArgumentParser();p.add_argument("--loader",choices=["fabric","forge"],required=True);p.add_argument("--run",required=True);a=p.parse_args()
out=Path("build/project-everything-chipped-implemented/gameplay")/f"1.20.1-{a.loader}-{a.run}";rows=[]
def c(kind,**kw):return send(out,kind,**kw)
for name,item in [("mason_table","minecraft:stone"),("carpenters_table","minecraft:oak_planks"),("loom_table","minecraft:white_wool"),("glassblower","minecraft:glass"),("botanist_workbench","minecraft:oak_leaves"),("alchemy_bench","minecraft:amethyst_block"),("tinkering_table","minecraft:iron_bars")]:
 row={"station":name,"material":item};rows.append(row)
 try:
  c("fixture",block="chipped:"+name,ticks=12);r=c("open",ticks=20);c("dismiss-help");r=c("capture",capture=a.loader+"-"+name.replace("_","-")+"-empty",ticks=12)
  w=r["client"]["allWindows"];row["menu"]=r["client"]["menu"];row["screen"]=r["client"]["screen"];row["windows"]=w;row["capture"]=r["screenshot"]
  row["one_window"]=len(w)==1;row["chipped_menu"]="chipped" in row["menu"].lower()
  if len(w)==1:
   b=w[0]["bounds"];row["inside_screen"]=b["x"]>=0 and b["y"]>=0 and b["x"]+b["width"]<=r["client"]["guiWidth"] and b["y"]+b["height"]<=r["client"]["guiHeight"]
  c("close")
 except Exception as e:row["error"]=str(e)
 (out/"chipped-open-checks.json").write_text(json.dumps(rows,indent=2));print(name,row.get("error","captured"),flush=True)
