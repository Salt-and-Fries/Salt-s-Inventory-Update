"""Native Accessories screenshot research; no SIU implementation changes."""
import json
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'functional-tests/project-everything-ten'))
from send import send

RUN=ROOT/'build/remaining-ui-audit/gameplay/1.20.1-fabric-native-accessories-research01'
OUT=ROOT/'build/accessories-ui-research/evidence'
OUT.mkdir(parents=True,exist_ok=True)

def act(kind='capture',**kw):return send(RUN,kind,**kw)['client']
def click(x,y,button=0):
    act('click',x=x,y=y,button=button)
    return act('release',x=x,y=y,button=button)
def capture(name):
    s=act(capture=name)
    (OUT/(name+'.json')).write_text(json.dumps(s,indent=2))
    print(name,s['screen'],flush=True)
    return s
def command(text,**kw):return act('command',command=text,**kw)
def brief(s):return {k:v for k,v in s.items() if k!='slots'}
