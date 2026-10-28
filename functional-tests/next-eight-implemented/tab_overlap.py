"""Focused right-selected native tab overlap captures."""
import argparse,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent.parent/"project-everything-ten"))
from send import send
p=argparse.ArgumentParser();p.add_argument("--run",required=True);a=p.parse_args()
out=Path("build/project-everything-eight-implemented/gameplay")/f"1.20.1-fabric-{a.run}";results=[]
def c(k,**kw):return send(out,k,**kw)
for name,block,expected in [("reforge","minecraft:anvil","ReforgeScreenHandler"),("link","minecraft:smithing_table","SmitherScreenHandler"),("unlink","minecraft:grindstone","GrinderScreenHandler")]:
 c("fixture",block=block);r=c("open",ticks=20);c("dismiss-help");b=r["client"]["window"];r=c("xy-click",x=b["x"]+33,y=b["y"]-11,ticks=25);r=c("capture",capture="tab-overlap-"+name,ticks=12);w=r["client"]["allWindows"];check={"case":name,"menu":r["client"]["menu"],"expected_menu":expected,"menu_pass":r["client"]["menu"].endswith(expected),"window_count":len(w),"definition":w[0].get("definition") if len(w)==1 else None,"layout_error":w[0].get("layoutError") if len(w)==1 else None,"bounds":w[0].get("bounds") if len(w)==1 else None,"sequence":r["sequence"]};results.append(check);(out/"tab-overlap-checks.json").write_text(json.dumps(results,indent=2));print(name,check,flush=True);c("close")
