"""Locate real upstream callers for container candidates without opening menus.

Reads original retained release bytes. Javap output is source evidence only,
never proof that a workflow was reached in gameplay. All outputs stay in build/.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import importlib.util
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
JAVAP = Path('C:/Program Files/Microsoft/jdk-21.0.10.7-hotspot/bin/javap.exe')
spec = importlib.util.spec_from_file_location('inventory_tool', ROOT/'tools/plan-remaining-ui-audit.py')
inventory_tool = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory_tool)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('entries',nargs='+',type=int)
    args=parser.parse_args()
    inventory=json.loads((ROOT/'output/remaining-ui-audit/inventory.json').read_text(encoding='utf-8'))
    tasks=[]
    out=ROOT/'build/remaining-ui-audit/source-workflows';out.mkdir(parents=True,exist_ok=True)
    for entry in inventory['entries']:
        if entry['entry'] not in args.entries:continue
        targets=set(entry['container_screens']+entry['menus'])
        found={t:[] for t in targets}
        # Choose the first retained release containing each type. The record
        # retains release identity so a different target is never implied.
        for release in entry['releases']:
            if not release['path']:continue
            with zipfile.ZipFile(ROOT/release['path']) as jar:
                names=set(jar.namelist())
                contained={t for t in targets if t+'.class' in names}
                if not contained:continue
                for member in names:
                    if not member.endswith('.class'):continue
                    header=inventory_tool.class_header(jar.read(member),True)
                    strings=header['strings']
                    matched={t for t in contained if any(s==t or ('L'+t+';') in s for s in strings)}
                    for target in matched:
                        found[target].append({'caller':member[:-6],'release':release['version'],'path':release['path']})
                        # Dump all actual references, including network handlers
                        # and registry code; filtering by class name misses
                        # inherited or item-triggered entry points.
                        dest=out/str(entry['entry'])/(member[:-6].replace('/','.')+'.txt')
                        tasks.append((ROOT/release['path'],member[:-6],dest))
                targets-=contained
                if not targets:break
        (out/(str(entry['entry'])+'-callers.json')).write_text(json.dumps({'entry':entry['entry'],'title':entry['title'],'targets':found},indent=2)+'\n',encoding='utf-8')
    tasks=list({str(t[2]):t for t in tasks}.values())
    def run(task):
        jar,cls,dest=task
        if dest.exists():return
        dest.parent.mkdir(parents=True,exist_ok=True)
        result=subprocess.run([str(JAVAP),'-classpath',str(jar),'-p','-c',cls.replace('/','.')],capture_output=True,text=True,encoding='utf-8',errors='replace')
        if result.returncode:raise RuntimeError(result.stderr)
        dest.write_text(result.stdout,encoding='utf-8')
    with ThreadPoolExecutor(max_workers=2) as pool:list(pool.map(run,tasks))
    print(json.dumps({'entries':args.entries,'reference_classes':len(tasks),'output':str(out.relative_to(ROOT))}))


if __name__=='__main__':main()
