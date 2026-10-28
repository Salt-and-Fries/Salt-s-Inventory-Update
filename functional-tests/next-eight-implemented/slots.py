"""Authoritative pickup/return/reopen checks for Graveyard storage."""
import argparse,json,sys,traceback
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent.parent/"project-everything-ten"))
from send import send
p=argparse.ArgumentParser();p.add_argument("--loader",choices=["fabric","forge"],required=True);p.add_argument("--run",required=True);a=p.parse_args()
out=Path("build/project-everything-eight-implemented/gameplay")/f"1.20.1-{a.loader}-{a.run}"
checks=[]
def cmd(k,**args):return send(out,k,**args)
def check(case,name,passed):checks.append(dict(case=case,name=name,passed=bool(passed)))
for case,block in [("urn","graveyard:red_urn"),("small-urn","graveyard:small_red_urn"),("coffin","graveyard:oak_coffin"),("sarcophagus","graveyard:sarcophagus")]:
 try:
  cmd("fixture",block=block);cmd("open",ticks=20);cmd("dismiss-help")
  s=cmd("seed",items=[dict(slot=0,item="minecraft:diamond",count=3)])
  check(case,"seed reached server",s["server"]["slots"][0]["count"]==3)
  s=cmd("click",slot=0);check(case,"pickup reached server cursor",s["server"]["cursor"]["count"]==3 and s["server"]["slots"][0]["count"]==0)
  s=cmd("click",slot=0);check(case,"return conserved item",s["server"]["slots"][0]["count"]==3 and s["server"]["cursor"]["count"]==0)
  cmd("close");s=cmd("open",ticks=20);check(case,"contents survive reopen",s["server"]["slots"][0]["count"]==3)
  cmd("close")
 except Exception as e:
  checks.append(dict(case=case,name="driver error",passed=False,error=str(e)));traceback.print_exc()
 (out/"eight-slot-checks.json").write_text(json.dumps(checks,indent=2))
 print(case,flush=True)
