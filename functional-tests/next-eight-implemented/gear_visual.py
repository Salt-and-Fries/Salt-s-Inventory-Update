"""Focused six gear-window capture and tab transition checks against a ready SIU client."""
import argparse,json,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent.parent/"project-everything-ten"))
from send import send
p=argparse.ArgumentParser();p.add_argument("--run",required=True);a=p.parse_args()
out=Path("build/project-everything-eight-implemented/gameplay")/f"1.20.1-fabric-{a.run}"
results=[]
def command(kind,**args):return send(out,kind,**args)
def check(case,name,passed,detail=None):
 results.append({"case":case,"name":name,"passed":bool(passed),"detail":detail})
 (out/"gear-visual-checks.json").write_text(json.dumps(results,indent=2))
def capture(name):return command("capture",capture=name,ticks=12)
def bounds(case,r):
 windows=r["client"]["allWindows"];check(case,"one native template",len(windows)==1 and windows[0].get("definition","").endswith("NativeTemplateWindow"))
 if len(windows)!=1:return
 w=windows[0];b=w["bounds"];check(case,"native layout valid",w.get("layoutError")=="",w.get("layoutError"));check(case,"window within screen",b["x"]>=0 and b["y"]>=0 and b["x"]+b["width"]<=r["client"]["guiWidth"] and b["y"]+b["height"]<=r["client"]["guiHeight"],b)
for pair,block,first,second in [("anvil","minecraft:anvil","AnvilMenu","ReforgeScreenHandler"),("smith","minecraft:smithing_table","SmithingMenu","SmitherScreenHandler"),("grind","minecraft:grindstone","GrindstoneMenu","GrinderScreenHandler")]:
 command("fixture",block=block);r=command("open",ticks=20);command("dismiss-help");r=capture("gear-"+pair+"-first");check(pair,"first menu",r["client"]["menu"].endswith(first),r["client"]["menu"]);bounds(pair+"-first",r)
 if pair=="smith":
  command("seed",items=[{"slot":0,"item":"minecraft:netherite_upgrade_smithing_template","count":1},{"slot":1,"item":"minecraft:diamond_chestplate","count":1},{"slot":2,"item":"minecraft:netherite_ingot","count":1}],ticks=20);capture("gear-smith-equipped");command("seed",items=[{"slot":i,"item":"minecraft:air","count":0} for i in range(3)])
 b=r["client"]["window"];r=command("xy-click",x=b["x"]+39,y=b["y"]-10,ticks=30);check(pair,"switch to second",r["client"]["menu"].endswith(second),r["client"]["menu"]);r=capture("gear-"+pair+"-second");bounds(pair+"-second",r)
 if r["client"]["allWindows"]:
  b=r["client"]["window"];r=command("xy-click",x=b["x"]+16,y=b["y"]-10,ticks=30);check(pair,"switch back to first",r["client"]["menu"].endswith(first),r["client"]["menu"])
 command("close")
print(json.dumps(results,indent=2));print("failed:",sum(not x["passed"] for x in results))
