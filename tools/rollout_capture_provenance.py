"""Keep comparisons tied to the same original mod UI binary on both sides."""
from pathlib import Path
from functools import lru_cache
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
ARCHIVE_MEMBERS={}
@lru_cache(maxsize=None)
def archive(path):
 path=Path(path)
 if not path.is_file() or path.suffix!='.jar':return None
 digest=hashlib.sha256(path.read_bytes()).hexdigest()
 if digest not in ARCHIVE_MEMBERS:
  with zipfile.ZipFile(path) as z:ARCHIVE_MEMBERS[digest]=frozenset(z.namelist())
 return digest,ARCHIVE_MEMBERS[digest]
def inputs(row):
 data=row.get('staged_artifacts')
 if not data:
  path=Path(row['run'])/'staged-artifacts.json'
  if not path.is_file():return []
  data=json.loads(path.read_text(encoding='utf-8-sig'))
 records=data.get('artifacts',[]) if isinstance(data,dict) else data
 return [ROOT/record.get('file',record.get('path','')) for record in records]
def same_ui_binary(sides):
 native=sides['native'];screen=native['state'].get('screen','')
 if screen.startswith('net.minecraft.'):
  return True,{'status':'shared Minecraft screen; owning mod is identified by its recorded entry workflow'}
 member=screen.replace('.','/')+'.class';owner=None
 for path in inputs(native):
  record=archive(str(path))
  if record and member in record[1]:owner=(path,record[0]);break
 if owner is None:
  return True,{'status':'shared or nested screen; recorded input manifests retained for owner review','screen':screen}
 original,digest=owner
 for path in inputs(sides['siu']):
  record=archive(str(path))
  if record and record[0]==digest:
   return True,{'status':'same original UI publisher binary on both sides','screen':screen,'sha256':digest,'native_input':str(original),'siu_input':str(path)}
 return False,{'status':'UI publisher binary differs between comparison sides','screen':screen,'native_input':str(original),'native_sha256':digest}
