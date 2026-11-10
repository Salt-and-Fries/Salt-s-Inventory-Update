"""Find real registry prerequisites, including IDs embedded in seed commands."""
import re,hashlib
from pathlib import Path
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
ID=r'([a-z0-9_.-]+:[a-z0-9_./-]+)'
ROOT=r'(?:^|\brun\s+)'

def requirements(actions):
    found={'items':set(),'blocks':set(),'entities':set()}
    for action in actions:
        for key,registry in [('item','items'),('block','blocks'),('entity','entities')]:
            value=action.get(key,'')
            if re.fullmatch(ID,value):found[registry].add(value)
        command=action.get('command','')
        patterns=[('items',r'\bitem\s+replace\s+.*?\swith\s+'+ID),
            ('items',ROOT+r'give\s+\S+\s+'+ID),('entities',ROOT+r'summon\s+'+ID),
            ('blocks',ROOT+r'setblock\s+(?:\S+\s+){3}'+ID),
            ('blocks',ROOT+r'fill\s+(?:\S+\s+){6}'+ID)]
        for registry,pattern in patterns:found[registry].update(re.findall(pattern,command))
    return found

def missing(actions,*,items,blocks,entities):
    present={'items':set(items),'blocks':set(blocks),'entities':set(entities)}
    return {kind:sorted(values-present[kind]) for kind,values in requirements(actions).items()
            if values-present[kind]}
