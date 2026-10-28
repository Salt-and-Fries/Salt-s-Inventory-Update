"""QA-only: map SRG names in mixin string constants after ModDev SRG_TO_MCP."""
from pathlib import Path
import argparse,zipfile,struct,re,hashlib,json,os
p=argparse.ArgumentParser();p.add_argument('--mapping',type=Path,required=True);p.add_argument('--dir',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True);a=p.parse_args()
names={};conflicts=set()
for line in a.mapping.read_text().splitlines():
 fields=line.split()
 if not fields or fields[0] not in ('MD:','FD:'):continue
 old=fields[1].rsplit('/',1)[-1];new=fields[3 if fields[0]=='MD:' else 2].rsplit('/',1)[-1]
 if not re.fullmatch(r'[mf]_\d+_',old):continue
 if old in names and names[old]!=new:conflicts.add(old)
 else:names[old]=new
for k in conflicts:names.pop(k,None)
pat=re.compile(rb'(?<![A-Za-z0-9_])(?:m|f)_\d+_(?![A-Za-z0-9_])')
def patch(data):
 if data[:4]!=b'\xca\xfe\xba\xbe':return data,[]
 count=struct.unpack_from('>H',data,8)[0];out=bytearray(data[:10]);pos=10;idx=1;changes=[]
 while idx<count:
  tag=data[pos];out.append(tag);pos+=1
  if tag==1:
   n=struct.unpack_from('>H',data,pos)[0];pos+=2;raw=data[pos:pos+n];pos+=n
   def sub(m):
    old=m.group().decode();new=names.get(old)
    if new is not None:changes.append([old,new]);return new.encode()
    return m.group()
   raw=pat.sub(sub,raw);out+=struct.pack('>H',len(raw))+raw
  else:
   size={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}[tag];out+=data[pos:pos+size];pos+=size
   if tag in (5,6):idx+=1
  idx+=1
 out+=data[pos:];return bytes(out),changes
records=[]
for prefix in ('simplyswords-forge-neoforge-','SimplyTooltips-forge-'):
 matches=list(a.dir.glob(prefix+'*.jar'));assert len(matches)==1,(prefix,matches)
 jar=matches[0];before=hashlib.sha256(jar.read_bytes()).hexdigest();tmp=jar.with_suffix('.qa.tmp');changed={}
 with zipfile.ZipFile(jar) as z,zipfile.ZipFile(tmp,'w') as out:
  for info in z.infolist():
   data=z.read(info.filename)
   if '/mixin/' in info.filename and info.filename.endswith('.class'):
    data,edits=patch(data)
    if edits:changed[info.filename]=edits
   out.writestr(info,data)
 os.replace(tmp,jar);records.append({'jar':str(jar),'sha256_before':before,'sha256_after':hashlib.sha256(jar.read_bytes()).hexdigest(),'changed_mixin_classes':changed})
a.evidence.write_text(json.dumps({'operation':'QA-only annotation-string SRG to named after regular remap; all upstream mixins enabled','mapping':str(a.mapping),'conflicting_ids_skipped':sorted(conflicts),'artifacts':records},indent=2));print('annotation classes changed:',[(Path(r['jar']).name,len(r['changed_mixin_classes'])) for r in records])
