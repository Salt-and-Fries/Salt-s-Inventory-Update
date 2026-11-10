"""Small deterministic Minecraft structures used only in disposable QA worlds."""
from pathlib import Path
import gzip,struct

def structure(length=1):
    def utf(s):
        b=s.encode('utf-8');return struct.pack('>H',len(b))+b
    def entry(t,n,p):return bytes([t])+utf(n)+p
    def integer(n,v):return entry(3,n,struct.pack('>i',v))
    def string(n,v):return entry(8,n,utf(v))
    def ints(n,values):return entry(9,n,b'\x03'+struct.pack('>i',len(values))+b''.join(struct.pack('>i',v) for v in values))
    def compounds(n,rows):return entry(9,n,b'\x0a'+struct.pack('>i',len(rows))+b''.join(r+b'\0' for r in rows))
    root=b'\x0a\0\0'+integer('DataVersion',3465)+ints('size',[1,1,length])+compounds('palette',[string('Name','minecraft:stone')])+compounds('blocks',[ints('pos',[0,0,z])+integer('state',0) for z in range(length)])+compounds('entities',[])+b'\0'
    return gzip.compress(root,mtime=0)

def prepare_schematics(runtime,owner='UIAudit'):
    runtime=Path(runtime).resolve()
    assert runtime.is_dir() and runtime.parent.name in ('production-runtime','remaining-audit-native'),runtime
    for name,length in [('stone',1),('line',16)]:
        file='siu-create-functional-'+name+'.nbt';data=structure(length)
        for folder in [runtime/'schematics',runtime/'schematics/uploaded'/owner]:
            folder.mkdir(parents=True,exist_ok=True);(folder/file).write_bytes(data)

def deployed_schematic(length=16,owner='UIAudit'):
    return '{Anchor:{X:12,Y:65,Z:4},Bounds:[1,1,'+str(length)+'],Deployed:1b,File:"siu-create-functional-'+('line' if length==16 else 'stone')+'.nbt",Mirror:"NONE",Owner:"'+owner+'",Rotation:"NONE"}'
