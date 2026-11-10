"""Small publisher-binary inspection helper; does not modify source or evidence."""
from pathlib import Path
import argparse,json,zipfile
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('project');p.add_argument('--target');p.add_argument('--contains',default='Screen');a=p.parse_args()
lock=json.loads((ROOT/'build/all-target-rollout/availability.json').read_text(encoding='utf-8'))
project=lock['projects'][a.project]
for target,release in project['targets'].items():
 if a.target and target!=a.target:continue
 path=ROOT/'build/all-target-rollout/upstream'/(release['sha512'][:16]+'-'+release['filename'])
 print(target,release['version'],path)
 if path.exists():
  with zipfile.ZipFile(path) as z:
   for name in z.namelist():
    if a.contains.lower() in name.lower() and name.endswith('.class'):print(' ',name)
