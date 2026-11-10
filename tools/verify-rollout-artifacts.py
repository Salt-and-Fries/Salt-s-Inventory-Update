"""Focused production-JAR integrity, strict metadata and registered-mixin checks."""
from pathlib import Path
import hashlib,json,tomllib,zipfile

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/all-target-rollout'
versions=['1.20.1','1.21.1','1.21.11','26.1.2','26.2','26.3']
rows=[];failures=[]
for mc in versions:
 for loader in ['fabric','forge' if mc=='1.20.1' else 'neoforge']:
  for family,mod,version in [('core','salts_inventory_update','0.1.6'),('compatibility','salts_inventory_compat','0.1.3')]:
   folder=ROOT/('versions' if family=='core' else 'compatibility/versions')/mc/loader/'build/libs'
   path=folder/f'{mod}-{mc}-{dict(fabric="Fabric",forge="Forge",neoforge="NeoForge")[loader]}-{version}.jar'
   row={'minecraft':mc,'loader':loader,'family':family,'file':str(path),'checks':[]};rows.append(row)
   try:
    with zipfile.ZipFile(path) as archive:
     assert archive.testzip() is None,'ZIP integrity failure'
     entries=set(archive.namelist())
     if family=='compatibility' and loader=='neoforge' and mc in ['1.21.1','26.1.2']:
      # The optional requester menu uses AE2's independent desktop style path.
      # A registered adapter without this document produces an empty window.
      style=json.loads(archive.read('assets/ae2/screens/desktop/wireless_requester_terminal.json'))
      assert '../wireless_requester_terminal.json' in style['includes']
      assert style['desktop']['preserveAeLayout'] and style['desktop']['inventoryCut']>0
      lang=json.loads(archive.read('assets/ae2/lang/en_us.json'))
      assert lang.get('gui.ae2.DesktopWindow.wireless_requester_terminal')
      row['checks'].append('optional requester desktop style and title')
     assert not any(n.startswith(('com/siu_reference/','com/salts_inventory_update/functionaltest/')) for n in entries),'QA classes packaged'
     metadata='fabric.mod.json' if loader=='fabric' else 'META-INF/mods.toml' if loader=='forge' else 'META-INF/neoforge.mods.toml'
     raw=archive.read(metadata)
     if loader=='fabric':
      parsed=json.loads(raw);assert parsed['id']==mod
     else:
      parsed=tomllib.loads(raw.decode('utf-8'));assert any(m['modId']==mod for m in parsed['mods'])
     row['checks'].append('strict loader metadata')
     if 'pack.mcmeta' in entries:
      pack=json.loads(archive.read('pack.mcmeta'));assert isinstance(pack['pack']['description'],(str,dict,list))
      assert not (mc=='1.21.11' and loader=='neoforge'),'NeoForge must synthesize separate client/data format metadata'
      row['checks'].append('strict resource-pack JSON')
     else:row['checks'].append('loader-synthesized pack metadata')
     configs=sorted(n for n in entries if n.endswith('.mixins.json'))
     for config in configs:
      parsed=json.loads(archive.read(config));package=parsed.get('package','')
      classes=[*parsed.get('mixins',[]),*parsed.get('client',[]),*parsed.get('server',[])]
      for cls in classes:
       member=((package+'.') if package else '')+cls
       assert member.replace('.','/')+'.class' in entries,f'Missing configured mixin: {member}'
      if parsed.get('plugin'):assert parsed['plugin'].replace('.','/')+'.class' in entries,'Missing mixin plugin'
     row['mixin_configs']=len(configs);row['checks'].append('configured mixin classes packaged')
     if family=='core':assert 'com/salts_inventory_update/server/NativePacketContext.class' in entries,'Missing current native context bridge'
     row.update(status='pass',bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest())
     print('PASS',family,mc,loader,flush=True)
   except Exception as error:
    row.update(status='failed',error=str(error));failures.append(row);print('FAIL',family,mc,loader,str(error),flush=True)
(WORK/'artifact-integrity.json').write_text(json.dumps({'scope':'Production archive integrity, metadata, QA exclusion and mixin inventory only; not gameplay validation','artifacts':rows,'failures':len(failures)},indent=2))
if failures:raise SystemExit(1)
print('All 24 current production archives passed focused integrity checks.')
