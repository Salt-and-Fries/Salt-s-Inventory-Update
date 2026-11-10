"""Recover ordinary entry workflows from the original, SIU-absent UI atlas.

Only setup commands and ordinary block/item/entity use are replayed. Screen
construction, forced providers and absolute GUI clicks are never replayed.
Each case is gated on the exact publisher binary that supplied its screen.
"""
from collections import defaultdict
from pathlib import Path
import hashlib,json,zipfile
import re
ROOT=Path(__file__).resolve().parents[2]
SOURCE_SHA256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
OUT=ROOT/'output/project-everything-regression/native-entry-cases.json'
ALLOWED={'fixture','command','server-command','held','sneak','open','open-ray','use-block-ray','use-entity-ray','client-use-block',
         'client-use-item','client-use-entity','client-use-block-ray','client-use-any-block-ray','client-use-entity-ray','use-item','use-entity','aim-entity','wait','close'}
IGNORE={'catalog','capture','snapshot-server','reviewed-block-probe','wait-native-frame','move'}
COMMANDS={'clear','gamemode','gamerule','time','weather','kill','summon','tp','teleport','item','data','setblock','fill','clone','give','effect','execute','attribute','scoreboard','tag','enchant','experience','advancement'}

def settings_transition(parent,child):
    """Known custom native widgets omitted by the atlas's AbstractWidget list."""
    if parent=='com.tiviacz.travelersbackpack.client.screens.BackpackScreen' and child=='com.tiviacz.travelersbackpack.client.screens.BackpackSettingsScreen':
        return 'com.tiviacz.travelersbackpack.client.screens.widgets.SettingsWidget'
    backpack=(parent.startswith('net.p3pp3rf1y.sophisticatedbackpacks.client.gui.') and child=='net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackSettingsScreen')
    storage=(parent.startswith('net.p3pp3rf1y.sophisticatedstorage.client.gui.') and child in {
        'net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen',
        'net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelSettingsScreen'})
    if (backpack or storage) and not parent.endswith('SettingsScreen'):
        return 'net.p3pp3rf1y.sophisticatedcore.client.gui.StorageSettingsTab'
    return None

def translate(command, old, new):
    delta=[b-a for a,b in zip(old,new)]
    if not any(delta):return command
    number=r'(-?\d+(?:\.\d+)?)'
    def shift(match):
        prefix=match.group(1);values=match.groups()[1:]
        return prefix+' '.join(str(float(v)+delta[i%3]).removesuffix('.0') for i,v in enumerate(values))
    patterns=[r'((?:^|\brun )(?:setblock|data (?:merge|get|modify) block)\s+)'+number+r'\s+'+number+r'\s+'+number,
              r'((?:^|\brun )(?:tp|teleport)\s+\S+\s+)'+number+r'\s+'+number+r'\s+'+number,
              r'((?:^|\brun )summon\s+\S+\s+)'+number+r'\s+'+number+r'\s+'+number,
              r'((?:^|\brun )fill\s+)'+r'\s+'.join([number]*6),
              r'((?:^|\brun )clone\s+)'+r'\s+'.join([number]*9)]
    for pattern in patterns:command=re.sub(pattern,shift,command)
    # This native AE2 component stores an absolute link to the seeded access
    # point. Moving only the setup blocks leaves a valid item bound elsewhere.
    link=(r'(ae2:wireless_link_target\s*=\s*\{\s*dimension\s*:\s*"minecraft:overworld"\s*,\s*pos\s*:\s*\[I;\s*)'
          r'(-?\d+)\s*,\s*(-?\d+)\s*,\s*(-?\d+)(\s*\])')
    command=re.sub(link,lambda m:m[1]+','.join(str(int(m[i+2])+delta[i]) for i in range(3))+m[5],command)
    return command

def build():
    ledger=json.loads((ROOT/'build/all-target-rollout/ui-capture-ledger.json').read_text())
    rows=[r for r in ledger if r['side']=='native' and r.get('state',{}).get('menuClass')
          and not r['state'].get('screen','').startswith(('net.minecraft.','com.salts_inventory_update.'))]
    wanted={r['state']['screen'].replace('.','/')+'.class' for r in rows}
    owners=defaultdict(set);files={}
    for r in rows:
        for artifact in r.get('staged_artifacts',{}).get('artifacts',[]):
            if artifact.get('family') not in ['upstream',None]:continue
            file=Path(artifact.get('file',''))
            if file.is_file() and file.suffix=='.jar': files[str(file)]=artifact.get('sha256')
    for path,declared in files.items():
        try:
            with zipfile.ZipFile(path) as jar: matches=wanted.intersection(jar.namelist())
            if not matches:continue
            digest=hashlib.sha256(Path(path).read_bytes()).hexdigest()
            if declared:assert digest==declared,path
            for name in matches: owners[name].add(digest)
        except zipfile.BadZipFile:continue
    groups=defaultdict(list)
    for r in rows:groups[r['run']].append(r)
    cases={};gaps=[]
    for run,records in groups.items():
        path=Path(run)/'observations.json'
        if not path.exists():continue
        history=json.loads(path.read_text());captures={v.get('command',{}).get('capture'):i for i,v in enumerate(history) if v.get('command',{}).get('capture')}
        catalog=Path(run)/'catalog.json';origin=json.loads(catalog.read_text()).get('origin','8,65,8') if catalog.exists() else '8,65,8'
        for r in records:
            screen=r['state']['screen']
            hashes=owners[screen.replace('.','/')+'.class'].intersection(a.get('sha256') for a in r.get('staged_artifacts',{}).get('artifacts',[]))
            key=(r['minecraft'],r['loader'],screen,tuple(sorted(hashes)))
            if key in cases:continue
            index=captures.get(Path(r['screenshot']).stem)
            if index is None or not hashes:continue
            start=next((i for i in range(index,-1,-1) if history[i].get('command',{}).get('type')=='fixture'),None)
            if start is None:continue
            actions=[];observed='none';paired_release=False
            for step in range(start,index+1):
                observation=history[step]
                command=dict(observation.get('command',{}));kind=command.pop('type',None);command.pop('sequence',None);command.pop('capture',None)
                if kind in IGNORE:
                    observed=observation.get('client',{}).get('screen',observed);continue
                if kind=='release' and paired_release:
                    paired_release=False;observed=observation.get('client',{}).get('screen','none');continue
                if kind=='click':
                    # Recover a semantic widget, never reuse native screen pixel
                    # coordinates after SIU has laid out the window elsewhere.
                    before=history[step-1].get('client',{}) if step else {}
                    release=history[step+1].get('command',{}) if step+1<len(history) else {}
                    widgets=[w for w in before.get('widgets',[]) if w.get('visible',True)]
                    hits=[w for w in widgets if w['x']<=command.get('x',-1)<w['x']+w['width'] and w['y']<=command.get('y',-1)<w['y']+w['height']]
                    if release.get('type')!='release' or any(release.get(k,0)!=command.get(k,0) for k in ['x','y','button']):break
                    if hits:
                        widget=min(hits,key=lambda w:w['width']*w['height'])
                        peers=[w for w in widgets if w['type']==widget['type'] and w.get('message','')==widget.get('message','')]
                    else:
                        widget_type=settings_transition(before.get('screen',''),observation.get('client',{}).get('screen',''))
                        if not widget_type:break
                        widget={'type':widget_type,'message':''};peers=[widget]
                    actions.append({'type':'regression-native-widget','widget_type':widget['type'],'message':widget.get('message',''),
                        'ordinal':peers.index(widget),'button':int(command.get('button',0)),'ticks':min(40,int(release.get('ticks',16))),
                        'parent_menu':before.get('menuClass'),'parent_title':before.get('title',''),'parent_screen':before.get('screen','')})
                    paired_release=True;observed=observation.get('client',{}).get('screen','none');continue
                if kind not in ALLOWED:break
                if kind=='server-command':kind='command'
                if kind=='command':
                    roots=[command.get('command','').split(' ',1)[0],*re.findall(r'\brun\s+(\S+)',command.get('command',''))]
                    if any(root not in COMMANDS for root in roots):break
                if kind=='held' and 'count' in command:
                    value=float(command['count']);assert value.is_integer(),command;command['count']=int(value)
                command['ticks']=min(40,int(command.get('ticks',8)))
                actions.append({'type':kind,**command});observed=observation.get('client',{}).get('screen','none')
            if observed!=screen or not actions:
                gaps.append({'minecraft':r['minecraft'],'loader':r['loader'],'ui':r['ui'],'screen':screen,'publisher_sha256':sorted(hashes),'reason':'Requires semantic child-menu or specialized entry actions.'});continue
            cases[key]={'id':'atlas-'+r['ui'],'minecraft':r['minecraft'],'loader':r['loader'],'expected_native_screen':screen,
                'expected_native_menu':r['state']['menuClass'],
                'expected_nonplayer_slot_count':sum(not s.get('player',False) for s in r['state'].get('slots',[])),
                'publisher_sha256':sorted(hashes),'captured_origin':origin,'actions':actions,'original_evidence':r['screenshot']}
    unresolved={}
    for gap in gaps:
        key=(gap['minecraft'],gap['loader'],gap['screen'],tuple(gap['publisher_sha256']))
        if key not in cases:unresolved[key]=gap
    gaps=list(unresolved.values())
    temporary=OUT.with_suffix('.tmp');temporary.write_text(json.dumps({'cases':list(cases.values()),'specialized_entry_gaps':gaps},indent=2)+'\n');temporary.replace(OUT)
    print('Recovered',len(cases),'ordinary native screen entries;',len(gaps),'specialized entries remain explicit')

if __name__=='__main__':build()
